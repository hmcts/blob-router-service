package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.slf4j.Logger;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.groupingBy;
import static org.slf4j.LoggerFactory.getLogger;

public class RejectedFilesHandler {

    private static final Logger logger = getLogger(RejectedFilesHandler.class);

    private static final String REJECTED_CONTAINER_SUFFIX = "-rejected";

    private final BlobServiceClient storageClient;
    private final EnvelopeRepository envelopeRepository;

    public RejectedFilesHandler(BlobServiceClient storageClient, EnvelopeRepository envelopeRepository) {
        this.storageClient = storageClient;
        this.envelopeRepository = envelopeRepository;
    }

    /**
     * Handles files that were rejected. Ie:
     * - moves files to container for rejected files
     * - removes files from the original container
     * - marks envelopes in the DB as deleted
     */
    public void handle() {
        List<Envelope> rejectedEnvelopes = envelopeRepository.find(Status.REJECTED, false);

        logger.info("Found {} rejected envelopes", rejectedEnvelopes.size());

        rejectedEnvelopes
            .stream()
            .collect(groupingBy(e -> e.container))
            .forEach((container, envelopes) -> {
                logger.info("Started moving rejected files from container {}", container);

                var sourceContainer = storageClient.getBlobContainerClient(container);
                var targetContainer = storageClient.getBlobContainerClient(container + REJECTED_CONTAINER_SUFFIX);

                envelopes.forEach(envelope -> handleEnvelope(sourceContainer, targetContainer, envelope));

                logger.info("Finished moving rejected files from container {}", container);
            });
    }

    private void handleEnvelope(
        BlobContainerClient sourceContainer,
        BlobContainerClient targetContainer,
        Envelope envelope
    ) {
        String loggingContext = String.format(
            "File name: %s. Container: %s",
            envelope.fileName,
            sourceContainer.getBlobContainerName()
        );

        try {
            BlobClient sourceBlob = sourceContainer.getBlobClient(envelope.fileName);
            BlobClient targetBlob = targetContainer.getBlobClient(envelope.fileName);

            if (targetBlob.exists()) {
                targetBlob.createSnapshot();
            }

            if (!sourceBlob.exists()) {
                logger.error("File already deleted. " + loggingContext);
                envelopeRepository.markAsDeleted(envelope.id);
            } else {
                byte[] blobContent = download(sourceBlob);
                upload(targetBlob, blobContent, loggingContext);
                sourceBlob.delete();
                envelopeRepository.markAsDeleted(envelope.id);
                logger.info("Rejected file successfully handled. " + loggingContext);
            }
        } catch (Exception exc) {
            logger.error("Error handling rejected file. " + loggingContext);
        }
    }

    private byte[] download(BlobClient blobClient) throws IOException {
        try (var outputStream = new ByteArrayOutputStream()) {
            blobClient.download(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void upload(BlobClient blobClient, byte[] blobContent, String loggingContext) {
        blobClient
            .getBlockBlobClient()
            .upload(
                new ByteArrayInputStream(blobContent),
                blobContent.length,
                true
            );
        logger.info("File successfully uploaded to rejected container. " + loggingContext);
    }
}
