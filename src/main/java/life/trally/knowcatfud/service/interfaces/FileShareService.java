package life.trally.knowcatfud.service.interfaces;

import life.trally.knowcatfud.pojo.ShareInfo;
import life.trally.knowcatfud.service.ServiceResult;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface FileShareService {

    enum Result {
        SUCCESS,
        FILED,
        SHARE_NOT_FOUND,
        ALREADY_SHARED,
        INVALID_ACCESS,
        ALREADY_LIKE
    }

    ServiceResult<Result, String> share(String token, String username, String path, ShareInfo shareInfo);

    ResponseEntity<Resource> download(String shareUUID, String password, String rangeHeader);

    Result like(String shareUUID);

    ServiceResult<Result, Object> getLikeRanking();
}
