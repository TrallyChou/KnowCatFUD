package life.trally.knowcatfud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import life.trally.knowcatfud.entity.FileShare;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FileShareMapper extends BaseMapper<FileShare> {
    // TODO: 后续改用返回DTO
    @Select("""
            select uuid,file_share.type,password,file_share.created_at,expire from file_share
            inner join user_file on file_id = user_file.id
            where user_id = #{userId}
            """)
    List<FileShare> getShares(Long userId);

    @Select("""
            select user_file.user_id from user_file
            inner join file_share on file_share.file_id = user_file.id
            where file_share.id = #{shareId}
            """)
    Long getUserIdByShareId(Long shareId);
}
