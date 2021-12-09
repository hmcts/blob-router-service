package uk.gov.hmcts.reform.blobrouter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.blobrouter.Application;

@Configuration
public class SwaggerConfiguration {

    public static final String BASE_PACKAGE = Application.class.getPackage().getName();

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
            .info(
                new Info().title("Send letter API")
                    .description("Send letter handlers")
                    .version("v0.0.1")
            );
    }
}
