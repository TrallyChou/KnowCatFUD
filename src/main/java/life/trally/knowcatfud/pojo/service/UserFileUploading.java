package life.trally.knowcatfud.pojo.service;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class UserFileUploading {

    private Long id;

    private Long userId;      // username仅用作登录，数据库存储内容皆用user_id
    private String filename;
    private String parent;
    private String path;
    private Timestamp createdAt;
    private Integer type;
    private String hash;
    private Long size;
    private String token;
    private Long startByte;

}
