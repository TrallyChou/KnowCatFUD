package life.trally.knowcatfud.response;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class UserFileResponse {

    private String path;
    private Timestamp createdAt;
    private Integer type;

}
