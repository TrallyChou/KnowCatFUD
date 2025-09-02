package life.trally.knowcatfud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import life.trally.knowcatfud.pojo.entity.UserFile;
import life.trally.knowcatfud.pojo.response.UserFileResponse;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserFileMapper extends BaseMapper<UserFile> {

    @Select("""
            select path,created_at,type from user_file
            where user_id = #{userId} and parent = #{path}
            """
    )
    List<UserFileResponse> getUserFiles(Long userId, String path);

}
