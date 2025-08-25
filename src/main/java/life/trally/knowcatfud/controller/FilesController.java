package life.trally.knowcatfud.controller;

import life.trally.knowcatfud.jwt.LoginUser;
import life.trally.knowcatfud.entity.UserFile;
import life.trally.knowcatfud.service.interfaces.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FilesController {

    private final FileService fileService;

    public FilesController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/files/{*path}")
    @PreAuthorize("hasAnyAuthority('files:upload_or_mkdir')")
    public R uploadOrMkdir(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String path,
            @RequestPart("file") @Nullable MultipartFile multipartFile,
            @RequestPart("info") @NonNull UserFile userFile   // 在客户端一定要指明Context-Type为application/json
    ) {
        return switch (fileService.uploadOrMkdir(loginUser.getId(), path, multipartFile, userFile)) {
            case FILE_SUCCESS -> R.ok().message("文件上传成功");
            case FILE_ALREADY_EXISTS -> R.error().message("文件已存在");
            case FILE_UPLOAD_FAILED -> R.error().message("文件上传失败");
            case INVALID_ACCESS -> R.error().message("非法访问");
            case DIR_SUCCESS -> R.ok().message("目录创建成功");
            default -> R.error().message("未知错误");
        };
    }

    // 文件列表获取 或 文件下载
    @GetMapping("/files/{*path}")
    @PreAuthorize("hasAnyAuthority('files:list')")
    public R listOrDownload(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String path) {
        var r = fileService.listOrDownload(loginUser.getId(), path);

        return switch (r.getResult()) {
            case DIR_SUCCESS -> R.ok().data("files_list", r.getData());
            case FILE_SUCCESS -> R.ok().data("file_token", r.getData());
            case FILE_NOT_FOUND -> R.error().message("文件或目录不存在");
            case INVALID_ACCESS -> R.error().message("非法访问");
            default -> R.error().message("未知错误");
        };
    }

    // 文件删除
    @DeleteMapping("/files/{*path}")
    @PreAuthorize("hasAnyAuthority('files:delete')")
    public R delete(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String path
    ) {

        return switch (fileService.delete(loginUser.getId(), path)) {
            case FILE_SUCCESS -> R.ok().message("文件删除成功");
            case DIR_SUCCESS -> R.ok().message("目录删除成功");
            case FILE_NOT_FOUND -> R.ok().message("文件不存在");
            case DELETE_FAILED -> R.error().message("删除失败");
            case INVALID_ACCESS -> R.error().message("非法访问");
            default -> null;
        };
    }

    @GetMapping("/download/{token}")
    public ResponseEntity<Resource> download(
            @PathVariable String token,
            @RequestHeader("Range") @Nullable String range) {
        return fileService.download(token, range);
    }


}
