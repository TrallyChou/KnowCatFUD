package life.trally.knowcatfud.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareInfo {
    boolean sharePublic = false;  // 公开分享，即参与排行榜
    String password = null;       // 密码 可选
}
