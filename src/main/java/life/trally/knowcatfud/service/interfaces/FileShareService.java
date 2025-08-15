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
        ALREADY_LIKE
    }

    ServiceResult<Result, String> share(String token, String username, String path, FileShare fileShare);

    ServiceResult<Result, String> download(String shareUUID, String password);

    Result like(String shareUUID);

    ServiceResult<Result, Object> getLikeRanking();
}
