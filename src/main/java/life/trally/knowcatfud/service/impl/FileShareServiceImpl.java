package life.trally.knowcatfud.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import life.trally.knowcatfud.dao.FilePathInfoMapper;
import life.trally.knowcatfud.pojo.FilePathInfo;
import life.trally.knowcatfud.service.ServiceResult;
import life.trally.knowcatfud.service.interfaces.FileShareService;
import life.trally.knowcatfud.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static life.trally.knowcatfud.utils.AccessCheckUtil.checkAccess;

@Service
public class FileShareServiceImpl implements FileShareService {

    @Autowired
    FilePathInfoMapper filePathInfoMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public ServiceResult<Result, String> share(String token, String username, String path) {

        if (!checkAccess(token, username)) {
            return new ServiceResult<>(Result.INVALID_ACCESS, null);
        }

        // 然后检查文件是否存在

        String queryPath = username + path;
        QueryWrapper<FilePathInfo> qw = new QueryWrapper<>();
        qw.eq("user_path", queryPath);
        FilePathInfo filePathInfo = filePathInfoMapper.selectOne(qw);

        if (filePathInfo == null || filePathInfo.getType() == FilePathInfo.TYPE_DIR) {
            return new ServiceResult<>(Result.SHARE_FAILED, null);
        }


        try {

            // 检查是否已分享过
            String fileUUID = redisUtil.get("share:file_uuid:" + queryPath);
            if (fileUUID != null) {
                return new ServiceResult<>(Result.ALREADY_SHARED, fileUUID);
            }
            fileUUID = UUID.randomUUID().toString();

            // 后续再进行优化，将更多文件信息存入redis
            redisUtil.set("share:file_uuid:" + queryPath, fileUUID, 7 * 24);
            redisUtil.set("share:uuid_file:" + fileUUID, queryPath, 7 * 24); // 期限暂定为一周
            return new ServiceResult<>(Result.SHARE_SUCCESS, fileUUID);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public ResponseEntity<Resource> download(String shareUUID) {

        String fileUserPath = redisUtil.get("share:uuid_file:" + shareUUID);
        if (fileUserPath == null) {
            return null;
        }
        try {
            QueryWrapper<FilePathInfo> qw = new QueryWrapper<>();
            qw.eq("user_path", fileUserPath);
            FilePathInfo filePathInfo = filePathInfoMapper.selectOne(qw);

            String fileName = filePathInfo.getHash() + filePathInfo.getSize();
            Path filePath = Paths.get("files/", fileName);
            Resource resource = new UrlResource(filePath.toUri());
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                    + StringUtils.getFilename(fileUserPath) + "\"");
            return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);

        } catch (Exception e) {
            return null;
        }

    }
}
