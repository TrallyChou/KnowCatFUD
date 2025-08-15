package life.trally.knowcatfud.pojo;

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

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String filename;
    private String parent;
    private String path;
    private Timestamp createdAt;
    private Integer type;
    private String hash;
    private Integer size;

    public static UserFile rootDir(String username) {
        return new UserFile(null, username, null, null, "/", null, TYPE_DIR, null, null);
    }


}
