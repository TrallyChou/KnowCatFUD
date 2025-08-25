package life.trally.knowcatfud.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;


@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("file_share")
public class FileShare {
    public static final int PRIVATE = 0;  // 私人分享 可设密码 不参与排行 不支持搜索
    public static final int PUBLIC_RANKING = 1;  // 公开分享 无需密码 参与排行 支持搜索
    public static final int PUBLIC_UNRANKING = 2;  // 公开分享 无需密码 不参与排行 支持搜索
    public static final int PUBLIC_UNRANKING_UNSEARCHABLE = 3;  // 公开分享 无需密码 不参与排行 不支持搜索

    @TableId(type = IdType.ASSIGN_ID)   // 雪花算法分配
    private Long id;
    private String uuid;
    private Long fileId;
    private Integer type = PUBLIC_RANKING;  // 分享类型
    private String password;
    private Timestamp createdAt;
    private Integer expire;
}
