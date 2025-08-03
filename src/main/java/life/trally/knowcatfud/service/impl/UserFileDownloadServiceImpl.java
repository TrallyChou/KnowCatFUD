package life.trally.knowcatfud.service.impl;

import life.trally.knowcatfud.pojo.FilePathInfo;
import life.trally.knowcatfud.service.interfaces.UserFileDownloadService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * 用来提供分片下载等功能的类
 */

@Service
public class UserFileDownloadServiceImpl implements UserFileDownloadService {

    @Override
    public ResponseEntity<Resource> download(FilePathInfo filePathInfo) throws MalformedURLException {

        String fileName = filePathInfo.getHash() + filePathInfo.getSize();
        Path filePath = Paths.get("files/", fileName);
        Resource resource = new UrlResource(filePath.toUri());
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                + StringUtils.getFilename(filePathInfo.getUserPath()) + "\"");
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);

    }
}
