package life.trally.knowcatfud.mapping;

import life.trally.knowcatfud.pojo.entity.UserFile;
import life.trally.knowcatfud.pojo.response.UploadOrMkdirResponse;
import life.trally.knowcatfud.pojo.service.UserFileUploading;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileServiceMapping {
    UserFileUploading toUserFileUploading(UserFile userFile, UploadOrMkdirResponse uploadOrMkdirResponse);

    @Mapping(target = "token", ignore = true)
    @Mapping(target = "startByte", ignore = true)
    UserFileUploading toUserFileUploading(UserFile userFile);

    UserFile toUserFile(UserFileUploading userFileUploading);

}
