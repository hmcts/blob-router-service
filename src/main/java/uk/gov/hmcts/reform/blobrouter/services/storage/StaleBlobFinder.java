package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.model.out.BlobInfo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.blobrouter.util.TimeUtils.toLocalTimeZone;

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
                    UUID.randomUUID(),
                    toLocalTimeZone(blob.getProperties().getCreationTime().toInstant())
                )
            );
    }

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
