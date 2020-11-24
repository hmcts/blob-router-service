package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import uk.gov.hmcts.reform.blobrouter.Application;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    public static final String BASE_PACKAGE = Application.class.getPackage().getName();

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .useDefaultResponseMessages(false)
            .select()
            .apis(RequestHandlerSelectors.basePackage(BASE_PACKAGE + ".controllers")
                      .or(RequestHandlerSelectors.basePackage(BASE_PACKAGE + ".reconciliation.controller")))
            .paths(PathSelectors.ant("/token/*")
                       .or((PathSelectors.ant("/reconciliation-report/*"))))
            .build();
    }

}
