package uk.gov.hmcts.reform.blobrouter.tasks;

import org.slf4j.Logger;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.ContainerCleaner;

import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

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

    public void run() {
        logger.info("Started {} job", TASK_NAME);

        getAvailableContainers().forEach(containerCleaner::process);

        logger.info("Finished {} job", TASK_NAME);
    }

    private Stream<String> getAvailableContainers() {
        return serviceConfiguration.getStorageConfig()
            .values()
            .stream()
            .filter(ServiceConfiguration.StorageConfig::isEnabled)
            .map(ServiceConfiguration.StorageConfig::getName);
    }

}
