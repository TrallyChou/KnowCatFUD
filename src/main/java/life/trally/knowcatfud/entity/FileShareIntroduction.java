package life.trally.knowcatfud.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Data
@NoArgsConstructor
@AllArgsConstructor
// mysql
@TableName("share_introduction")
// ES
@Document(indexName = "share_introductions")
@Setting(shards = 1, replicas = 0)
public class FileShareIntroduction {
    @Id        // ES
    @Field
    @TableId(type = IdType.NONE)  // 需手动指定以保持数据一致
    private Long id;

    @TableField(exist = false)     // 数据库中不存在该字段
    @Field(type = FieldType.Keyword, index = false)
    private String uuid;  // 只存储在ES中

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;

    // 分享图标、分享中的图片链接等，均于前端固定某种标记格式，进行存储和读取
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String introduction;

}
