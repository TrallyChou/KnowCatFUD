package life.trally.knowcatfud.a1sc.service.interfaces;

import life.trally.knowcatfud.a1sc.service.ServiceResult;
import life.trally.knowcatfud.pojo.entity.UserFile;
import life.trally.knowcatfud.pojo.response.ListOrDownloadResponse;
import life.trally.knowcatfud.pojo.response.UploadOrMkdirResponse;
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
        INVALID_ACCESS,
        FAST_UPLOAD_SUCCESS,
        NEED_CHECK
    }

    ServiceResult<Result, UploadOrMkdirResponse> uploadOrMkdir(Long userId, String path, UserFile userFile);

    Result upload(String token, MultipartFile multipartFile);

    ServiceResult<Result, ListOrDownloadResponse> listOrDownload(Long userId, String path);

    ResponseEntity<Resource> download(String token, String range);

    Result delete(Long userId, String path);

}
