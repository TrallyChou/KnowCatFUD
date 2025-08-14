package life.trally.knowcatfud.pojo;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("share_info")
public class ShareInfo {

    public static final int PRIVATE = 0;  // 私人分享
    public static final int PUBLIC_RANKING = 1;  // 公开分享，参与排行
    public static final int PUBLIC_UNRANKING = 2;  // 非公开分享，不参与排行

    @TableId(value = "uuid", type = IdType.INPUT)
    String uuid = "";
    String file = "";
    Integer type = PUBLIC_RANKING;  // 分享类型
    String password = null;
    String introduction = "";

    @Setter(AccessLevel.NONE)  // 不生成setter
    Integer likes = 0;

}
