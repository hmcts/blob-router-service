package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.model.out.BlobInfo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Component
@EnableConfigurationProperties(ServiceConfiguration.class)
public class StaleBlobFinder {

    private final BlobServiceClient storageClient;
    private final ServiceConfiguration serviceConfiguration;

    public StaleBlobFinder(BlobServiceClient storageClient, ServiceConfiguration serviceConfiguration) {
        this.storageClient = storageClient;
        this.serviceConfiguration = serviceConfiguration;
    }

    public List<BlobInfo> findStaleBlobs(int staleTime) {
        return serviceConfiguration
            .getSourceContainers()
            .stream()
            .flatMap(c -> findStaleBlobsByContainer(c, staleTime))
            .collect(toList());
    }

    private Stream<BlobInfo> findStaleBlobsByContainer(String containerName, int staleTime) {
        return storageClient.getBlobContainerClient(containerName)
            .listBlobs()
            .stream()
            .filter(b -> isStale(b, staleTime))
            .map(blob -> new BlobInfo(
                    containerName,
                    blob.getName(),
                    blob.getProperties().getCreationTime().toInstant()
                )
            )
            .sorted(Comparator.comparing(o -> o.createdAt));
    }

    /**
     * The function `isStale` determines if a `BlobItem` object is considered stale based on a specified stale time in
     * minutes.
     *
     * @param blobItem BlobItem is an object representing a blob in a storage system.
     *                 It contains properties such as creation time.
     * @param staleTime The `staleTime` parameter in the `isStale` method represents the duration in
     *                  minutes after which a `BlobItem` object is considered stale. This method checks
     *                  if the current time is after the creation time of the `BlobItem` plus the
     *                  specified `staleTime` duration.
     * @return The method `isStale` is returning a boolean value indicating whether the `BlobItem`
     *      object is considered stale based on the provided `staleTime`.
     */
    private boolean isStale(BlobItem blobItem, int staleTime) {
        return
            Instant.now().isAfter(
                blobItem
                    .getProperties()
                    .getCreationTime()
                    .toInstant()
                    .plus(staleTime, ChronoUnit.MINUTES)
            );
    }
}
