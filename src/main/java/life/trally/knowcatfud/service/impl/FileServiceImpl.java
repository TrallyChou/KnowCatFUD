package life.trally.knowcatfud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import life.trally.knowcatfud.dao.UserFileMapper;
import life.trally.knowcatfud.pojo.UserFile;
import life.trally.knowcatfud.service.ServiceResult;
import life.trally.knowcatfud.service.interfaces.FileDownloadService;
import life.trally.knowcatfud.service.interfaces.FileService;
import life.trally.knowcatfud.utils.FileUtil;
import life.trally.knowcatfud.utils.RedisUtil;
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

    private final UserFileMapper userFileMapper;

    private final FileDownloadService fileDownloadService;
    private final RedisUtil redisUtil;

    public FileServiceImpl(UserFileMapper userFileMapper, FileDownloadService fileDownloadService, RedisUtil redisUtil) {
        this.userFileMapper = userFileMapper;
        this.fileDownloadService = fileDownloadService;
        this.redisUtil = redisUtil;
    }

    /**
     * 文件上传和创建目录
     *
     * @param token
     * @param username
     * @param path
     * @param multipartFile
     * @param userFile
     * @return
     */
    @Override
    public Result uploadOrMkdir(String token, String username, String path, MultipartFile multipartFile, UserFile userFile) {

        if (!checkAccess(token, username)) {   // 非法访问
            return Result.INVALID_ACCESS;
        }

        userFile.setUsername(username);
        userFile.setPath(path);
        userFile.setId(null);
        userFile.setCreatedAt(null);

        if (userFile.getType() == null) {
            return Result.INVALID_ACCESS;
        }

        // 查询是否重名
        LambdaQueryWrapper<UserFile> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFile::getUsername, username).eq(UserFile::getPath, userFile.getPath());
        UserFile oldUserFile = userFileMapper.selectOne(qw);

        if (oldUserFile != null) {
            return Result.FILE_ALREADY_EXISTS;
        }

        // 要求父目录存在
        String parent = Paths.get(userFile.getPath()).getParent()
                .toString().replace("\\", "/");    // 统一win和linux下的路径样式

        // 如果父目录不存在，则返回
        LambdaQueryWrapper<UserFile> qw1 = new LambdaQueryWrapper<>();
        if (!parent.equals("/")) {
            // getParent()对于根目录会返回"/"，但是对于其它目录会不以/结尾，这里进行了统一
            parent = parent + "/";
        }
        qw1.eq(UserFile::getUsername, username).eq(UserFile::getPath, parent);

        UserFile parentPath = userFileMapper.selectOne(qw1);
        if (parentPath == null) {
            return Result.FILE_UPLOAD_FAILED;
        }

        // 目录存在
        userFile.setParent(parent);


        // 如果是目录，则只需简单添加
        if (userFile.getType() == UserFile.TYPE_DIR) {

            // 要求目录以/结尾

            if (!userFile.getPath().endsWith("/")) {
                return Result.FILE_UPLOAD_FAILED;
            }

            userFile.setFilename(null);
            userFile.setHash(null);
            userFile.setSize(null);

            userFileMapper.insert(userFile);

            return Result.DIR_SUCCESS;
        }


        // 如果是文件，要求文件名不以/结尾：
        if (userFile.getPath().endsWith("/")) {
            return Result.FILE_UPLOAD_FAILED;
        }

        // 记录文件名，方便查找
        userFile.setFilename(StringUtils.getFilename(path));


        if (multipartFile == null || userFile.getSize() != multipartFile.getSize()) {
            return Result.FILE_UPLOAD_FAILED;
        }

        // 保存时文件名为 哈希+文件大小
        Path cachePath = Paths.get("files/cache", userFile.getHash() + userFile.getSize());
        Path storagePath = Paths.get("files/", userFile.getHash() + userFile.getSize());
        if (Files.exists(storagePath)) {
            // TODO:
            // 抽检文件，防止已存储文件泄露
            userFileMapper.insert(userFile);  // 已经存在则只需要记录
            return Result.FILE_SUCCESS;
        }

        // 下面保存文件
        try {
            Files.createDirectories(cachePath.getParent());  //防止目录不存在
            // 服务器获取文件，存入缓存目录
            Files.copy(multipartFile.getInputStream(), cachePath, StandardCopyOption.REPLACE_EXISTING);

            String fileRealHash = FileUtil.nioSHA256(cachePath);

            // 检查文件哈希和大小 存在大小写问题，后面存储时统一转换为由nioSHA256得到的HASH
            if (!fileRealHash.equalsIgnoreCase(userFile.getHash())
                    || Files.size(cachePath) != userFile.getSize()) {
                Files.delete(cachePath);
                return Result.FILE_UPLOAD_FAILED;
            }

            userFile.setHash(fileRealHash);
            userFileMapper.insert(userFile);
            Files.move(cachePath, storagePath);
            return Result.FILE_SUCCESS;
        } catch (Exception e) {
            userFileMapper.deleteById(userFile);  // 若未上传成功，则从表中删去
            return Result.FILE_UPLOAD_FAILED;
        }
    }


    /**
     * 文件列表获取、文件下载token获取
     *
     * @param token
     * @param username
     * @param path
     * @return
     */
    @Override
    public ServiceResult<Result, Object> listOrDownload(String token, String username, String path) {

        if (!checkAccess(token, username)) {
            return new ServiceResult<>(Result.INVALID_ACCESS, null);
        }

        LambdaQueryWrapper<UserFile> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFile::getUsername, username).eq(UserFile::getPath, path);
        UserFile userFile = userFileMapper.selectOne(qw);

        if (userFile == null) {
            return new ServiceResult<>(Result.FILE_NOT_FOUND, null);
        }

        switch (userFile.getType()) {
            case UserFile.TYPE_DIR:

                LambdaQueryWrapper<UserFile> qw1 = new LambdaQueryWrapper<>();

                qw1.eq(UserFile::getUsername, username)
                        .eq(UserFile::getParent, path);     // 根据父目录查询列表

                List<UserFile> filesInDir = userFileMapper.selectList(qw1);
                return new ServiceResult<>(Result.DIR_SUCCESS, filesInDir);

            case UserFile.TYPE_FILE:

                String fileToken = UUID.randomUUID().toString();
                redisUtil.hSet("download:" + fileToken, "hash", userFile.getHash());
                redisUtil.hSet("download:" + fileToken, "size", String.valueOf(userFile.getSize()));
                redisUtil.hSet("download:" + fileToken, "filename", StringUtils.getFilename(userFile.getPath()));
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

        if ("/".equals(path)) {
            return Result.DELETE_FAILED;
        }

        LambdaQueryWrapper<UserFile> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFile::getUsername, username).eq(UserFile::getPath, path);
        UserFile userFile = userFileMapper.selectOne(qw);

        if (userFile == null) {
            return Result.FILE_NOT_FOUND;
        } else {
            userFileMapper.delete(qw);
            return Result.FILE_SUCCESS;
        }


    }


}
