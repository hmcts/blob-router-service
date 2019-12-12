package uk.gov.hmcts.reform.blobrouter.util;

import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.models.BlobContainerItem;
import reactor.core.publisher.Flux;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.exceptions.ServiceConfigNotFoundException;

import java.util.Map;

public final class StorageClientManager {

    private static final String REJECTED_CONTAINER_PREFIX = "rejected";

    private StorageClientManager() {
        // utility class constructor
    }

    public static Flux<BlobContainerItem> getAvailableContainers(
        BlobServiceAsyncClient storageClient,
        ServiceConfiguration serviceConfiguration
    ) {
        return storageClient
            .listBlobContainers()
            .filter(item -> !item.getName().endsWith(REJECTED_CONTAINER_PREFIX))
            .filter(item -> isContainerEnabled(serviceConfiguration.getStorageConfig(), item.getName()));
    }

    private static boolean isContainerEnabled(
        Map<String, ServiceConfiguration.StorageConfig> storageConfiguration,
        String containerName
    ) {
        if (storageConfiguration.containsKey(containerName)) {
            return storageConfiguration.get(containerName).isEnabled();
        } else {
            throw new ServiceConfigNotFoundException("No service configuration found for " + containerName);
        }
    }
}
