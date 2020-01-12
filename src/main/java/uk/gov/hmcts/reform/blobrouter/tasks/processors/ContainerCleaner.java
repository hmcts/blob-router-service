package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;

import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.data.model.Status.DISPATCHED;

@SuppressWarnings("java:S1135") // ignore TODOs. will be removed when blob deletion part is implemented
public class ContainerCleaner {

    private static final Logger logger = getLogger(ContainerCleaner.class);

    private final BlobServiceClient storageClient;
    private final EnvelopeRepository envelopeRepository;

    public ContainerCleaner(
        BlobServiceClient storageClient,
        EnvelopeRepository envelopeRepository
    ) {
        this.storageClient = storageClient;
        this.envelopeRepository = envelopeRepository;
    }

    public void process(String containerName) {
        logger.info("Deleting from container {}", containerName);

        Map<String, UUID> envelopesToDelete = envelopeRepository.find(DISPATCHED, false)
            .stream()
            .collect(toMap(env -> env.fileName, env -> env.id));
        BlobContainerClient containerClient = storageClient.getBlobContainerClient(containerName);

        containerClient
            .listBlobs()
            .stream()
            .map(BlobItem::getName)
            .forEach(blobName -> {
                tryToDeleteBlob(envelopesToDelete, containerClient, blobName);
            });
    }

    private void tryToDeleteBlob(Map<String, UUID> envelopesToDelete, BlobContainerClient containerClient, String blobName) {
        BlobClient blob = containerClient.getBlobClient(blobName);

        if (envelopesToDelete.containsKey(blobName)) {
            blob.delete();
            envelopeRepository.markAsDeleted(envelopesToDelete.get(blobName));
            logger.info("Deleted file {}", blobName);
        }
    }
}
