package life.trally.knowcatfud.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadOrMkdirResponse {
    String token;
    Long startByte;
}
