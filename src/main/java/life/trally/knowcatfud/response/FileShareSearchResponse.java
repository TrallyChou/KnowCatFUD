package life.trally.knowcatfud.response;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
public class FileShareSearchResponse {
    private String uuid;  // 只存储在ES中
    private String title;
    private String introduction;
}
