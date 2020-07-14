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

    public List<BlobInfo> listBlobs(int staleTime) {
        return serviceConfiguration
            .getSourceContainers()
            .stream()
            .flatMap(c -> listBlobsByContainer(c, staleTime))
            .collect(toList());
    }

    private Stream<BlobInfo> listBlobsByContainer(String containerName, int staleTime) {
        return storageClient.getBlobContainerClient(containerName)
            .listBlobs()
            .stream()
            .filter(b -> timeFilter(b, staleTime))
            .map(blob -> new BlobInfo(
                    containerName,
                    blob.getName(),
                    toLocalTimeZone(blob.getProperties().getCreationTime().toInstant())
                )
            );
    }

    private boolean timeFilter(BlobItem blobItem, int staleTime) {
        return
            Instant.now().isAfter(
                blobItem
                    .getProperties()
                    .getCreationTime()
                    .toInstant()
                    .plus(staleTime, ChronoUnit.HOURS)
            );
    }

}
