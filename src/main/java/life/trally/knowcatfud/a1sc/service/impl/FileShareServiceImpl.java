package life.trally.knowcatfud.a1sc.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import life.trally.knowcatfud.pojo.entity.FileShare;
import life.trally.knowcatfud.pojo.entity.FileShareIntroduction;
import life.trally.knowcatfud.pojo.entity.UserFile;
import life.trally.knowcatfud.mapper.FileShareIntroductionMapper;
import life.trally.knowcatfud.mapper.FileShareMapper;
import life.trally.knowcatfud.mapper.UserFileMapper;
import life.trally.knowcatfud.mapper.UserLikesShareMapper;
import life.trally.knowcatfud.mapping.FileShareMapping;
import life.trally.knowcatfud.rabbitmq.messages.DeleteShareMessage;
import life.trally.knowcatfud.rabbitmq.messages.LikeMessage;
import life.trally.knowcatfud.pojo.request.ShareRequest;
import life.trally.knowcatfud.pojo.response.FileShareSearchResponse;
import life.trally.knowcatfud.pojo.response.GetShareResponse;
import life.trally.knowcatfud.pojo.response.GetSharesResponse;
import life.trally.knowcatfud.a1sc.service.ServiceResult;
import life.trally.knowcatfud.a1sc.service.interfaces.FileDownloadService;
import life.trally.knowcatfud.a1sc.service.interfaces.FileShareService;
import life.trally.knowcatfud.utils.JsonUtils;
import life.trally.knowcatfud.utils.RedisUtils;
import life.trally.knowcatfud.utils.TimeUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class FileShareServiceImpl implements FileShareService {

    private final FileShareMapper fileShareMapper;
    private final RedisUtils redisUtils;
    private final UserFileMapper userFileMapper;
    private final UserLikesShareMapper userLikesShareMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ElasticsearchOperations elasticsearchOperations;
    private final FileShareMapping fileShareMapping;
    private final FileShareIntroductionMapper fileShareIntroductionMapper;
    private final TransactionTemplate transactionTemplate;
    private final FileDownloadService fileDownloadService;

    public FileShareServiceImpl(FileShareMapper fileShareMapper, RedisUtils redisUtils, UserFileMapper userFileMapper, UserLikesShareMapper userLikesShareMapper, RabbitTemplate rabbitTemplate, ElasticsearchOperations elasticsearchOperations, FileShareMapping fileShareMapping, FileShareIntroductionMapper fileShareIntroductionMapper, TransactionTemplate transactionTemplate, FileDownloadService fileDownloadService) {
        this.fileShareMapper = fileShareMapper;
        this.redisUtils = redisUtils;
        this.userFileMapper = userFileMapper;
        this.userLikesShareMapper = userLikesShareMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.elasticsearchOperations = elasticsearchOperations;
        this.fileShareMapping = fileShareMapping;
        this.fileShareIntroductionMapper = fileShareIntroductionMapper;
        this.transactionTemplate = transactionTemplate;
        this.fileDownloadService = fileDownloadService;
    }


    @Override
    public ServiceResult<Result, String> share(Long userId, String path, ShareRequest shareRequest) {

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
            fileShareMapper.deleteById(oldFileShare);
        }

        // 未分享过

        if (shareRequest.getExpire() != null) {
            // 过期时间禁止负数输入
            if (shareRequest.getExpire() <= 0) {
                return new ServiceResult<>(Result.FAILED, null);
            }

            // 排行分享不允许设置过期时间
            if (shareRequest.getType() == FileShare.PUBLIC_RANKING) {
                return new ServiceResult<>(Result.FAILED, null);
            }
        }

        FileShare fileShare = fileShareMapping.toFileShare(shareRequest);
        // 分享操作是低频操作，暂时不增加redis缓存。且考虑到分享在mysql中存储需要获取id，为在架构简单的同时保持一致性，暂时不用redis
        // 后续将改用有序UUID作为mysql的主键
        String shareUUID = UUID.randomUUID().toString();
        fileShare.setFileId(userFile.getId());
        fileShare.setUuid(shareUUID);
        fileShare.setId(null);
        if (fileShare.getType() > 3) {
            fileShare.setType(FileShare.PRIVATE);
        }

        Long id = IdWorker.getId();
        fileShare.setId(id);
        // 分享介绍
        FileShareIntroduction fileShareIntroduction = fileShareMapping.toShareIntroduction(fileShare, shareRequest);

        Boolean executeResult = transactionTemplate.execute(
                status -> {
                    try {
                        // 分享入库
                        fileShareMapper.insert(fileShare);
                        // 分享介绍入库
                        fileShareIntroductionMapper.insert(fileShareIntroduction);
                        return Boolean.TRUE;
                    } catch (Exception e) {
                        status.setRollbackOnly();
                    }
                    return Boolean.FALSE;
                }

        );

        if (Boolean.TRUE.equals(executeResult)) {
            // 向ES添加分享介绍，便于搜索
            // 一定要使用消息队列，否则延迟极高
            if (fileShare.getType() != FileShare.PRIVATE) {
                rabbitTemplate.convertAndSend(
                        "knowcatfud.share",
                        "share",
                        JsonUtils.serialize(fileShareIntroduction)
                );
            }
            return new ServiceResult<>(Result.SUCCESS, shareUUID);
        } else {
            return new ServiceResult<>(Result.FAILED, null);
        }

    }

    @Override
    public ServiceResult<Result, GetShareResponse> getShare(String shareUUID, String password) {

        Long id = uuid2Id(shareUUID);
        if (id == null) {
            return new ServiceResult<>(Result.SHARE_NOT_FOUND, null);
        }
        Map<String, String> shareInfo = redisUtils.hGetAll("share:info:" + id);
        // 查询
        String expireString = shareInfo.get("expire");
        Integer expire = expireString == null ? null : Integer.valueOf(expireString);
        var r = new GetShareResponse(
                Integer.valueOf(shareInfo.get("type")),
                expire,
                shareInfo.get("title"),
                shareInfo.get("introduction"),
                shareInfo.get("created_at")
        );

        return new ServiceResult<>(Result.SUCCESS, r);
    }


    @Override
    public ServiceResult<Result, String> download(String shareUUID, String password) {

        Long shareId = uuid2Id(shareUUID);
        if (shareId == null) {
            return new ServiceResult<>(Result.SHARE_NOT_FOUND, null);
        }


        String infoKey = "share:info:" + shareId;

        // 检查是否私密分享
        if (String.valueOf(FileShare.PRIVATE).equals(redisUtils.hGet(infoKey, "type"))) {
            if (!Objects.equals(password, redisUtils.hGet(infoKey, "password"))) {
                return new ServiceResult<>(Result.FAILED, null);
            }
        }

        String fileHash = redisUtils.hGet(infoKey, "hash");
        String fileSize;
        String fileName;
        if (fileHash == null) {
            // 缓存
            Long fileId = Long.valueOf(redisUtils.hGet(infoKey, "file_id"));
            UserFile userFile = userFileMapper.selectById(fileId);  // 认为非空，因为删除逻辑会删除缓存，shareId==null
            fileHash = userFile.getHash();
            fileSize = String.valueOf(userFile.getSize());
            fileName = userFile.getFilename();
        } else {
            fileSize = redisUtils.hGet(infoKey, "size");
            fileName = redisUtils.hGet(infoKey, "filename");
        }

        String downloadToken = fileDownloadService.generateDownloadToken(fileHash, fileSize, fileName);
        return new ServiceResult<>(Result.SUCCESS, downloadToken);
    }

    @Override
    public Result like(Long userId, String shareUUID) {

        Long shareId = uuid2Id(shareUUID);
        // 分享不存在
        if (shareId == null) {
            return Result.FAILED;
        }

        // 用户已点赞
        if (checkIfLiked(userId, shareId)) {
            return Result.ALREADY_LIKE;
        }

        // 用户未点赞，则记录点赞并增加点赞数
        redisUtils.sAdd("share:likes:" + shareId, String.valueOf(userId));
        redisUtils.zIncrby("share:ranking", String.valueOf(shareUUID), 1);

        // 同步到mysql....使用消息队列
        String message = JsonUtils.serialize(new LikeMessage(userId, shareId));
        rabbitTemplate.convertAndSend("knowcatfud.likes", "like", message);

        return Result.SUCCESS;
    }

    @Override
    public Result likeStatus(Long userId, String shareUUID) {

        Long shareId = uuid2Id(shareUUID);

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
    public ServiceResult<Result, Integer> likesCount(String shareUUID) {
        Long shareId = uuid2Id(shareUUID);
        if (shareId == null) {
            return new ServiceResult<>(Result.FAILED, null);
        }

        String likesCountString;
        String infoKey = "share:info:" + shareId;

        int type = Integer.parseInt(redisUtils.hGet(infoKey, "type"));
        if (type == FileShare.PUBLIC_RANKING) {
            likesCountString = String.valueOf(redisUtils.zScore("share:ranking", String.valueOf(shareUUID)));
        } else {
            likesCountString = redisUtils.hGet(infoKey, "likes");
        }
        Integer likesCount = likesCountString == null ? null : Float.valueOf(likesCountString).intValue();
        return new ServiceResult<>(Result.SUCCESS, likesCount);
    }

    @Override
    public ServiceResult<Result, Set<ZSetOperations.TypedTuple<String>>> getLikeRankingByPage(int page) {

        int start = (page - 1) * 10;
        int end = page * 10 - 1;
        var r = getLikeRanking(start, end);
        return new ServiceResult<>(r.getResult(), r.getData());
    }

    public ServiceResult<Result, Set<ZSetOperations.TypedTuple<String>>> getLikeRanking(int start, int end) {

        Set<ZSetOperations.TypedTuple<String>> ranking = redisUtils.zRevRangeWithScore("share:ranking", start, end);
        return new ServiceResult<>(Result.SUCCESS, ranking);

    }


    public boolean checkIfLiked(Long userId, Long shareId) {
        Boolean b = redisUtils.sIsMember("share:likes:" + shareId, String.valueOf(userId));
        return b != null && b;  // 相当于判断b是否为null，为null则返回false，否则返回b
    }


    // 检查分享是否存在
    // 同时这个时候就做所有的缓存工作
    public Long uuid2Id(String shareUUID) {

        // 缓存uuid和shareId的关联
        // 先检查是否已经缓存
        String shareId = redisUtils.get("share:uuid:" + shareUUID);
        if (shareId != null) {
            if (shareId.equals("0")) {        // 已删除，禁止访问
                return null;
            }
            return Long.valueOf(shareId);
        }

        // 缓存中不存在，检查分享是否存在于mysql
        LambdaQueryWrapper<FileShare> qw = new LambdaQueryWrapper<>();
        qw.eq(FileShare::getUuid, shareUUID);
        FileShare fileShare = fileShareMapper.selectOne(qw);
        if (fileShare == null) {
            // 分享不存在
            return null;
        }

        // 分享存在
        Timestamp createdAt = fileShare.getCreatedAt();
        Integer expire = fileShare.getExpire();

        // 分享已过期
        if (expire != null && TimeUtils.expired(createdAt, expire, ChronoUnit.HOURS)) {
            return null;
        }

        // 分享存在，加入缓存
        // 1.缓存uuid对应的share id

        Long shareIdLong = fileShare.getId();
        shareId = String.valueOf(shareIdLong);

        redisUtils.set("share:uuid_id:" + shareUUID, shareId);

        String infoKey = "share:info:" + shareId;

        // 2.缓存分享信息
        redisUtils.hSet(infoKey, "file_id", String.valueOf(fileShare.getFileId()));
        redisUtils.hSet(infoKey, "uuid", shareUUID);
        redisUtils.hSet(infoKey, "type", String.valueOf(fileShare.getType()));
        redisUtils.hSet(infoKey, "password", fileShare.getPassword());
        redisUtils.hSet(infoKey, "created_at", String.valueOf(createdAt));
        if (expire != null) {
            redisUtils.hSet(infoKey, "expire", String.valueOf(expire));
        }

        // 3.缓存分享介绍

        // 从mysql中读取
        FileShareIntroduction fileShareIntroduction =
                fileShareIntroductionMapper.selectById(shareIdLong);

        redisUtils.hSet(infoKey, "title", fileShareIntroduction.getTitle());
        redisUtils.hSet(infoKey, "introduction", fileShareIntroduction.getIntroduction());

        // 4.缓存点赞列表 和 点赞数
        Long likesCount = 0L;
        List<String> likeUsers = userLikesShareMapper.getLikeUsersString(shareIdLong);
        if (!likeUsers.isEmpty()) {
            redisUtils.sAdd("share:likes:" + shareId, likeUsers);
            likesCount = redisUtils.sSize("share:likes:" + shareId);
        }

        // 公开且排行分享
        if (fileShare.getType() == FileShare.PUBLIC_RANKING) {
            redisUtils.zAdd("share:ranking", shareUUID, likesCount);
        } else {
            // 非公开分享 将获赞数量缓存于分享信息，不参与排行
            redisUtils.hSet(infoKey, "likes", String.valueOf(likesCount));
        }

        long keyExpire = 30;
        if (expire != null) {
            Instant expireTime = createdAt.toInstant().plus(expire, ChronoUnit.HOURS);// 分享过期时刻
            long untilExpire = Duration.between(
                    expireTime,
                    Instant.now()
            ).toMinutes();   // Instant.now() - expireTime
            // 缓存的过期时间  tips: 前面已经判断过分享是否过期
            if (untilExpire < 30) {
                keyExpire = untilExpire;
            }
        }

        redisUtils.expire("share:uuid_id:" + shareUUID, keyExpire, TimeUnit.MINUTES);
        redisUtils.expire("share:likes:" + shareId, keyExpire, TimeUnit.MINUTES);
        redisUtils.expire(infoKey, keyExpire, TimeUnit.MINUTES);

        return Long.valueOf(shareId);
    }

    @Override
    public ServiceResult<Result, List<FileShareSearchResponse>> search(String keywords, int page) {

        if (page <= 0) return new ServiceResult<>(Result.FAILED, null);

        Pageable pageable = PageRequest.of(page - 1, 10);

        Query matchQuery = new CriteriaQuery(
                new Criteria("introduction").matches(keywords)
                        .or("title").matches(keywords)
        );
        NativeQuery nativeQuery = new NativeQueryBuilder()
                .withQuery(matchQuery)
                .withPageable(pageable)
                .build();

        List<FileShareIntroduction> fileShareIntroductions = elasticsearchOperations
                .search(nativeQuery, FileShareIntroduction.class)
                .map(SearchHit::getContent)
                .toList();
        var r = fileShareMapping.toFileShareSearchResponse(fileShareIntroductions);
        return new ServiceResult<>(Result.SUCCESS, r);
    }

    @Override
    public ServiceResult<Result, List<GetSharesResponse>> getShares(Long userId) {
        List<GetSharesResponse> fileShares = fileShareMapper.getMyShares(userId);
        return new ServiceResult<>(Result.SUCCESS, fileShares);
    }

    @Override
    public Result delete(Long userId, String shareUuid) {
        Long shareId = uuid2Id(shareUuid);
        if (shareId == null) {
            return Result.SHARE_NOT_FOUND;
        }

        Long shareUserId = fileShareMapper.getUserIdByShareId(shareId);
        if (userId.equals(shareUserId)) {

            // 禁止访问
            redisUtils.set("share:uuid_id:" + shareUuid, "0");

            // 使用消息队列完成删除
            rabbitTemplate.convertAndSend("knowcatfud.share", "delete",
                    JsonUtils.serialize(new DeleteShareMessage(shareId, shareUuid)));


            return Result.SUCCESS;
        }

        return Result.INVALID_ACCESS;
    }


}
