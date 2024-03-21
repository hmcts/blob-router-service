package uk.gov.hmcts.reform.blobrouter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The SwaggerConfiguration class defines a bean for configuring Swagger documentation with information about the Blob
 * Router API.
 */
@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
            .info(
                new Info().title("Blob Router API")
                    .description("Blob Router handlers")
                    .version("v0.0.1")
            );
    }
}
