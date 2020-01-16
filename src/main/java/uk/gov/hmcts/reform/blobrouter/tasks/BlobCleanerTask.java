package uk.gov.hmcts.reform.blobrouter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.ContainerCleaner;

import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty(value = "scheduling.task.delete-dispatched-files.enabled")
@EnableConfigurationProperties(ServiceConfiguration.class)
public class BlobCleanerTask {

    private static final String TASK_NAME = "blob-cleaner";

    private static final Logger logger = getLogger(BlobCleanerTask.class);

    private final ContainerCleaner containerCleaner;
    private final ServiceConfiguration serviceConfiguration;

    public BlobCleanerTask(
        ContainerCleaner containerCleaner,
        ServiceConfiguration serviceConfiguration
    ) {
        this.containerCleaner = containerCleaner;
        this.serviceConfiguration = serviceConfiguration;
    }

    @Scheduled(cron = "${scheduling.task.delete-dispatched-files.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = "delete-dispatched-files")
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        getAvailableContainerNames().forEach(containerCleaner::process);

        logger.info("Finished {} job", TASK_NAME);
    }

    private Stream<String> getAvailableContainerNames() {
        return serviceConfiguration.getStorageConfig()
            .values()
            .stream()
            .filter(ServiceConfiguration.StorageConfig::isEnabled)
            .map(ServiceConfiguration.StorageConfig::getName);
    }

}
