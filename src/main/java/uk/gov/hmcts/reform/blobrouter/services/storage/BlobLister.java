package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.model.out.BlobInfo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.blobrouter.util.TimeUtils.toLocalTimeZone;

@Component
public class BlobLister {

    private final BlobServiceClient storageClient;
    private final ServiceConfiguration serviceConfiguration;

    public BlobLister(BlobServiceClient storageClient, ServiceConfiguration serviceConfiguration) {
        this.storageClient = storageClient;
        this.serviceConfiguration = serviceConfiguration;
    }

    public List<BlobInfo> listBlobs(Predicate<BlobItem> filter) {
        return serviceConfiguration
            .getSourceContainers()
            .stream()
            .map(c -> listBlobsByContainer(c, filter))
            .flatMap(list -> list.stream())
            .collect(Collectors.toList());
    }

    private List<BlobInfo> listBlobsByContainer(String containerName, Predicate<BlobItem> filter) {
        return storageClient.getBlobContainerClient(containerName)
            .listBlobs()
            .stream()
            .filter(filter)
            .map(blob -> new BlobInfo(containerName, blob.getName(),
                toLocalTimeZone(blob.getProperties().getCreationTime().toInstant())))
            .collect(Collectors.toList());
    }

    public Predicate<BlobItem> timeFilter(int staleTime) {
        return blobItem ->
            Instant
                .now()
                .isAfter(
                    blobItem.getProperties().getCreationTime().toInstant()
                        .plus(staleTime, ChronoUnit.HOURS)
                );
    }

}
