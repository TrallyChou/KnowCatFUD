package life.trally.knowcatfud.controller;

import life.trally.knowcatfud.jwt.LoginUser;
import life.trally.knowcatfud.pojo.FileShare;
import life.trally.knowcatfud.service.ServiceResult;
import life.trally.knowcatfud.service.interfaces.FileShareService;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class FilesShareController {

    private final FileShareService fileShareService;

    public FilesShareController(FileShareService fileShareService) {
        this.fileShareService = fileShareService;
    }

    // 分享
    @PostMapping("/share/{*path}")
    @PreAuthorize("hasAnyAuthority('files_share:share')")
    public R share(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String path,
            @RequestBody @NonNull FileShare fileShare) {

        ServiceResult<FileShareService.Result, String> r = fileShareService.share(loginUser.getId(), path, fileShare);
        return switch (r.getResult()) {
            case SUCCESS -> R.ok().message("分享成功").data("uuid", r.getData());
            case SHARE_NOT_FOUND -> R.error().message("文件未找到");
            case ALREADY_SHARED -> R.ok().message("已经分享过了").data("uuid", r.getData());
            case INVALID_ACCESS -> R.error().message("非法访问");
            default -> R.error().message("分享失败");
        };
    }

    // 下载
    @GetMapping(value = "/share/{shareUUID}")
    //@PreAuthorize("hasAnyAuthority('files_share:download')")
    public R download(
            @PathVariable String shareUUID,
            @RequestParam @Nullable String password) {

        var r = fileShareService.download(shareUUID, password);
        return switch (r.getResult()) {
            case SUCCESS -> R.ok().data("file_token", r.getData());
            case FAILED -> R.error().message("获取文件token失败");
            default -> R.error().message("未知错误");
        };

    }

    // 点赞
    @PatchMapping("/share/{shareUUID}/like")
    @PreAuthorize("hasAnyAuthority('files_share:like')")
    public R like(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String shareUUID) {
        return switch (fileShareService.like(loginUser.getId(), shareUUID)) {
            case SUCCESS -> R.ok().message("点赞成功");
            case ALREADY_LIKE -> R.ok().message("已经点过赞");
            default -> R.error().message("点赞失败");
        };
    }

    // 获取点赞状态
    @GetMapping("/share/{shareUUID}/like")
    //@PreAuthorize("hasAnyAuthority('files_share:like_count')")
    public R likeStatus(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String shareUUID) {
        return switch (fileShareService.like(loginUser.getId(), shareUUID)) {
            case SUCCESS -> R.ok().message("点赞成功");
            case ALREADY_LIKE -> R.ok().message("已经点过赞");
            default -> R.error().message("点赞失败");
        };
    }

    // 获取点赞数
    @GetMapping("/share/{shareUUID}/likes")
    //@PreAuthorize("hasAnyAuthority('files_share:like_count')")
    public R likeCount(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String shareUUID) {
        return switch (fileShareService.like(loginUser.getId(), shareUUID)) {
            case SUCCESS -> R.ok().message("点赞成功");
            case ALREADY_LIKE -> R.ok().message("已经点过赞");
            default -> R.error().message("点赞失败");
        };
    }

    // 获取点赞排行榜
    @GetMapping("/share")
    @PreAuthorize("hasAnyAuthority('files_share:get_like_ranking')")
    public R getLikeRanking() {
        ServiceResult<FileShareService.Result, Object> r = fileShareService.getLikeRanking();
        return switch (r.getResult()) {
            case SUCCESS -> R.ok().message("获取成功").data("ranking", r.getData());
            default -> R.error().message("获取失败");
        };
    }


}
