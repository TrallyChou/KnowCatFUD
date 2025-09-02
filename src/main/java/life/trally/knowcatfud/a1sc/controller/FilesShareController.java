package life.trally.knowcatfud.a1sc.controller;

import io.swagger.v3.oas.annotations.Operation;
import life.trally.knowcatfud.pojo.jwt.LoginUser;
import life.trally.knowcatfud.pojo.request.ShareRequest;
import life.trally.knowcatfud.pojo.response.*;
import life.trally.knowcatfud.a1sc.service.ServiceResult;
import life.trally.knowcatfud.a1sc.service.interfaces.FileShareService;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
public class FilesShareController {

    private final FileShareService fileShareService;

    public FilesShareController(FileShareService fileShareService) {
        this.fileShareService = fileShareService;
    }

    // 分享
    @PostMapping("/share/{*path}")
    @PreAuthorize("hasAnyAuthority('files_share:share')")
    @Operation(summary = "分享文件")
    public R<ShareResponse> share(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String path,
            @RequestBody @NonNull ShareRequest shareRequest) {

        ServiceResult<FileShareService.Result, String> r = fileShareService.share(loginUser.getId(), path, shareRequest);
        return switch (r.getResult()) {
            case SUCCESS -> R.ok(new ShareResponse(r.getData())).message("分享成功");
            case SHARE_NOT_FOUND -> R.error("文件未找到");
            case ALREADY_SHARED -> R.ok(new ShareResponse(r.getData())).message("已经分享过了");
            case INVALID_ACCESS -> R.error("非法访问");
            default -> R.error("分享失败");
        };
    }

    // 获取
    @GetMapping(value = "/share/{shareUUID}")
    @Operation(summary = "获取分享信息")
    //@PreAuthorize("hasAnyAuthority('files_share:download')")
    public R<GetShareResponse> getShare(
            @PathVariable String shareUUID,
            @RequestParam @Nullable String password) {

        var r = fileShareService.getShare(shareUUID, password);
        return switch (r.getResult()) {
            case SUCCESS -> R.ok(r.getData());
            case SHARE_NOT_FOUND -> R.error("分享不存在");
            default -> R.error("未知错误");
        };

    }


    // 下载
    @GetMapping(value = "/share/{shareUUID}/download")
    //@PreAuthorize("hasAnyAuthority('files_share:download')")
    @Operation(summary = "获取分享文件下载token")
    public R<ShareDownloadResponse> shareDownload(
            @PathVariable String shareUUID,
            @RequestParam @Nullable String password) {

        var r = fileShareService.download(shareUUID, password);
        return switch (r.getResult()) {
            case SUCCESS -> R.ok(new ShareDownloadResponse(r.getData()));
            case FAILED -> R.error("获取文件token失败");
            default -> R.error("未知错误");
        };

    }

    // 点赞
    @PatchMapping("/share/{shareUUID}/like")
    @PreAuthorize("hasAnyAuthority('files_share:like')")
    @Operation(summary = "点赞")
    public R<Void> like(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String shareUUID) {
        return switch (fileShareService.like(loginUser.getId(), shareUUID)) {
            case SUCCESS -> R.ok("点赞成功");
            case ALREADY_LIKE -> R.ok("已经点过赞");
            default -> R.error("点赞失败");
        };
    }

    // 获取点赞状态
    @GetMapping("/share/{shareUUID}/like")
    @PreAuthorize("hasAnyAuthority('files_share:like')")
    @Operation(summary = "获取点赞状态")
    public R<Void> likeStatus(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String shareUUID) {
        return switch (fileShareService.likeStatus(loginUser.getId(), shareUUID)) {
            case ALREADY_LIKE -> R.ok("已经点过赞");
            case NOT_LIKE -> R.ok("未点赞");
            default -> R.error("获取失败");
        };
    }

    // 获取点赞数
    @GetMapping("/share/{shareUUID}/likes")
    @Operation(summary = "获取点赞数")
    // 该功能无需登录，无需权限   @PreAuthorize("hasAnyAuthority('files_share:like')")
    public R<LikesCountResponse> likesCount(
            @PathVariable String shareUUID) {
        var r = fileShareService.likesCount(shareUUID);
        return switch (r.getResult()) {
            case SUCCESS -> R.ok(new LikesCountResponse(r.getData())).message("获取成功");
            default -> R.error("获取点赞数失败");
        };
    }

    // 获取点赞排行榜
    // PS:SpringBoot优先匹配具体路径
    @GetMapping("/share/ranking")
    @PreAuthorize("hasAnyAuthority('files_share:get_like_ranking')")
    @Operation(summary = "获取点赞排行榜")
    public R<GetLikesRankingResponse> getLikesRanking(
            @RequestParam int page
    ) {
        if (page <= 0) {
            return R.error("参数错误");
        }
        ServiceResult<FileShareService.Result, Set<ZSetOperations.TypedTuple<String>>> r = fileShareService.getLikeRankingByPage(page);
        return switch (r.getResult()) {
            case SUCCESS -> R.ok(new GetLikesRankingResponse(r.getData())).message("获取成功");
            default -> R.error("获取失败");
        };
    }

    // 搜索
    @GetMapping("/share/search")
    @Operation(summary = "根据分享介绍搜索分享")
//    @PreAuthorize("")
    public R<List<FileShareSearchResponse>> shareSearch(
            @RequestParam String keywords,
            @RequestParam int page
    ) {

        var r = fileShareService.search(keywords, page);
        return switch (r.getResult()) {
            case SUCCESS -> R.ok(r.getData());
            default -> R.error("搜索失败");
        };
    }

    @GetMapping("/share")
    @Operation(summary = "获取自己创建的分享")
    public R<List<GetSharesResponse>> getShares(@AuthenticationPrincipal LoginUser loginUser) {
        var r = fileShareService.getShares(loginUser.getId());
        return switch (r.getResult()) {
            case SUCCESS -> R.ok(r.getData());
            default -> R.error("失败");
        };
    }

    @DeleteMapping("/share/{shareUUID}")
    @Operation(summary = "删除 分享")
    public R<Void> delete(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable String shareUUID) {
        return switch (fileShareService.delete(loginUser.getId(), shareUUID)) {
            case SUCCESS -> R.ok("删除成功");
            case SHARE_NOT_FOUND -> R.ok("分享不存在");
            case INVALID_ACCESS -> R.error("非法访问");
            default -> R.error("删除失败");
        };
    }


}
