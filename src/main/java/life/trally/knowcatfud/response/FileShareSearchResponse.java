package life.trally.knowcatfud.response;

import lombok.Data;

@Data
public class FileShareSearchResponse {
    private String uuid;  // 只存储在ES中
    private String title;
    private String introduction;
}
