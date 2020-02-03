package uk.gov.hmcts.reform.blobrouter.tasks;

import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfig;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.ContainerProcessor;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ServiceConfiguration.class)
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

    @Scheduled(fixedDelayString = "${scheduling.task.scan.delay}")
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        List<String> containers = getAvailableContainers();
        Collections.shuffle(containers);
        containers.forEach(containerProcessor::process);

        logger.info("Finished {} job", TASK_NAME);
    }

    private List<String> getAvailableContainers() {
        return serviceConfiguration.getStorageConfig()
            .values()
            .stream()
            .filter(StorageConfig::isEnabled)
            .map(StorageConfig::getName)
            .collect(toList());
    }

}
