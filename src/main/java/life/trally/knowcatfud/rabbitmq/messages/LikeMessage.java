package life.trally.knowcatfud.rabbitmq.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeMessage {
    private Long userId;
    private Long shareId;
}
