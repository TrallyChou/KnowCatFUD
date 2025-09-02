package life.trally.knowcatfud.mapping;

import life.trally.knowcatfud.config.MappingConfig;
import life.trally.knowcatfud.pojo.entity.FileShare;
import life.trally.knowcatfud.pojo.entity.FileShareIntroduction;
import life.trally.knowcatfud.pojo.request.ShareRequest;
import life.trally.knowcatfud.pojo.response.FileShareSearchResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", config = MappingConfig.class)
public interface FileShareMapping {

    // FileShareRequest拆分
    // 由于ShareIntroduction中也要存储和FileShare相同的id，故这里同时需要一个FileShare，来访问在插入到数据库后得到的id
    FileShareIntroduction toShareIntroduction(FileShare fileShare, ShareRequest fileShareRequest);

    FileShare toFileShare(ShareRequest shareRequest);

    List<FileShareSearchResponse> toFileShareSearchResponse(List<FileShareIntroduction> fileShareIntroductions);
}
