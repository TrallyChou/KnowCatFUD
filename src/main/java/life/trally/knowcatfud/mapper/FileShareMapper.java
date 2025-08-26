package life.trally.knowcatfud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import life.trally.knowcatfud.entity.FileShare;
import life.trally.knowcatfud.response.FileShareResponseForCreator;
import life.trally.knowcatfud.response.FileShareResponseForOtherUsers;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FileShareMapper extends BaseMapper<FileShare> {
    @Select("""
            select uuid,file_share.type,password,expire,file_share.created_at,
                   title,introduction,path
            from file_share
            inner join user_file on file_id = user_file.id
            inner join share_introduction on share_introduction.id = file_share.id
            where user_id = #{userId}
            """)
    List<FileShareResponseForCreator> getShares(Long userId);

    @Select("""
            select user_file.user_id from user_file
            inner join file_share on file_share.file_id = user_file.id
            where file_share.id = #{shareId}
            """)
    Long getUserIdByShareId(Long shareId);

    @Select("""
            select type,expire,title,introduction,created_at
            from file_share
            inner join share_introduction on file_share.id = share_introduction.id
            where file_share.id = #{shareId}
            """)
    FileShareResponseForOtherUsers getShare(Long shareId);
}
