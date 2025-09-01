package life.trally.knowcatfud.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "KnowCatFUD 文档",
                version = "1.0.0"
        )
)
public class SwaggerConfig {

}
