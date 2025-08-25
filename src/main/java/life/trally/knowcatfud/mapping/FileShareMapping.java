package life.trally.knowcatfud.mapping;

import life.trally.knowcatfud.entity.FileShare;
import life.trally.knowcatfud.entity.FileShareIntroduction;
import life.trally.knowcatfud.request.FileShareRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileShareMapping {

    // FileShareRequest拆分
    // 由于ShareIntroduction中也要存储和FileShare相同的id，故这里同时需要一个FileShare，来访问在插入到数据库后得到的id
    FileShareIntroduction toShareIntroduction(FileShare fileShare, FileShareRequest fileShareRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    FileShare toFileShare(FileShareRequest fileShareRequest);
}
