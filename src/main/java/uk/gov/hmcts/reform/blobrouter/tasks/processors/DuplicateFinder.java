package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

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

    public List<BlobItem> findIn(String containerName) {
        return storageClient
            .getBlobContainerClient(containerName)
            .listBlobs()
            .stream()
            .filter(blob -> hasAlreadyBeenProcessed(blob.getName(), containerName))
            .collect(toList());
    }

    private boolean hasAlreadyBeenProcessed(String fileName, String containerName) {
        return envelopeService
            .findEnvelope(fileName, containerName)
            .map(e -> e.isDeleted)
            .orElse(false);
    }
}
