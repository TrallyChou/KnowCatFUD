package life.trally.knowcatfud.pojo.response;

import lombok.Data;

@Data
public class GetSharesResponse {
    private String uuid;
    private Integer type;  // 分享类型
    private String password;
    private Integer expire;
    private String title;
    private String introduction;
    private String createdAt;    // 分享创建时间
    private String path;

}
