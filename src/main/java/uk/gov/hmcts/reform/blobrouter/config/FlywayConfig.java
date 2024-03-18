package uk.gov.hmcts.reform.blobrouter.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Stream;

/**
 * The `FlywayConfig` class in Java checks if all Flyway migrations have been applied and throws an exception
 * if any are pending.
 */
@AutoConfigureAfter({DataSourceAutoConfiguration.class})
@AutoConfigureBefore({FlywayAutoConfiguration.class})
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
public class FlywayConfig {

    /**
     * This function checks if all Flyway migrations have been applied and throws an exception if any are pending.
     *
     * @return A `FlywayMigrationStrategy` bean is being returned. This bean is conditional on the property
     *      `flyway.skip-migrations` being set to `true`. If this property is set to `true`, the bean will be
     *      created and it will check if all migrations have been applied. If there are any pending migrations, a
     *      `PendingMigrationException` will be thrown with the details of the pending migration.
     */
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
