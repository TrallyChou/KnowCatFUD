package life.trally.knowcatfud.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import life.trally.knowcatfud.dao.FileShareMapper;
import life.trally.knowcatfud.dao.UserFileMapper;
import life.trally.knowcatfud.dao.UserLikesShareMapper;
import life.trally.knowcatfud.pojo.FileShare;
import life.trally.knowcatfud.pojo.UserFile;
import life.trally.knowcatfud.service.ServiceResult;
import life.trally.knowcatfud.service.interfaces.FileShareService;
import life.trally.knowcatfud.utils.RedisUtil;
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

    private final RedisUtil redisUtil;

    private final UserFileMapper userFileMapper;

    private final UserLikesShareMapper userLikesShareMapper;

    public FileShareServiceImpl(FileShareMapper fileShareMapper, RedisUtil redisUtil, UserFileMapper userFileMapper, UserLikesShareMapper userLikesShareMapper) {
        this.fileShareMapper = fileShareMapper;
        this.redisUtil = redisUtil;
        this.userFileMapper = userFileMapper;
        this.userLikesShareMapper = userLikesShareMapper;
    }


    @Override
    public ServiceResult<Result, String> share(Long userId, String path, FileShare fileShare) {

        // 检查文件是否存在
        LambdaQueryWrapper<UserFile> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFile::getUserId, userId).eq(UserFile::getPath, path);
        UserFile userFile = userFileMapper.selectOne(qw);


        if (userFile == null || userFile.getType() == UserFile.TYPE_DIR) {
            return new ServiceResult<>(Result.FAILED, null);
        }

        // 检查是否已经分享过   // 分享是低频操作，所以暂不增加redis缓存
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
        if (fileShare.getExpire() != null && fileShare.getExpire() <= 0) {
            return new ServiceResult<>(Result.FAILED, null);
        }

        // 分享操作是低频操作，暂时不增加redis缓存。且考虑到分享在mysql中存储需要获取自增id，为保证一致性，暂时不用redis
        // 后续将改用有序UUID作为mysql的主键
        String shareUUID = UUID.randomUUID().toString();
        fileShare.setFileId(userFile.getId());
        fileShare.setUuid(shareUUID);
        fileShare.setId(null);

        fileShareMapper.insert(fileShare);

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

        // TODO: redis缓存

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
//        userLikesShare = new UserLikesShare();
//        userLikesShare.setUserId(userId);
//        userLikesShare.setShareId(shareId);
//        userLikesShareMapper.insert(userLikesShare);

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
        redisUtil.hSet(infoKey, "introduction", fileShare.getIntroduction());
        redisUtil.hSet(infoKey, "created_at", String.valueOf(fileShare.getCreatedAt()));
        redisUtil.hSet(infoKey, "expire", String.valueOf(fileShare.getExpire()));

        // 3.缓存点赞列表
        List<String> likeUsers = userLikesShareMapper.getLikeUsers(shareIdLong);
        redisUtil.sAdd("share:likes:" + shareId, likeUsers);

        // 缓存点赞数...

        Long likesCount = redisUtil.sSize("share:likes:" + shareId);
        // 公开且排行分享
        if (fileShare.getType() == FileShare.PUBLIC_RANKING) {
            redisUtil.zAdd("share:ranking", shareId, likesCount);
        } else {
            // 非公开分享 将获赞数量缓存于分享信息
            redisUtil.hSet(infoKey, "likes", String.valueOf(likesCount));
        }

        // 为以上key设置过期时间3天
        redisUtil.expire("share:uuid_id:" + shareUUID, 6, TimeUnit.HOURS);
        redisUtil.expire("share:likes:" + shareId, 6, TimeUnit.HOURS);
        redisUtil.expire("share:info:" + shareId, 6, TimeUnit.HOURS);
        // 点赞数由于使用有序集合存储，所以需要额外处理过期，但数据量仅为帖子数量，足够小，所以暂时不做过期处理...
        // 而且实际上，让点赞数较低 的

        return shareId;
    }


}
