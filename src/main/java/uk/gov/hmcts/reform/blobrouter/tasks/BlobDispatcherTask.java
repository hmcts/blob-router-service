package uk.gov.hmcts.reform.blobrouter.tasks;

import org.slf4j.Logger;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.ContainerProcessor;

import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

public class BlobDispatcherTask {

    static final String TASK_NAME = "blob-dispatcher";

    private static final Logger logger = getLogger(BlobDispatcherTask.class);

    private final ContainerProcessor containerProcessor;
    private final ServiceConfiguration serviceConfiguration;

    public BlobDispatcherTask(
        ContainerProcessor containerProcessor,
        ServiceConfiguration serviceConfiguration
    ) {
        this.containerProcessor = containerProcessor;
        this.serviceConfiguration = serviceConfiguration;
    }

    public void run() {
        logger.info("Started {} job", TASK_NAME);

        getAvailableContainers().forEach(containerProcessor::process);

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
