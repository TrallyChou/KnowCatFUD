package life.trally.knowcatfud.ai.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModerationResult {
    private Boolean safe;
    private String cause;
}
