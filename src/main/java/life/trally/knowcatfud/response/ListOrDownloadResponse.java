package life.trally.knowcatfud.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListOrDownloadResponse {
    List<UserFileResponse> list;
    String downloadToken;
}
