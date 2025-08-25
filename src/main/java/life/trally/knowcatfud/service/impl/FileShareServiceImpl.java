package life.trally.knowcatfud.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import life.trally.knowcatfud.entity.FileShare;
import life.trally.knowcatfud.entity.FileShareIntroduction;
import life.trally.knowcatfud.entity.UserFile;
import life.trally.knowcatfud.mapper.FileShareIntroductionMapper;
import life.trally.knowcatfud.mapper.FileShareMapper;
import life.trally.knowcatfud.mapper.UserFileMapper;
import life.trally.knowcatfud.mapper.UserLikesShareMapper;
import life.trally.knowcatfud.mapping.FileShareMapping;
import life.trally.knowcatfud.rabbitmq.messages.LikeMessage;
import life.trally.knowcatfud.request.FileShareRequest;
import life.trally.knowcatfud.service.ServiceResult;
import life.trally.knowcatfud.service.interfaces.FileShareService;
import life.trally.knowcatfud.utils.JsonUtils;
import life.trally.knowcatfud.utils.RedisUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FileShareServiceImpl implements FileShareService {

    private final FileShareMapper fileShareMapper;

    private final RedisUtils redisUtil;

    private final UserFileMapper userFileMapper;

    private final UserLikesShareMapper userLikesShareMapper;

    private final RabbitTemplate rabbitTemplate;

    private final ElasticsearchOperations elasticsearchOperations;

    private final FileShareMapping fileShareMapping;
    private final FileShareIntroductionMapper fileShareIntroductionMapper;

    public FileShareServiceImpl(FileShareMapper fileShareMapper, RedisUtils redisUtil, UserFileMapper userFileMapper, UserLikesShareMapper userLikesShareMapper, RabbitTemplate rabbitTemplate, ElasticsearchOperations elasticsearchOperations, FileShareMapping fileShareMapping, FileShareIntroductionMapper fileShareIntroductionMapper) {
        this.fileShareMapper = fileShareMapper;
        this.redisUtil = redisUtil;
        this.userFileMapper = userFileMapper;
        this.userLikesShareMapper = userLikesShareMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.elasticsearchOperations = elasticsearchOperations;
        this.fileShareMapping = fileShareMapping;
        this.fileShareIntroductionMapper = fileShareIntroductionMapper;
    }


    @Override
    public ServiceResult<Result, String> share(Long userId, String path, FileShareRequest fileShareRequest) {

        // 检查文件是否存在
        LambdaQueryWrapper<UserFile> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFile::getUserId, userId).eq(UserFile::getPath, path);
        UserFile userFile = userFileMapper.selectOne(qw);


        if (userFile == null || userFile.getType() == UserFile.TYPE_DIR) {
            return new ServiceResult<>(Result.FAILED, null);
        }

        // 检查是否已经分享过
        LambdaQueryWrapper<FileShare> qw1 = new LambdaQueryWrapper<>();
        qw1.eq(FileShare::getFileId, userFile.getId());
        FileShare oldFileShare = fileShareMapper.selectOne(qw1);
        if (oldFileShare != null) {

            // 检查旧分享是否过期
            Integer expire = oldFileShare.getExpire();

            if (expire == null) {
                return new ServiceResult<>(Result.ALREADY_SHARED, oldFileShare.getUuid());
            }

            Instant deadtime = oldFileShare.getCreatedAt().toInstant()  // 分享创建时间
                    .plus(expire, ChronoUnit.HOURS);   // 加 过期时间 小时

            if (!Instant.now().isAfter(deadtime)) {
                // 没过期 返回原来的uuid
                return new ServiceResult<>(Result.ALREADY_SHARED, oldFileShare.getUuid());
            }

            // 过期了，重新分享（使用新uuid）
            fileShareMapper.deleteById(oldFileShare);  // 这条之后放到消息队列
        }

        // 未分享过

        // 过期时间禁止负数输入
        if (fileShareRequest.getExpire() != null && fileShareRequest.getExpire() <= 0) {
            return new ServiceResult<>(Result.FAILED, null);
        }


        FileShare fileShare = fileShareMapping.toFileShare(fileShareRequest);
        // 分享操作是低频操作，暂时不增加redis缓存。且考虑到分享在mysql中存储需要获取id，为在架构简单的同时保持一致性，暂时不用redis
        // 后续将改用有序UUID作为mysql的主键
        String shareUUID = UUID.randomUUID().toString();
        fileShare.setFileId(userFile.getId());
        fileShare.setUuid(shareUUID);
        fileShare.setId(null);
        if (fileShare.getType() > 0) {      // 非私人分享   此逻辑后续优化
            fileShare.setType(null);
        }

        // 入库
        fileShareMapper.insert(fileShare);

        // 将分享介绍存入数据库
        FileShareIntroduction fileShareIntroduction = fileShareMapping.toShareIntroduction(fileShare, fileShareRequest);
        fileShareIntroductionMapper.insert(fileShareIntroduction);  // TODO: 使用事务

        // 向ES添加分享介绍，便于搜索
        // 一定要使用消息队列，否则延迟极高
        rabbitTemplate.convertAndSend(
                "knowcatfud.share",
                "share",
                JsonUtils.serialize(fileShareIntroduction)
        );

        return new ServiceResult<>(Result.SUCCESS, shareUUID);
    }


    @Override
    public ServiceResult<Result, String> download(String shareUUID, String password) {

        // TODO:redis缓存
        LambdaQueryWrapper<FileShare> qw = new LambdaQueryWrapper<>();
        qw.eq(FileShare::getUuid, shareUUID);
        FileShare fileShare = fileShareMapper.selectOne(qw);
        if (fileShare == null) {
            return new ServiceResult<>(Result.FAILED, null);
        }

        // 检查分享是否过期
        Integer expire = fileShare.getExpire();
        if (expire != null) {
            Instant deadtime = fileShare.getCreatedAt().toInstant()  // 分享创建时间
                    .plus(fileShare.getExpire(), ChronoUnit.HOURS);   // 加 过期时间 小时

            if (Instant.now().isAfter(deadtime)) {
                // 过期了，禁止下载并删除分享
                fileShareMapper.deleteById(fileShare);
                return new ServiceResult<>(Result.FAILED, fileShare.getUuid());
            }
        }

        // 检查是否私密分享
        if (fileShare.getType() == FileShare.PRIVATE) {
            if (!Objects.equals(password, fileShare.getPassword())) {
                return new ServiceResult<>(Result.FAILED, null);
            }
        }

        // 查找文件
        LambdaQueryWrapper<UserFile> qw1 = new LambdaQueryWrapper<>();
        qw1.eq(UserFile::getId, fileShare.getFileId());
        UserFile userFile = userFileMapper.selectOne(qw1);
        if (userFile == null || userFile.getType() == UserFile.TYPE_DIR) {
            throw new RuntimeException("分享文件未找到");
        }

        String fileToken = UUID.randomUUID().toString();
        redisUtil.hSet("download:" + fileToken, "hash", userFile.getHash());
        redisUtil.hSet("download:" + fileToken, "size", String.valueOf(userFile.getSize()));
        redisUtil.hSet("download:" + fileToken, "filename", StringUtils.getFilename(userFile.getPath()));
        redisUtil.expire("download:" + fileToken, 3, TimeUnit.MINUTES);

        return new ServiceResult<>(Result.SUCCESS, fileToken);

    }


    @Override
    public Result like(Long userId, String shareUUID) {

        String shareId = uuid2Id(shareUUID);
        // 分享不存在
        if (shareId == null) {
            return Result.FAILED;
        }

        // 用户已点赞
        if (checkIfLiked(userId, shareId)) {
            return Result.ALREADY_LIKE;
        }

        // 用户未点赞，则记录点赞并增加点赞数
        redisUtil.sAdd("share:likes:" + shareId, String.valueOf(userId));
        redisUtil.zIncrby("share:ranking", shareId, 1);

        // 同步到mysql....使用消息队列
        String message = JsonUtils.serialize(new LikeMessage(userId, Long.valueOf(shareId)));
        rabbitTemplate.convertAndSend("knowcatfud.likes", "like", message);

        return Result.SUCCESS;
    }

    @Override
    public Result likeStatus(Long userId, String shareUUID) {

        String shareId = uuid2Id(shareUUID);

        // 分享不存在
        if (shareId == null) {
            return Result.FAILED;
        }

        boolean liked = checkIfLiked(userId, shareId);

        if (liked) {
            return Result.ALREADY_LIKE;
        }

        return Result.NOT_LIKE;
    }

    @Override
    public ServiceResult<Result, String> likesCount(String shareUUID) {
        String shareId = uuid2Id(shareUUID);
        if (shareId == null) {
            return new ServiceResult<>(Result.FAILED, null);
        }

        String likesCountString;
        String infoKey = "share:info:" + shareId;

        int type = Integer.parseInt(redisUtil.hGet(infoKey, "type"));
        if (type == FileShare.PUBLIC_RANKING) {
            likesCountString = String.valueOf(redisUtil.zScore("share:ranking", shareId));
        } else {
            likesCountString = redisUtil.hGet(infoKey, "likes");
        }

        return new ServiceResult<>(Result.SUCCESS, likesCountString);
    }

    @Override
    public ServiceResult<Result, Object> getLikeRanking() {

        // TODO: 分页

        Set<ZSetOperations.TypedTuple<String>> ranking = redisUtil.zRevRangeWithScore("share:ranking", 0, 20);

        return new ServiceResult<>(Result.SUCCESS, ranking);
    }


    public boolean checkIfLiked(Long userId, String shareId) {
        Boolean b = redisUtil.sIsMember("share:likes:" + shareId, String.valueOf(userId));
        return b != null && b;  // 相当于判断b是否为null，为null则返回false，否则返回b
    }


    // 检查分享是否存在
    // 同时这个时候就做所有的缓存工作
    public String uuid2Id(String shareUUID) {

        // 缓存uuid和shareId的关联
        // 先检查是否已经缓存
        String shareId = redisUtil.get("share:uuid:" + shareUUID);
        if (shareId != null) {
            return shareId;
        }

        // 缓存中不存在，检查分享是否存在于mysql
        LambdaQueryWrapper<FileShare> qw = new LambdaQueryWrapper<>();
        qw.eq(FileShare::getUuid, shareUUID);
        FileShare fileShare = fileShareMapper.selectOne(qw);
        if (fileShare == null) {
            // 分享不存在
            return null;
        }

        // TODO: 这里还需要判断分享是否过期

        // 分享存在，加入缓存
        // 1.缓存uuid对应的share id

        Long shareIdLong = fileShare.getId();
        shareId = String.valueOf(shareIdLong);
        redisUtil.set("share:uuid_id:" + shareUUID, shareId);

        String infoKey = "share:info:" + shareId;

        // 2.缓存分享信息
        redisUtil.hSet(infoKey, "file_id", String.valueOf(fileShare.getFileId()));
        redisUtil.hSet(infoKey, "type", String.valueOf(fileShare.getType()));
        redisUtil.hSet(infoKey, "password", fileShare.getPassword());
        redisUtil.hSet(infoKey, "created_at", String.valueOf(fileShare.getCreatedAt()));
        redisUtil.hSet(infoKey, "expire", String.valueOf(fileShare.getExpire()));

        // 3.缓存分享介绍
        //        redisUtil.hSet(infoKey, "introduction", fileShare.getIntroduction());

        // 4.缓存点赞列表 和 点赞数
        Long likesCount = 0L;
        List<String> likeUsers = userLikesShareMapper.getLikeUsersString(shareIdLong);
        if (!likeUsers.isEmpty()) {
            redisUtil.sAdd("share:likes:" + shareId, likeUsers);
            likesCount = redisUtil.sSize("share:likes:" + shareId);
        }

        // 公开且排行分享
        if (fileShare.getType() == FileShare.PUBLIC_RANKING) {
            redisUtil.zAdd("share:ranking", shareId, likesCount);
        } else {
            // 非公开分享 将获赞数量缓存于分享信息，不参与排行
            redisUtil.hSet(infoKey, "likes", String.valueOf(likesCount));
        }

        // 为以上key设置过期时间6小时
        redisUtil.expire("share:uuid_id:" + shareUUID, 6, TimeUnit.HOURS);
        redisUtil.expire("share:likes:" + shareId, 6, TimeUnit.HOURS);
        redisUtil.expire("share:info:" + shareId, 6, TimeUnit.HOURS);
        // 参与排行榜的需要额外编写缓存过期处理代码，但是，参与排行榜的在缓存过期后仍然要存在于排行榜上，所以反而无需为其设计缓存过期时间
        // TODO：从排行榜移除分享过期/手动删除的分享

        return shareId;
    }

    @Override
    public ServiceResult<Result, List<FileShareIntroduction>> search(String keywords) {

        // TODO: 分页
        Query query = new CriteriaQuery(
                new Criteria("introduction").matches(keywords)
                        .or("title").matches(keywords)
        );
        List<FileShareIntroduction> r = elasticsearchOperations
                .search(query, FileShareIntroduction.class)
                .get()
                .limit(20)
                .map(SearchHit::getContent)
                .toList();
        return new ServiceResult<>(Result.SUCCESS, r);
    }

    @Override
    public ServiceResult<Result, Object> getShares(Long userId) {
        List<FileShare> fileShares = fileShareMapper.getShares(userId);
        return new ServiceResult<>(Result.SUCCESS, fileShares);
    }

    @Override
    public Result delete(Long userId, String shareUuid) {
        String shareIdString = uuid2Id(shareUuid);
        if (shareIdString == null) {
            return Result.SHARE_NOT_FOUND;
        }
        Long shareId = Long.valueOf(shareIdString);
        Long realUserId = fileShareMapper.getUserIdByShareId(shareId);
        if (userId.equals(realUserId)) {
            fileShareMapper.deleteById(shareId);
            fileShareIntroductionMapper.deleteById(shareId);

            // TODO: 删除点赞记录
            // TODO: 使用消息队列从ES中删除文件信息

            return Result.SUCCESS;
        }

        return Result.INVALID_ACCESS;
    }


}
