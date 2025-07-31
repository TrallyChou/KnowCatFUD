package life.trally.knowcatfud.controller;

import life.trally.knowcatfud.service.ServiceResult;
import life.trally.knowcatfud.service.interfaces.FileShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class FilesShareController {

    @Autowired
    private FileShareService fileShareService;

    @PostMapping("/share/{username}/{*path}")
    public R share(
            @RequestHeader("Authorization") String token,
            @PathVariable String username,
            @PathVariable String path) {

        ServiceResult<FileShareService.Result, String> r = fileShareService.share(token, username, path);
        return switch (r.getResult()) {
            case SHARE_SUCCESS -> R.ok().message("分享成功").data("uuid", r.getData());
            case SHARE_FAILED -> R.error().message("分享失败");
            case SHARE_NOT_FOUND -> R.error().message("文件未找到");
            case ALREADY_SHARED -> R.ok().message("已经分享过了").data("uuid", r.getData());
            case INVALID_ACCESS -> R.error().message("非法访问");
        };
    }

    @GetMapping(value = "/share/{shareUUID}",produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> download(@PathVariable String shareUUID) {
        // TODO:
        // 1. 放行未登录用户，允许其下载

        return fileShareService.download(shareUUID);
    }


}
