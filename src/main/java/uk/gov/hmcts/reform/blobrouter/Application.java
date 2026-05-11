package uk.gov.hmcts.reform.blobrouter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform"})
@SpringBootApplication(
    exclude = {uk.gov.hmcts.reform.authorisation.ServiceAuthAutoConfiguration.class}
)
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
