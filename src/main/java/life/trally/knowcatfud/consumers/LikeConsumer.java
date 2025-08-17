package life.trally.knowcatfud.consumers;


import com.fasterxml.jackson.core.type.TypeReference;
import life.trally.knowcatfud.dao.UserLikesShareMapper;
import life.trally.knowcatfud.pojo.UserLikesShare;
import life.trally.knowcatfud.utils.JsonUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LikeConsumer {

    private final UserLikesShareMapper userLikesShareMapper;

    private final RabbitTemplate rabbitTemplate;

    public LikeConsumer(UserLikesShareMapper userLikesShareMapper, RabbitTemplate rabbitTemplate) {
        this.userLikesShareMapper = userLikesShareMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "like.queue")
    public void process(String message) {
        Map<String, Object> event = JsonUtils.deserialize(message, new TypeReference<Map<String, Object>>() {
        });
        Integer userId = (Integer) event.get("userId");
        Integer shareId = (Integer) event.get("shareId");

        UserLikesShare userLikesShare = new UserLikesShare();
        userLikesShare.setUserId(Long.valueOf(userId));
        userLikesShare.setShareId(Long.valueOf(shareId));
        userLikesShareMapper.insert(userLikesShare);

        System.out.println("yes");
    }


}
