package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobServiceClient;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

import java.time.Instant;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class DuplicateFinder {

    private final BlobServiceClient storageClient;
    private final EnvelopeService envelopeService;

    public DuplicateFinder(
        BlobServiceClient storageClient,
        EnvelopeService envelopeService
    ) {
        this.storageClient = storageClient;
        this.envelopeService = envelopeService;
    }

    public List<Duplicate> findIn(String containerName) {
        return storageClient
            .getBlobContainerClient(containerName)
            .listBlobs()
            .stream()
            .filter(blobItem -> isDuplicate(blobItem.getName(), containerName))
            .map(blobItem -> new Duplicate(
                    blobItem.getName(),
                    containerName,
                    blobItem.getProperties().getLastModified().toInstant(),
                    blobItem.getProperties().getContentLength()
            ))
            .collect(toList());
    }

    private boolean isDuplicate(String fileName, String container) {
        return envelopeService
            .findLastEnvelope(fileName, container)
            .filter(envelope -> envelope.isDeleted)
            .isPresent();
    }

    public static class Duplicate {
        public final String fileName;
        public final String container;
        public final Instant blobCreatedAt;
        public final Long fileSize;

        public Duplicate(String fileName, String container, Instant blobCreatedAt, Long fileSize) {
            this.fileName = fileName;
            this.container = container;
            this.blobCreatedAt = blobCreatedAt;
            this.fileSize = fileSize;
        }
    }
}
