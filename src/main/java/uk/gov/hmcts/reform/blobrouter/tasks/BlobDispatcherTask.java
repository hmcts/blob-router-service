package uk.gov.hmcts.reform.blobrouter.tasks;

import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.models.BlobContainerItem;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.ContainerProcessor;
import uk.gov.hmcts.reform.blobrouter.util.StorageClientManager;

import static org.slf4j.LoggerFactory.getLogger;

public class BlobDispatcherTask {

    static final String TASK_NAME = "blob-dispatcher";

    private static final Logger logger = getLogger(BlobDispatcherTask.class);

    private final BlobServiceAsyncClient storageClient;
    private final ContainerProcessor containerProcessor;
    private final ServiceConfiguration serviceConfiguration;

    public BlobDispatcherTask(
        BlobServiceAsyncClient storageClient,
        ContainerProcessor containerProcessor,
        ServiceConfiguration serviceConfiguration
    ) {
        this.containerProcessor = containerProcessor;
        this.serviceConfiguration = serviceConfiguration;
        this.storageClient = storageClient;
    }

    public void run() {
        logger.info("Started {} job", TASK_NAME);

        StorageClientManager
            .getAvailableContainers(storageClient, serviceConfiguration)
            .toStream()
            .map(BlobContainerItem::getName)
            .map(storageClient::getBlobContainerAsyncClient)
            .forEach(containerProcessor::process);

        logger.info("Finished {} job", TASK_NAME);
    }
}
