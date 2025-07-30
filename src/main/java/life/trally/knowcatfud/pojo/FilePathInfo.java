package life.trally.knowcatfud.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 用以保存目录和文件
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName("file_path_info")
public class FilePathInfo {

    public final static int TYPE_DIR = 0;
    public final static int TYPE_FILE = 1;

    @TableId(value = "user_path", type = IdType.INPUT)
    private String userPath;    // 用户名和目录
    private int type;       // 类型，0表示目录，1表示文件
    private String hash;     // 文件hash值 如果是目录则忽略
    private long size;      // 文件大小 如果是目录则忽略

}