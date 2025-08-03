package life.trally.knowcatfud.service.interfaces;

import life.trally.knowcatfud.pojo.FilePathInfo;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.net.MalformedURLException;

public interface UserFileDownloadService {
    ResponseEntity<Resource> download(FilePathInfo filePathInfo) throws MalformedURLException;

}
