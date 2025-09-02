package life.trally.knowcatfud.pojo.response;

import lombok.Data;

@Data
public class FileShareSearchResponse {
    private String uuid;  // 只存储在ES中
    private String title;
    private String introduction;
}
