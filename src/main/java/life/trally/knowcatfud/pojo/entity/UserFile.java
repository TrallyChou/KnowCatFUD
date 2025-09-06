package life.trally.knowcatfud.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_file")
public class UserFile {

    public final static int TYPE_DIR = 0;
    public final static int TYPE_FILE = 1;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;      // username仅用作登录，数据库存储内容皆用user_id
    private String filename;
    private String parent;
    private String path;
    private Timestamp createdAt;
    private Integer type;
    private String hash;
    private Long size;

    public static UserFile rootDir(Long userId) {
        return new UserFile(null, userId, null, null, "/", null, TYPE_DIR, null, null);
    }


}
