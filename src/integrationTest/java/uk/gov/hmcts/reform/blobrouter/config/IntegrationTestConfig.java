package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.blobrouter.reconciliation.service.ReconciliationMailService;

import static org.mockito.Mockito.mock;

@Configuration
public class IntegrationTestConfig {

    @Bean
    @ConditionalOnProperty(name = "scheduling.task.send-reconciliation-report-mail.enabled", havingValue = "false")
    public ReconciliationMailService reconciliationMailService() {
        return mock(ReconciliationMailService.class);
    }
}
