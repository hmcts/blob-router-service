package uk.gov.hmcts.reform.blobrouter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;

import java.util.stream.Stream;

public class FlywayConfig {

    @Bean
    @ConditionalOnProperty(prefix = "flyway", name = "skip-migrations", havingValue = "true")
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        // don't run migrations from the app, just check if all were applied
        return flyway ->
            Stream
                .of(flyway.info().all())
                .filter(migration -> !migration.getState().isApplied())
                .findFirst()
                .ifPresent(notAppliedMigration -> {
                    throw new PendingMigrationException("Pending migration: " + notAppliedMigration.getScript());
                });
    }
}
