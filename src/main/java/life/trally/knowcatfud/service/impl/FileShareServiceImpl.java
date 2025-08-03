package life.trally.knowcatfud.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import life.trally.knowcatfud.dao.FilePathInfoMapper;
import life.trally.knowcatfud.pojo.FilePathInfo;
import life.trally.knowcatfud.pojo.ShareInfo;
import life.trally.knowcatfud.service.ServiceResult;
import life.trally.knowcatfud.service.interfaces.FileShareService;
import life.trally.knowcatfud.service.interfaces.UserFileDownloadService;
import life.trally.knowcatfud.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import static life.trally.knowcatfud.utils.AccessCheckUtil.checkAccess;

@Service
public class FileShareServiceImpl implements FileShareService {

    @Autowired
    FilePathInfoMapper filePathInfoMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    UserFileDownloadService userFileDownloadService;


    @Override
    public ServiceResult<Result, String> share(String token, String username, String path, ShareInfo shareInfo) {

        if (!checkAccess(token, username)) {
            return new ServiceResult<>(Result.INVALID_ACCESS, null);
        }

        // 然后检查文件是否存在

        String queryPath = username + path;
        QueryWrapper<FilePathInfo> qw = new QueryWrapper<>();
        qw.eq("user_path", queryPath);
        FilePathInfo filePathInfo = filePathInfoMapper.selectOne(qw);

        if (filePathInfo == null || filePathInfo.getType() == FilePathInfo.TYPE_DIR) {
            return new ServiceResult<>(Result.FILED, null);
        }


        try {

            // 检查是否已分享过
            String fileUUID = redisUtil.get("share:file_uuid:" + queryPath);
            if (fileUUID != null) {
                return new ServiceResult<>(Result.ALREADY_SHARED, fileUUID);
            }
            fileUUID = UUID.randomUUID().toString();
            redisUtil.set("share:file_uuid:" + queryPath, fileUUID, 7 * 24);


            String key = "share:uuid_info:" + fileUUID;
            redisUtil.hSet(key, "file", queryPath);
            redisUtil.expire(key, 7 * 24);
            if (shareInfo.isSharePublic() && shareInfo.getPassword() == null) {
                redisUtil.hSet(key, "public", "true");
                redisUtil.zAdd("share:uuid_ranking", fileUUID, 0);
            } else {
                redisUtil.hSet(key, "public", "false");
                redisUtil.hSet(key, "password", shareInfo.getPassword());
            }

            return new ServiceResult<>(Result.SUCCESS, fileUUID);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public ResponseEntity<Resource> download(String shareUUID, String password) {
        String passwd = redisUtil.hGet("share:uuid_info:" + shareUUID, "password");
        String fileUserPath = null;

        // 检查分享码
        if (passwd == null || passwd.equals(password)) {
            fileUserPath = redisUtil.hGet("share:uuid_info:" + shareUUID, "file");
        }

        if (fileUserPath == null) {
            return null;
        }
        try {
            QueryWrapper<FilePathInfo> qw = new QueryWrapper<>();
            qw.eq("user_path", fileUserPath);
            FilePathInfo filePathInfo = filePathInfoMapper.selectOne(qw);
            return userFileDownloadService.download(filePathInfo);
        } catch (Exception e) {
            return null;
        }

    }


    @Override
    public Result like(String shareUUID) {

        // TODO: 限制每个用户只可点赞一次

        String key = "share:uuid_info:" + shareUUID;

        if (!redisUtil.exists(key)) {
            return Result.SHARE_NOT_FOUND;
        }
        if (!"true".equals(redisUtil.hGet(key, "public"))) {
            return Result.FILED;
        }
        redisUtil.zIncrby("share:uuid_ranking", shareUUID, 1);
        return Result.SUCCESS;
    }

    @Override
    public ServiceResult<Result, Object> getLikeRanking() {

        // TODO: 分页

        Set<ZSetOperations.TypedTuple<String>> ranking = redisUtil.zRevRangeWithScore("share:uuid_ranking", 0, 20);
        if (ranking == null) {
            return new ServiceResult<>(Result.FILED, null);
        }
        return new ServiceResult<>(Result.SUCCESS, ranking);
    }


}
