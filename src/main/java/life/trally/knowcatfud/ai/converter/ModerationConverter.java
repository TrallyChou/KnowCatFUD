package life.trally.knowcatfud.ai.converter;

import life.trally.knowcatfud.ai.pojo.ModerationResult;
import life.trally.knowcatfud.utils.JsonUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.converter.StructuredOutputConverter;

// 最终选择了不使用自定义Converter，因为英文省token，嘻嘻
@Deprecated
@Slf4j
public class ModerationConverter implements StructuredOutputConverter<ModerationResult> {
    @Getter // 单例模式
    private static final ModerationConverter instance = new ModerationConverter();

    @Override
    public String getFormat() {
        return """
                你必须用JSON格式来回答
                不要包含任何解释，只提供一个遵守RFC8259文件的JSON响应
                回答不要使用Markdown
                这是你的输出必须遵守的JSON schema:
                ```{
                    "$schema" : "https://json-schema.org/draft/2020-12/schema",
                    "type" : "object",
                    "properties" : {
                      "isSafe" : {
                        "type" : "boolean"
                      },
                      "cause" : {
                        "type" : "string"
                      }
                    },
                    "additionalProperties" : false
                }```
                
                """;
    }

    @Override
    public ModerationResult convert(@NotNull String source) {
        try {
            return JsonUtils.deserialize(source, ModerationResult.class);
        } catch (Exception e) {
            log.error("AI审核结果转换失败", e);
            return null;
        }

    }
}
