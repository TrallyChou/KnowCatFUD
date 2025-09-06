package life.trally.knowcatfud.rabbitmq.consumers;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import life.trally.knowcatfud.a1sc.service.interfaces.FileShareService;
import life.trally.knowcatfud.ai.pojo.ModerationResult;
import life.trally.knowcatfud.ai.service.ModerationService;
import life.trally.knowcatfud.mapper.FileShareMapper;
import life.trally.knowcatfud.pojo.entity.FileShare;
import life.trally.knowcatfud.pojo.entity.FileShareIntroduction;
import life.trally.knowcatfud.utils.JsonUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ModerationConsumer {

    private final ModerationService moderationService;
    private final RabbitTemplate rabbitTemplate;
    private final FileShareMapper fileShareMapper;
    private final FileShareService fileShareService;

    public ModerationConsumer(ModerationService moderationService, RabbitTemplate rabbitTemplate, FileShareMapper fileShareMapper, FileShareService fileShareService) {
        this.moderationService = moderationService;
        this.rabbitTemplate = rabbitTemplate;
        this.fileShareMapper = fileShareMapper;
        this.fileShareService = fileShareService;
    }

    @RabbitListener(queues = "moderation.queue")
    public void process(String message) {
        FileShareIntroduction fileShareIntroduction =
                JsonUtils.deserialize(message, FileShareIntroduction.class);
        ModerationResult r = moderationService.moderation(fileShareIntroduction);
        if (!r.getSafe()) {
            LambdaUpdateWrapper<FileShare> uw = new LambdaUpdateWrapper<>();
            uw.eq(
                    FileShare::getId, fileShareIntroduction.getId()
            ).set(
                    FileShare::getViolation, true
            ).set(
                    FileShare::getCause, r.getCause()
            );
            fileShareMapper.update(uw);
            fileShareService.clearCache(fileShareIntroduction.getId());  // 实际是为了更新违规标记
            return;
        }

        // 审核通过，允许搜索，加入ES库
        rabbitTemplate.convertAndSend(
                "knowcatfud.share",
                "share",
                JsonUtils.serialize(fileShareIntroduction)
        );

    }
}
