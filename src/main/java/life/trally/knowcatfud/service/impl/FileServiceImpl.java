package life.trally.knowcatfud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import life.trally.knowcatfud.dao.FilePathInfoMapper;
import life.trally.knowcatfud.pojo.FilePathInfo;
import life.trally.knowcatfud.service.ServiceResult;
import life.trally.knowcatfud.service.interfaces.FileDownloadService;
import life.trally.knowcatfud.service.interfaces.FileService;
import life.trally.knowcatfud.utils.FileUtil;
import life.trally.knowcatfud.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static life.trally.knowcatfud.utils.AccessCheckUtil.checkAccess;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    FilePathInfoMapper filePathInfoMapper;

    @Autowired
    FileDownloadService fileDownloadService;
    @Autowired
    private RedisUtil redisUtil;


    @Override
    public Result uploadOrMkdir(String token, String username, MultipartFile multipartFile, FilePathInfo filePathInfo) {

        String userPath = filePathInfo.getUserPath();
        if (!checkAccess(token, username)  // 非法访问
                || !userPath.startsWith(username + "/")
        ) {
            return Result.INVALID_ACCESS;
        }

        // 查询文件是否重名
        QueryWrapper<FilePathInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_path", userPath);
        FilePathInfo oldFilePathInfo = filePathInfoMapper.selectOne(queryWrapper);
        if (oldFilePathInfo != null) {  // 文件已存在
            return Result.FILE_ALREADY_EXISTS;
        }

        // 如果是目录，则只需简单添加
        if (filePathInfo.getType() == FilePathInfo.TYPE_DIR) {

            if (userPath.endsWith("/")) {
                filePathInfo.setUserPath(userPath.replaceAll("/+$", ""));
            }

            if (StringUtils.hasText(filePathInfo.getHash()) || filePathInfo.getSize() != 0) {
                return Result.FILE_TYPE_NOT_SUPPORT;
            }

            filePathInfoMapper.insert(filePathInfo);
            return Result.DIR_SUCCESS;
        }

        if (multipartFile == null || filePathInfo.getSize() != multipartFile.getSize()) {
            return Result.FILE_UPLOAD_FAILED;
        }

        // 保存时文件名为 哈希+文件大小
        Path cachePath = Paths.get("files/cache", filePathInfo.getHash() + filePathInfo.getSize());
        Path storagePath = Paths.get("files/", filePathInfo.getHash() + filePathInfo.getSize());
        if (Files.exists(storagePath)) {

            // TODO:
            // 抽检文件，防止已存储文件泄露
            filePathInfoMapper.insert(filePathInfo);  // 已经存在则只需要记录
            return Result.FILE_SUCCESS;
        }

        // 下面保存文件
        try {
            Files.createDirectories(cachePath.getParent());  //防止目录不存在
            // 服务器获取文件，存入缓存目录
            Files.copy(multipartFile.getInputStream(), cachePath, StandardCopyOption.REPLACE_EXISTING);

            String fileRealHash = FileUtil.nioSHA256(cachePath);

            // 检查文件哈希和大小 存在大小写问题，后面存储时统一转换为由nioSHA256得到的HASH
            if (!fileRealHash.equalsIgnoreCase(filePathInfo.getHash())
                    || Files.size(cachePath) != filePathInfo.getSize()) {
                Files.delete(cachePath);
                return Result.FILE_UPLOAD_FAILED;
            }

            filePathInfo.setHash(fileRealHash);
            filePathInfoMapper.insert(filePathInfo);
            Files.move(cachePath, storagePath);
            return Result.FILE_SUCCESS;
        } catch (Exception e) {
            filePathInfoMapper.deleteById(filePathInfo);  // 若未上传成功，则从表中删去
            return Result.FILE_UPLOAD_FAILED;
        }
    }


    /**
     * 文件列表获取、文件token获取
     *
     * @param token
     * @param username
     * @param path
     * @return
     */
    @Override
    public ServiceResult<Result, Object> filePathInfo(String token, String username, String path) {

        if (!checkAccess(token, username)) {
            return new ServiceResult<>(Result.INVALID_ACCESS, null);
        }

        String queryPath = username + path;

        if (queryPath.endsWith("/")) {
            queryPath = queryPath.substring(0, queryPath.length() - 1);
        }

        QueryWrapper<FilePathInfo> qw1 = new QueryWrapper<>();
        qw1.eq("user_path", queryPath);
        FilePathInfo filePathInfo = filePathInfoMapper.selectOne(qw1);
        if (filePathInfo == null) {
            return new ServiceResult<>(Result.FILE_NOT_FOUND, null);
        }

        switch (filePathInfo.getType()) {
            case FilePathInfo.TYPE_DIR:
                queryPath += "/";
                QueryWrapper<FilePathInfo> qw2 = new QueryWrapper<>();
                qw2.likeRight("user_path", queryPath).notLike("user_path", queryPath + "%/%");
                List<FilePathInfo> filePathInfos = filePathInfoMapper.selectList(qw2);
                return new ServiceResult<>(Result.DIR_SUCCESS, filePathInfos);
            case FilePathInfo.TYPE_FILE:

                String fileToken = UUID.randomUUID().toString();
                redisUtil.hSet("download:" + fileToken, "hash", filePathInfo.getHash());
                redisUtil.hSet("download:" + fileToken, "size", String.valueOf(filePathInfo.getSize()));
                redisUtil.hSet("download:" + fileToken, "filename", StringUtils.getFilename(filePathInfo.getUserPath()));
                redisUtil.expire("download:" + fileToken, 3, TimeUnit.MINUTES);

                return new ServiceResult<>(Result.FILE_SUCCESS, fileToken);
            default:
                return new ServiceResult<>(Result.INVALID_ACCESS, null);
        }


    }

    /**
     * 通过文件token和range头来分段下载文件
     *
     * @param token 文件token
     * @param range 请求range头
     * @return 文件资源或null
     */
    @Override
    public ResponseEntity<Resource> download(String token, String range) {
        String key = "download:" + token;
        if (!redisUtil.exists(key)) {
            return null;
        }
        String fileName = redisUtil.hGet(key, "filename");
        String hash = redisUtil.hGet(key, "hash");
        long size = Long.parseLong(redisUtil.hGet(key, "size"));

        try {
            var r = fileDownloadService.download(hash, size, fileName, range);
            redisUtil.expire("download:" + key, 3, TimeUnit.MINUTES);  //给文件续期三分钟
            return r;
        } catch (MalformedURLException e) {
            return null;
        }

    }


    @Override
    public Result delete(String token, String username, String path) {
        if (!checkAccess(token, username)) {
            return Result.INVALID_ACCESS;
        }

        String queryPath = username + path;

        QueryWrapper<FilePathInfo> qw = new QueryWrapper<>();
        qw.eq("user_path", queryPath);
        FilePathInfo filePathInfo = filePathInfoMapper.selectOne(qw);
        if (filePathInfo == null) {
            return Result.FILE_NOT_FOUND;
        } else {
            filePathInfoMapper.delete(qw);
            return Result.FILE_SUCCESS;
        }


    }


}
