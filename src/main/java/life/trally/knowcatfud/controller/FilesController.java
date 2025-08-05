package life.trally.knowcatfud.controller;

import life.trally.knowcatfud.pojo.FilePathInfo;
import life.trally.knowcatfud.service.ServiceResult;
import life.trally.knowcatfud.service.interfaces.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class FilesController {

    @Autowired
    private FileService fileService;

    @PostMapping("/files/{username}")
    @PreAuthorize("hasAnyAuthority('files:upload_or_mkdir')")
    public R uploadOrMkdir(
            @RequestHeader("Authorization") String token,
            @PathVariable String username,
            @RequestPart("file") @Nullable MultipartFile multipartFile,
            @RequestPart("info") @NonNull FilePathInfo filePathInfo   // 在客户端一定要指明Context-Type为application/json
    ) {
        return switch (fileService.uploadOrMkdir(token, username, multipartFile, filePathInfo)) {
            case FILE_SUCCESS -> R.ok().message("文件上传成功");
            case FILE_ALREADY_EXISTS -> R.error().message("文件已存在");
            case FILE_UPLOAD_FAILED -> R.error().message("文件上传失败");
            case INVALID_ACCESS -> R.error().message("非法访问");
            case DIR_SUCCESS -> R.ok().message("目录创建成功");
            default -> R.error().message("未知错误");
        };
    }

    // 文件列表获取
    @GetMapping(path = "/files/{username}/{*path}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('files:list')")
    public R list(
            @RequestHeader("Authorization") String token,
            @PathVariable String username,
            @PathVariable String path) {
        ServiceResult<FileService.Result, List<FilePathInfo>> r = fileService.getList(token, username, path);
        return switch (r.getResult()) {
            case FILE_SUCCESS -> R.ok().data("files_list", r.getData());
            case FILE_NOT_FOUND -> R.error().message("目录不存在");
            case INVALID_ACCESS -> R.error().message("非法访问");
            default -> R.error().message("未知错误");
        };
    }

    // 文件下载
    @GetMapping(value = "/files/{username}/{*path}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAnyAuthority('files:download')")
    public ResponseEntity<Resource> download(
            @RequestHeader("Authorization") String token,
            @PathVariable String username,
            @PathVariable String path,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        return fileService.download(token, username, path, rangeHeader);
    }

    // 文件删除
    @DeleteMapping(value = "/files/{username}/{*path}")
    @PreAuthorize("hasAnyAuthority('files:delete')")
    public R delete(
            @RequestHeader("Authorization") String token,
            @PathVariable String username,
            @PathVariable String path
    ) {

        return switch (fileService.delete(token, username, path)) {
            case FILE_SUCCESS -> R.ok().message("文件删除成功");
            case DIR_SUCCESS -> R.ok().message("目录删除成功");
            case FILE_NOT_FOUND -> R.ok().message("文件不存在");
            case DELETE_FAILED -> R.error().message("删除失败");
            case INVALID_ACCESS -> R.error().message("非法访问");
            default -> null;
        };
    }


//    @GetMapping("/files/{filename}")
//    public R download(@PathVariable String filename) {
//        return R.ok();
//    }


}
