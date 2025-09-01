package life.trally.knowcatfud.controller;

import io.swagger.v3.oas.annotations.Operation;
import life.trally.knowcatfud.jwt.LoginUser;
import life.trally.knowcatfud.mapping.RequestMapping;
import life.trally.knowcatfud.request.UploadOrMkdirRequest;
import life.trally.knowcatfud.response.ListOrDownloadResponse;
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
    private final RequestMapping requestMapping;

    public FilesController(FileService fileService, RequestMapping requestMapping) {
        this.fileService = fileService;
        this.requestMapping = requestMapping;
    }

    @Operation(summary = "上传文件或创建目录", description = "当path.type为1时创建目录，path.type为0时上传文件")
    @PostMapping("/files/{*path}")
    @PreAuthorize("hasAnyAuthority('files:upload_or_mkdir')")
    public R<Void> uploadOrMkdir(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String path,
            @RequestPart("file") @Nullable MultipartFile multipartFile,
            @RequestPart("info") @NonNull UploadOrMkdirRequest request   // 在客户端一定要指明Context-Type为application/json
    ) {
        return switch (fileService.uploadOrMkdir(loginUser.getId(), path, multipartFile, requestMapping.toUserFile(request))) {
            case FILE_SUCCESS -> R.ok("文件上传成功");
            case FILE_ALREADY_EXISTS -> R.error("文件已存在");
            case FILE_UPLOAD_FAILED -> R.error("文件上传失败");
            case INVALID_ACCESS -> R.error("非法访问");
            case DIR_SUCCESS -> R.ok("目录创建成功");
            default -> R.error("未知错误");
        };
    }

    // 文件列表获取 或 文件下载
    @GetMapping("/files/{*path}")
    @PreAuthorize("hasAnyAuthority('files:list')")
    public R<ListOrDownloadResponse> listOrDownload(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String path) {

        var r = fileService.listOrDownload(loginUser.getId(), path);

        return switch (r.getResult()) {
            case DIR_SUCCESS, FILE_SUCCESS -> R.ok(r.getData());
            case FILE_NOT_FOUND -> R.error("文件或目录不存在");
            case INVALID_ACCESS -> R.error("非法访问");
            default -> R.error("未知错误");
        };
    }

    // 文件删除
    @DeleteMapping("/files/{*path}")
    @PreAuthorize("hasAnyAuthority('files:delete')")
    public R<Void> delete(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String path
    ) {

        return switch (fileService.delete(loginUser.getId(), path)) {
            case FILE_SUCCESS -> R.ok("文件删除成功");
            case DIR_SUCCESS -> R.ok("目录删除成功");
            case FILE_NOT_FOUND -> R.ok("文件不存在");
            case DELETE_FAILED -> R.error("删除失败");
            case INVALID_ACCESS -> R.error("非法访问");
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
