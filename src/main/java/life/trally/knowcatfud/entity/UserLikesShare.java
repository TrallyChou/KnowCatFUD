package life.trally.knowcatfud.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_likes_share")
public class UserLikesShare {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Long shareId;
}
