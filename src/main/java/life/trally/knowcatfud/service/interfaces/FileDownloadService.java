package life.trally.knowcatfud.service.interfaces;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.net.MalformedURLException;

public interface FileDownloadService {
    
    /**
     * 文件分段下载
     *
     * @param fileHash    文件HASH
     * @param fileSize    文件大小
     * @param fileName    用户存储的文件名
     * @param rangeHeader 分片范围
     * @return 资源响应体
     * @throws MalformedURLException 文件资源URL异常
     */
    ResponseEntity<Resource> download(String fileHash, long fileSize, String fileName, String rangeHeader) throws MalformedURLException;
}
