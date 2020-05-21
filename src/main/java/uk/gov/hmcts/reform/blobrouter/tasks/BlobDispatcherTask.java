package uk.gov.hmcts.reform.blobrouter.tasks;

import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.ContainerProcessor;

import java.util.Collections;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ServiceConfiguration.class)
public class BlobDispatcherTask {

    static final String TASK_NAME = "blob-dispatcher";

    private static final Logger logger = getLogger(BlobDispatcherTask.class);

    private final ContainerProcessor containerProcessor;
    private final List<String> containers;

    public BlobDispatcherTask(
        ContainerProcessor containerProcessor,
        ServiceConfiguration serviceConfiguration
    ) {
        this.containerProcessor = containerProcessor;
        this.containers = serviceConfiguration.getEnabledSourceContainers();
    }

    //@Scheduled(fixedDelayString = "${scheduling.task.scan.delay}")
    @Scheduled(fixedDelayString = "50")
    public void run() {
        logger.info("Started {} job", TASK_NAME);

        Collections.shuffle(containers);
        containers.forEach(containerProcessor::process);

        logger.info("Finished {} job", TASK_NAME);
    }
}
