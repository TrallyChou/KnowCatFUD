package life.trally.knowcatfud.pojo.request;

import lombok.Data;

@Data
public class UploadOrMkdirRequest {
    private Integer type;
    private String hash;
    private Integer size;
}
