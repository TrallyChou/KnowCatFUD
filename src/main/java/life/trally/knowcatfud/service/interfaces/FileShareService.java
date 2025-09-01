package life.trally.knowcatfud.service.interfaces;

import life.trally.knowcatfud.request.ShareRequest;
import life.trally.knowcatfud.response.GetShareResponse;
import life.trally.knowcatfud.response.FileShareSearchResponse;
import life.trally.knowcatfud.response.GetSharesResponse;
import life.trally.knowcatfud.service.ServiceResult;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Set;

public interface FileShareService {

    enum Result {
        SUCCESS,
        FAILED,
        SHARE_NOT_FOUND,
        ALREADY_SHARED,
        INVALID_ACCESS,
        ALREADY_LIKE,
        NOT_LIKE
    }

    ServiceResult<Result, String> share(Long userId, String path, ShareRequest shareRequest);

    ServiceResult<Result, GetShareResponse> getShare(String shareUUID, String password);

    ServiceResult<Result, String> download(String shareUUID, String password);

    Result like(Long userId, String shareUUID);

    Result likeStatus(Long userId, String shareUUID);

    ServiceResult<Result, Integer> likesCount(String shareUUID);

    ServiceResult<Result, Set<ZSetOperations.TypedTuple<String>>> getLikeRankingByPage(int page);

    ServiceResult<Result, List<FileShareSearchResponse>> search(String keywords, int page);

    ServiceResult<Result, List<GetSharesResponse>> getShares(Long UserId);

    Result delete(Long userId, String shareUuid);
}
