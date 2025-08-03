package life.trally.knowcatfud.service.impl;

import life.trally.knowcatfud.pojo.FilePathInfo;
import life.trally.knowcatfud.service.interfaces.UserFileDownloadService;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * 用来提供分片下载等功能的类
 */

@Service
public class UserFileDownloadServiceImpl implements UserFileDownloadService {

    @Override
    public ResponseEntity<Resource> download(FilePathInfo filePathInfo, String rangeHeader) throws MalformedURLException {

        String fileName = filePathInfo.getHash() + filePathInfo.getSize();
        Path filePath = Paths.get("files/", fileName);
        Resource resource = new UrlResource(filePath.toUri());
        long fileLength = filePathInfo.getSize();

        if (!StringUtils.hasText(rangeHeader)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + StringUtils.getFilename(filePathInfo.getUserPath()) + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(filePathInfo.getSize())
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        }

        String[] ranges = rangeHeader.substring("bytes=".length()).split("-");
        long start = Long.parseLong(ranges[0]);
        long end = ranges.length > 1 ? Long.parseLong(ranges[1]) : fileLength - 1;
        long contentLength = end - start + 1;

        if (start < 0 || end >= fileLength || start > end) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
                    .build();
        }

        RangeResource rangeResource = new RangeResource(resource, start, contentLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(contentLength)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(rangeResource);
    }

    static class RangeResource extends AbstractResource {
        private final Resource delegate;
        private final long start;
        private final long length;

        RangeResource(Resource delegate, long start, long length) {
            this.delegate = delegate;
            this.start = start;
            this.length = length;
        }

        @Override
        public String getDescription() {
            return delegate.getDescription() + " [from " + start + " to " + (start + length - 1) + "]";
        }

        @Override
        public InputStream getInputStream() throws IOException {
            File file = delegate.getFile();
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(start);

            return new InputStream() {
                private long pos = 0;
                private final RandomAccessFile rafInternal = raf;

                @Override
                public int read() throws IOException {
                    if (pos >= length) return -1;
                    pos++;
                    return rafInternal.read();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    int bytesToRead = (int) Math.min(len, length - pos);
                    if (bytesToRead <= 0) return -1;
                    int read = rafInternal.read(b, off, bytesToRead);
                    if (read > 0) pos += read;
                    return read;
                }

                @Override
                public void close() throws IOException {
                    rafInternal.close();
                }
            };
        }

        @Override
        public long contentLength() {
            return length;
        }

        @Override
        public File getFile() throws IOException {
            return delegate.getFile();
        }
    }
}
