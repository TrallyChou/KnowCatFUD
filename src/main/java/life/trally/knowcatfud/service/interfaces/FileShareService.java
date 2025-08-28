package life.trally.knowcatfud.service.interfaces;

import life.trally.knowcatfud.request.FileShareRequest;
import life.trally.knowcatfud.response.FileShareResponseForCreator;
import life.trally.knowcatfud.response.FileShareResponseForOtherUsers;
import life.trally.knowcatfud.response.FileShareSearchResponse;
import life.trally.knowcatfud.service.ServiceResult;

import java.util.List;

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

    ServiceResult<Result, String> share(Long userId, String path, FileShareRequest fileShare);

    ServiceResult<Result, FileShareResponseForOtherUsers> getShare(String shareUUID, String password);

    ServiceResult<Result, String> download(String shareUUID, String password);

    Result like(Long userId, String shareUUID);

    Result likeStatus(Long userId, String shareUUID);

    ServiceResult<Result, String> likesCount(String shareUUID);

    ServiceResult<Result, Object> getLikeRanking();

    ServiceResult<Result, List<FileShareSearchResponse>> search(String keywords);

    ServiceResult<Result, List<FileShareResponseForCreator>> getShares(Long UserId);

    Result delete(Long userId, String shareUuid);
}
