package life.trally.knowcatfud.rabbitmq.consumers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import life.trally.knowcatfud.pojo.entity.FileShare;
import life.trally.knowcatfud.pojo.entity.FileShareIntroduction;
import life.trally.knowcatfud.pojo.entity.UserLikesShare;
import life.trally.knowcatfud.mapper.FileShareIntroductionMapper;
import life.trally.knowcatfud.mapper.FileShareMapper;
import life.trally.knowcatfud.mapper.UserLikesShareMapper;
import life.trally.knowcatfud.rabbitmq.messages.DeleteShareMessage;
import life.trally.knowcatfud.utils.JsonUtils;
import life.trally.knowcatfud.utils.RedisUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

@Component
public class ShareConsumer {

    private final ElasticsearchOperations elasticsearchOperations;
    private final RedisUtils redisUtils;
    private final FileShareMapper fileShareMapper;
    private final FileShareIntroductionMapper fileShareIntroductionMapper;
    private final UserLikesShareMapper userLikesShareMapper;

    public ShareConsumer(ElasticsearchOperations elasticsearchOperations, RedisUtils redisUtils, FileShareMapper fileShareMapper, FileShareIntroductionMapper fileShareIntroductionMapper, UserLikesShareMapper userLikesShareMapper) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.redisUtils = redisUtils;
        this.fileShareMapper = fileShareMapper;
        this.fileShareIntroductionMapper = fileShareIntroductionMapper;
        this.userLikesShareMapper = userLikesShareMapper;
    }


    // 分享介绍加入ES
    @RabbitListener(queues = "share.queue")
    public void processShare(String message) {
        FileShareIntroduction fileShareIntroduction =
                JsonUtils.deserialize(message, FileShareIntroduction.class);
        elasticsearchOperations.save(fileShareIntroduction);
    }

    // 删除分享
    @RabbitListener(queues = "share_delete.queue")
    public void processDelete(String message) {

        DeleteShareMessage deleteShareMessage = JsonUtils.deserialize(message, DeleteShareMessage.class);

        Long shareId = deleteShareMessage.getShareId();
        String uuid = deleteShareMessage.getUuid();

        // 清理缓存
        redisUtils.delete("share:likes:" + shareId);

        if (String.valueOf(FileShare.PUBLIC_RANKING)
                .equals(redisUtils.hGet("share:info:" + shareId, "type"))) {
            redisUtils.zDel("share:ranking", String.valueOf(shareId));
        }
        redisUtils.delete("share:info:" + shareId);
        fileShareMapper.deleteById(shareId);
        redisUtils.delete("share:uuid_id:" + uuid);

        fileShareIntroductionMapper.deleteById(shareId);

        // 删除点赞记录
        LambdaQueryWrapper<UserLikesShare> qw = new LambdaQueryWrapper<>();
        qw.eq(UserLikesShare::getShareId, shareId);
        userLikesShareMapper.delete(qw);

        // 删除ES文档
        elasticsearchOperations.delete(String.valueOf(shareId),FileShareIntroduction.class);

    }


}
