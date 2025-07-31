package life.trally.knowcatfud.controller;

import life.trally.knowcatfud.pojo.ShareInfo;
import life.trally.knowcatfud.service.ServiceResult;
import life.trally.knowcatfud.service.interfaces.FileShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

@RestController
public class FilesShareController {

    @Autowired
    private FileShareService fileShareService;

    // 分享
    @PostMapping("/share/{username}/{*path}")
    public R share(
            @RequestHeader("Authorization") String token,
            @PathVariable String username,
            @PathVariable String path,
            @RequestBody @NonNull ShareInfo shareInfo) {

        ServiceResult<FileShareService.Result, String> r = fileShareService.share(token, username, path, shareInfo);
        return switch (r.getResult()) {
            case SUCCESS -> R.ok().message("分享成功").data("uuid", r.getData());
            case SHARE_NOT_FOUND -> R.error().message("文件未找到");
            case ALREADY_SHARED -> R.ok().message("已经分享过了").data("uuid", r.getData());
            case INVALID_ACCESS -> R.error().message("非法访问");
            default -> R.error().message("分享失败");
        };
    }

    // 下载
    @GetMapping(value = "/share/{shareUUID}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> download(
            @PathVariable String shareUUID,
            @RequestParam @Nullable String password) {

        // TODO:
        // 1. 放行未登录用户，允许其下载

        return fileShareService.download(shareUUID, password);
    }

    // 点赞
    @PatchMapping("/share/{shareUUID}/like")
    public R like(@PathVariable String shareUUID) {
        return switch (fileShareService.like(shareUUID)) {
            case SUCCESS -> R.ok().message("点赞成功");
            case ALREADY_LIKE -> R.ok().message("已经点过赞");
            default -> R.error().message("点赞失败");
        };
    }

    // 获取点赞排行榜
    @GetMapping("/share")
    public R getLikeRanking() {
        ServiceResult<FileShareService.Result, Object> r = fileShareService.getLikeRanking();
        return switch (r.getResult()) {
            case SUCCESS -> R.ok().message("获取成功").data("ranking", r.getData());
            default -> R.error().message("获取失败");
        };
    }


}
