package life.trally.knowcatfud.rabbitmq.consumers;

import life.trally.knowcatfud.entity.FileShareIntroduction;
import life.trally.knowcatfud.utils.JsonUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

@Component
public class ShareConsumer {

    private final ElasticsearchOperations elasticsearchOperations;

    public ShareConsumer(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }


    @RabbitListener(queues = "share.queue")
    public void process(String message) {
        FileShareIntroduction fileShareIntroduction =
                JsonUtils.deserialize(message, FileShareIntroduction.class);
        elasticsearchOperations.save(fileShareIntroduction);
    }


}
