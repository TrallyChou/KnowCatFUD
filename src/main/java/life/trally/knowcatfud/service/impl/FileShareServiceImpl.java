package life.trally.knowcatfud.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import life.trally.knowcatfud.dao.FileShareMapper;
import life.trally.knowcatfud.dao.UserFileMapper;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static life.trally.knowcatfud.utils.AccessCheckUtil.checkAccess;

@Service
public class FileShareServiceImpl implements FileShareService {

    private final FileShareMapper fileShareMapper;

    private final RedisUtil redisUtil;

    private final UserFileMapper userFileMapper;

    public FileShareServiceImpl(FileShareMapper fileShareMapper, RedisUtil redisUtil, UserFileMapper userFileMapper) {
        this.fileShareMapper = fileShareMapper;
        this.redisUtil = redisUtil;
        this.userFileMapper = userFileMapper;
    }


    @Override
    public ServiceResult<Result, String> share(String token, String username, String path, FileShare fileShare) {

        if (!checkAccess(token, username)) {
            return new ServiceResult<>(Result.INVALID_ACCESS, null);
        }

        // 然后检查文件是否存在
        LambdaQueryWrapper<UserFile> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFile::getUsername, username).eq(UserFile::getPath, path);
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


//        try {
//
//            // 检查是否已分享过
//            String fileUUID = redisUtil.get("share:file_uuid:" + username + ":" + userFile.getPath());
//            if (fileUUID != null) {
//                return new ServiceResult<>(Result.ALREADY_SHARED, fileUUID);
//            }
//            fileUUID = UUID.randomUUID().toString();
//            redisUtil.set("share:file_uuid:" + queryPath, fileUUID, 7 * 24);
//
//
//            String key = "share:uuid_info:" + fileUUID;
//            redisUtil.hSet(key, "file", queryPath);
//            redisUtil.expire(key, 7 * 24);
////            if (shareInfo.isSharePublic() && shareInfo.getPassword() == null) {
////                redisUtil.hSet(key, "public", "true");
////                redisUtil.zAdd("share:uuid_ranking", fileUUID, 0);
////            } else {
////                redisUtil.hSet(key, "public", "false");
////                redisUtil.hSet(key, "password", shareInfo.getPassword());
////            }
//
//            return new ServiceResult<>(Result.SUCCESS, fileUUID);
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }


    @Override
    public ServiceResult<Result, String> download(String shareUUID, String password) {

        // 后续增加redis缓存
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


//        String passwd = redisUtil.hGet("share:uuid_info:" + shareUUID, "password");
//        String fileUserPath = null;
//
//        // 检查分享码
//        if (passwd == null || passwd.equals(password)) {
//            fileUserPath = redisUtil.hGet("share:uuid_info:" + shareUUID, "file");
//        }
//
//        if (fileUserPath == null) {
//            return new ServiceResult<>(Result.FAILED, null);
//        }
//        try {
//            QueryWrapper<FilePathInfo> qw = new QueryWrapper<>();
//            qw.eq("user_path", fileUserPath);
//            FilePathInfo filePathInfo = filePathInfoMapper.selectOne(qw);
//
//            String fileToken = UUID.randomUUID().toString();
//            redisUtil.hSet("download:" + fileToken, "hash", filePathInfo.getHash());
//            redisUtil.hSet("download:" + fileToken, "size", String.valueOf(filePathInfo.getSize()));
//            redisUtil.hSet("download:" + fileToken, "filename", StringUtils.getFilename(filePathInfo.getUserPath()));
//            redisUtil.expire("download:" + fileToken, 3, TimeUnit.MINUTES);
//
//            return new ServiceResult<>(Result.SUCCESS, fileToken);
//        } catch (Exception e) {
//            return new ServiceResult<>(Result.FAILED, null);
//        }
    }


    @Override
    public Result like(String shareUUID) {

        // TODO: 限制每个用户只可点赞一次

        String key = "share:uuid_info:" + shareUUID;

        if (!redisUtil.exists(key)) {
            return Result.SHARE_NOT_FOUND;
        }
        if (!"true".equals(redisUtil.hGet(key, "public"))) {
            return Result.FAILED;
        }
        redisUtil.zIncrby("share:uuid_ranking", shareUUID, 1);
        return Result.SUCCESS;
    }

    @Override
    public ServiceResult<Result, Object> getLikeRanking() {

        // TODO: 分页

        Set<ZSetOperations.TypedTuple<String>> ranking = redisUtil.zRevRangeWithScore("share:uuid_ranking", 0, 20);
        if (ranking == null) {
            return new ServiceResult<>(Result.FAILED, null);
        }
        return new ServiceResult<>(Result.SUCCESS, ranking);
    }


}
