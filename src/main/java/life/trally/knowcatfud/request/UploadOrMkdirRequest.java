package life.trally.knowcatfud.request;

import lombok.Data;

@Data
public class UploadOrMkdirRequest {
    private Integer type;
    private String hash;
    private Integer size;
}
