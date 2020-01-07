package uk.gov.hmcts.reform.blobrouter.util;

import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.models.BlobContainerItem;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public final class StorageClientManager {

    private static final Logger LOGGER = getLogger(StorageClientManager.class);

    private static final String REJECTED_CONTAINER_SUFFIX = "rejected";

    private StorageClientManager() {
        // utility class constructor
    }

    public static Flux<BlobContainerItem> getAvailableContainers(
        BlobServiceAsyncClient storageClient,
        ServiceConfiguration serviceConfiguration
    ) {
        return storageClient
            .listBlobContainers()
            .filter(item -> !item.getName().endsWith(REJECTED_CONTAINER_SUFFIX))
            .filter(item -> isContainerEnabled(serviceConfiguration.getStorageConfig(), item.getName()));
    }

    private static boolean isContainerEnabled(
        Map<String, ServiceConfiguration.StorageConfig> storageConfiguration,
        String containerName
    ) {
        if (storageConfiguration.containsKey(containerName)) {
            return storageConfiguration.get(containerName).isEnabled();
        } else {
            LOGGER.error("No service configuration found for '{}' container", containerName);
            return false;
        }
    }
}
