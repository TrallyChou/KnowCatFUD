package life.trally.knowcatfud.pojo;

import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("users")
public class User implements Serializable {

    @TableId(type = IdType.AUTO) // 主键，自增
    private Long id;
    private String username;

    @JSONField(serialize = false)
    private String password;
}
