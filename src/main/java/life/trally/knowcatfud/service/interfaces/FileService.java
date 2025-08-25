package life.trally.knowcatfud.service.interfaces;

import life.trally.knowcatfud.entity.UserFile;
import life.trally.knowcatfud.service.ServiceResult;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {


    enum Result {
        FILE_SUCCESS,
        DIR_SUCCESS,
        FILE_ALREADY_EXISTS,
        FILE_NOT_FOUND,
        FILE_TYPE_NOT_SUPPORT,
        FILE_UPLOAD_FAILED,
        FILE_DOWNLOAD_FAILED,
        DELETE_FAILED,
        INVALID_ACCESS
    }

    Result uploadOrMkdir(Long userId, String path, MultipartFile multipartFile, UserFile userFile);

    ServiceResult<Result, Object> listOrDownload(Long userId, String path);

    ResponseEntity<Resource> download(String token, String range);

    Result delete(Long userId, String path);

}
