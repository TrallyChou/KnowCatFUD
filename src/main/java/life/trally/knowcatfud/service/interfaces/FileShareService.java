package life.trally.knowcatfud.service.interfaces;

import life.trally.knowcatfud.service.ServiceResult;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface FileShareService {

    enum Result {
        SHARE_SUCCESS,
        SHARE_FAILED,
        SHARE_NOT_FOUND,
        ALREADY_SHARED,
        INVALID_ACCESS
    }

    ServiceResult<Result, String> share(String token, String username, String path);

    ResponseEntity<Resource> download(String userPath);

}
