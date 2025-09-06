package life.trally.knowcatfud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import life.trally.knowcatfud.pojo.entity.FileShare;
import life.trally.knowcatfud.pojo.response.GetSharesResponse;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FileShareMapper extends BaseMapper<FileShare> {
    @Select("""
            select uuid,file_share.type,password,expire,file_share.created_at,
                   title,introduction,path,violation,cause
            from file_share
            inner join user_file on file_id = user_file.id
            inner join share_introduction on share_introduction.id = file_share.id
            where user_id = #{userId}
            """)
    List<GetSharesResponse> getMyShares(Long userId);

    @Select("""
            select user_file.user_id from user_file
            inner join file_share on file_share.file_id = user_file.id
            where file_share.id = #{shareId}
            """)
    Long getUserIdByShareId(Long shareId);
}
