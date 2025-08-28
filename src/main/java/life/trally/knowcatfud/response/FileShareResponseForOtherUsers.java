package life.trally.knowcatfud.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileShareResponseForOtherUsers {
    private Integer type;  // 分享类型
    private Integer expire;
    private String title;
    private String introduction;
    private String createdAt;    // 分享创建时间
}
