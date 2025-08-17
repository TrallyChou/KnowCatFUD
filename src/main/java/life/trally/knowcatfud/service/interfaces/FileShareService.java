package life.trally.knowcatfud.service.interfaces;

import life.trally.knowcatfud.pojo.FileShare;
import life.trally.knowcatfud.service.ServiceResult;

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

    ServiceResult<Result, String> share(Long userId, String path, FileShare fileShare);

    ServiceResult<Result, String> download(String shareUUID, String password);

    Result like(Long userId, String shareUUID);

    Result likeStatus(Long userId, String shareUUID);

    ServiceResult<Result, String> likesCount(String shareUUID);

    ServiceResult<Result, Object> getLikeRanking();
}
