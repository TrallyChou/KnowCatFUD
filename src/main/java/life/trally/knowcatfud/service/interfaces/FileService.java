package life.trally.knowcatfud.service.interfaces;

import life.trally.knowcatfud.pojo.FilePathInfo;
import life.trally.knowcatfud.service.ServiceResult;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    Result uploadOrMkdir(String token, String username, MultipartFile multipartFile, FilePathInfo filePathInfo);

    ServiceResult<Result, List<FilePathInfo>> getList(String token, String username, String filePathInfo);

    ResponseEntity<Resource> download(String token, String username, String path);

    Result delete(String token, String username, String path);

}
