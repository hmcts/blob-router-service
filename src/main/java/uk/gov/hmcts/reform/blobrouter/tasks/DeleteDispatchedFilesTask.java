package uk.gov.hmcts.reform.blobrouter.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.ContainerCleaner;

import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty(value = "scheduling.task.delete-dispatched-files.enabled")
@EnableConfigurationProperties(ServiceConfiguration.class)
public class DeleteDispatchedFilesTask {

    private static final String TASK_NAME = "delete-dispatched-files";

    private static final Logger logger = getLogger(DeleteDispatchedFilesTask.class);

    private final ContainerCleaner containerCleaner;
    private final ServiceConfiguration serviceConfiguration;

    public DeleteDispatchedFilesTask(
        ContainerCleaner containerCleaner,
        ServiceConfiguration serviceConfiguration
    ) {
        this.containerCleaner = containerCleaner;
        this.serviceConfiguration = serviceConfiguration;
    }

    /**
     * This Java function runs periodically to delete dispatched files from available containers.
     */
    @Scheduled(cron = "${scheduling.task.delete-dispatched-files.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = "delete-dispatched-files")
    public void run() {
        logger.debug("Started {} job", TASK_NAME);

        getAvailableContainerNames().forEach(containerCleaner::process);

        logger.debug("Finished {} job", TASK_NAME);
    }

    /**
     * The function `getAvailableContainerNames` returns a stream of names of enabled storage containers
     * from the service configuration.
     *
     * @return A stream of available container names is being returned.
     */
    private Stream<String> getAvailableContainerNames() {
        return serviceConfiguration.getStorageConfig()
            .values()
            .stream()
            .filter(StorageConfigItem::isEnabled)
            .map(StorageConfigItem::getSourceContainer);
    }
}
