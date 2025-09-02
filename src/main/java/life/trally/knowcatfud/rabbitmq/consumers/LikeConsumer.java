package life.trally.knowcatfud.rabbitmq.consumers;


import life.trally.knowcatfud.mapper.UserLikesShareMapper;
import life.trally.knowcatfud.pojo.entity.UserLikesShare;
import life.trally.knowcatfud.rabbitmq.messages.LikeMessage;
import life.trally.knowcatfud.utils.JsonUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LikeConsumer {

    private final UserLikesShareMapper userLikesShareMapper;

    public LikeConsumer(UserLikesShareMapper userLikesShareMapper) {
        this.userLikesShareMapper = userLikesShareMapper;
    }

    @RabbitListener(queues = "like.queue")
    public void process(String message) {
        LikeMessage likeMessage = JsonUtils.deserialize(message, LikeMessage.class);

        UserLikesShare userLikesShare = new UserLikesShare();
        userLikesShare.setUserId(likeMessage.getUserId());
        userLikesShare.setShareId(likeMessage.getShareId());
        userLikesShareMapper.insert(userLikesShare);

    }


}
