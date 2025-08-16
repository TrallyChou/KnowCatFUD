package life.trally.knowcatfud.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import life.trally.knowcatfud.dao.FileShareMapper;
import life.trally.knowcatfud.dao.UserFileMapper;
import life.trally.knowcatfud.dao.UserLikesShareMapper;
import life.trally.knowcatfud.pojo.FileShare;
import life.trally.knowcatfud.pojo.UserFile;
import life.trally.knowcatfud.pojo.UserLikesShare;
import life.trally.knowcatfud.service.ServiceResult;
import life.trally.knowcatfud.service.interfaces.FileShareService;
import life.trally.knowcatfud.utils.RedisUtil;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

        // 然后检查文件是否存在
        LambdaQueryWrapper<UserFile> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFile::getUserId, userId).eq(UserFile::getPath, path);
        UserFile userFile = userFileMapper.selectOne(qw);


        if (userFile == null || userFile.getType() == UserFile.TYPE_DIR) {
            return new ServiceResult<>(Result.FAILED, null);
        }

        // 检查是否已经分享过   // 后续增加redis缓存
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
        if (fileShare.getExpire() <= 0) {
            return new ServiceResult<>(Result.FAILED, null);
        }
        String shareUUID = UUID.randomUUID().toString();
        fileShare.setFileId(userFile.getId());
        fileShare.setUuid(shareUUID);
        fileShare.setId(null);
        fileShare.setLikes(0);

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


        // 查分享id（以后改为优先用缓存）
        LambdaQueryWrapper<FileShare> qw = new LambdaQueryWrapper<>();
        qw.eq(FileShare::getUuid, shareUUID);
        FileShare fileShare = fileShareMapper.selectOne(qw);

        // 分享不存在
        if(fileShare == null){
            return Result.FAILED;
        }

        Long shareId = fileShare.getId();



        // 查点赞情况，避免重复点赞
        LambdaQueryWrapper<UserLikesShare> qw1 = new LambdaQueryWrapper<>();
        qw1.eq(UserLikesShare::getShareId, shareId)
                .eq(UserLikesShare::getUserId, userId);
        UserLikesShare userLikesShare = userLikesShareMapper.selectOne(qw1);
        if (userLikesShare != null) {
            return Result.ALREADY_LIKE;
        }

        // 点赞
        userLikesShare = new UserLikesShare();
        userLikesShare.setUserId(userId);
        userLikesShare.setShareId(shareId);
        userLikesShareMapper.insert(userLikesShare);

        // 更新share的likes数量
        fileShare.setLikes(fileShare.getLikes() + 1);
        fileShareMapper.updateById(fileShare);

        return Result.SUCCESS;
    }

    @Override
    public Result likeStatus(Long userId, String shareUUID) {

        // 重复代码后续优化

        // 查分享id（以后改为优先用缓存）
        LambdaQueryWrapper<FileShare> qw = new LambdaQueryWrapper<>();
        qw.eq(FileShare::getUuid, shareUUID);
        FileShare fileShare = fileShareMapper.selectOne(qw);

        // 分享不存在
        if(fileShare == null){
            return Result.FAILED;
        }

        Long shareId = fileShare.getId();



        // 查点赞情况，避免重复点赞
        LambdaQueryWrapper<UserLikesShare> qw1 = new LambdaQueryWrapper<>();
        qw1.eq(UserLikesShare::getShareId, shareId)
                .eq(UserLikesShare::getUserId, userId);
        UserLikesShare userLikesShare = userLikesShareMapper.selectOne(qw1);
        if (userLikesShare != null) {
            return Result.ALREADY_LIKE;
        }

        return Result.NOT_LIKE;
    }

    @Override
    public ServiceResult<Result, Object> getLikeRanking() {

        // TODO: 重写点赞
        // TODO: 分页

        Set<ZSetOperations.TypedTuple<String>> ranking = redisUtil.zRevRangeWithScore("share:uuid_ranking", 0, 20);
        if (ranking == null) {
            return new ServiceResult<>(Result.FAILED, null);
        }
        return new ServiceResult<>(Result.SUCCESS, ranking);
    }


}
