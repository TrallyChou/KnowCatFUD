package life.trally.knowcatfud.rabbitmq.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteShareMessage {
    private Long shareId;
    private String uuid;
}
