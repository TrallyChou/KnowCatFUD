package life.trally.knowcatfud.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetShareResponse {
    private Integer type;  // 分享类型
    private Integer expire;
    private String title;
    private String introduction;
    private String createdAt;    // 分享创建时间
    private Boolean violation;
    private String cause;

    public GetShareResponse(Integer type, Integer expire, String title, String introduction, String createdAt) {
        this.type = type;
        this.expire = expire;
        this.title = title;
        this.introduction = introduction;
        this.createdAt = createdAt;
    }

    public GetShareResponse(Boolean violation, String cause) {
        this.violation = violation;
        this.cause = cause;
    }



}
