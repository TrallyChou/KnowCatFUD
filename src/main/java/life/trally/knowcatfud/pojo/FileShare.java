package life.trally.knowcatfud.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("file_share")
public class FileShare {
    public static final int PRIVATE = 0;  // 私人分享
    public static final int PUBLIC_RANKING = 1;  // 公开分享，参与排行
    public static final int PUBLIC_UNRANKING = 2;  // 非公开分享，不参与排行

    @TableId(type = IdType.AUTO)
    private Long id;
    private String uuid;
    private Long fileId;
    private Integer type = PUBLIC_RANKING;  // 分享类型
    private String password;
    private String introduction;
    private Integer likes = 0;   // 缓存点赞数
}
