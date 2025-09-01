package life.trally.knowcatfud.request;

import lombok.Data;

@Data
//@NoArgsConstructor   // Java会自动生成无参构造器
public class ShareRequest {
    private Integer type = 0;  // 分享类型
    private String password;
    private Integer expire;
    private String title;
    private String introduction;
}
