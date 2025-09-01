package life.trally.knowcatfud.mapping;

import life.trally.knowcatfud.config.MappingConfig;
import life.trally.knowcatfud.entity.User;
import life.trally.knowcatfud.entity.UserFile;
import life.trally.knowcatfud.request.LoginRequest;
import life.trally.knowcatfud.request.RegRequest;
import life.trally.knowcatfud.request.UploadOrMkdirRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", config = MappingConfig.class)  // 配置来忽略缺少的字段
public interface RequestMapping {

    UserFile toUserFile(UploadOrMkdirRequest uploadOrMkdirRequest);

    User toUser(RegRequest regRequest);

    User toUser(LoginRequest loginRequest);

}
