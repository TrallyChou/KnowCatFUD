package life.trally.knowcatfud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import life.trally.knowcatfud.pojo.entity.UserLikesShare;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserLikesShareMapper extends BaseMapper<UserLikesShare> {

    @Select("select user_id from user_likes_share where share_id = #{shareId}")
    List<Long> getLikeUsers(Long shareId);

    @Select("select user_id from user_likes_share where share_id = #{shareId}")
    List<String> getLikeUsersString(Long shareId);

}
