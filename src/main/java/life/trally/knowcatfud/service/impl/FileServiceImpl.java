package life.trally.knowcatfud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import life.trally.knowcatfud.dao.FilePathInfoMapper;
import life.trally.knowcatfud.pojo.FilePathInfo;
import life.trally.knowcatfud.service.ServiceResult;
import life.trally.knowcatfud.service.interfaces.FileService;
import life.trally.knowcatfud.service.interfaces.UserFileDownloadService;
import life.trally.knowcatfud.utils.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static life.trally.knowcatfud.utils.AccessCheckUtil.checkAccess;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    FilePathInfoMapper filePathInfoMapper;

    @Autowired
    UserFileDownloadService userFileDownloadService;


    @Override
    public Result uploadOrMkdir(String token, String username, MultipartFile multipartFile, FilePathInfo filePathInfo) {

        if (!checkAccess(token, username)  // 非法访问
                || !filePathInfo.getUserPath().startsWith(username + "/")
        ) {
            return Result.INVALID_ACCESS;
        }

        // 查询文件是否重名
        QueryWrapper<FilePathInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_path", filePathInfo.getUserPath());
        FilePathInfo oldFilePathInfo = filePathInfoMapper.selectOne(queryWrapper);
        if (oldFilePathInfo != null) {  // 文件已存在
            return Result.FILE_ALREADY_EXISTS;
        }

        // 如果是目录，则只需简单添加
        if (filePathInfo.getType() == FilePathInfo.TYPE_DIR) {
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

            // 检查文件哈希
            if (!FileUtil.nioSHA256(cachePath).equals(filePathInfo.getHash())) {
                Files.delete(cachePath);
                return Result.FILE_UPLOAD_FAILED;
            }

            filePathInfoMapper.insert(filePathInfo);
            Files.move(cachePath, storagePath);
            return Result.FILE_SUCCESS;
        } catch (Exception e) {
            filePathInfoMapper.deleteById(filePathInfo);  // 若未上传成功，则从表中删去
            return Result.FILE_UPLOAD_FAILED;
        }
    }

    @Override
    public ServiceResult<Result, List<FilePathInfo>> getList(String token, String username, String path) {

        if (!checkAccess(token, username)) {
            return new ServiceResult<>(Result.INVALID_ACCESS, null);
        }

        String queryPath = username + path;
        if (!queryPath.endsWith("/")) queryPath += "/";
        QueryWrapper<FilePathInfo> qw = new QueryWrapper<>();
        qw.likeRight("user_path", queryPath).notLike("user_path", queryPath + "%/%");
        List<FilePathInfo> filePathInfos = filePathInfoMapper.selectList(qw);
        return new ServiceResult<>(Result.FILE_SUCCESS, filePathInfos);
    }

    @Override
    public ResponseEntity<Resource> download(String token, String username, String path, String rangeHeader) {
        if (!checkAccess(token, username)) {
            return null;
        }

        String queryPath = username + path;  // 这一步确保了只会下载到用户自己的文件

        QueryWrapper<FilePathInfo> qw = new QueryWrapper<>();
        qw.eq("user_path", queryPath);
        FilePathInfo filePathInfo = filePathInfoMapper.selectOne(qw);
        try {
            return userFileDownloadService.download(filePathInfo, rangeHeader);
        } catch (Exception e) {

            // TODO:
            // 更准确的错误处理方法


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
