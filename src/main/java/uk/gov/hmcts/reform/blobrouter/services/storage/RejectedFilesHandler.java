package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.util.stream.Collectors.groupingBy;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class RejectedFilesHandler {

    private static final Logger logger = getLogger(RejectedFilesHandler.class);

    public static final String REJECTED_CONTAINER_SUFFIX = "-rejected";

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
            "File name: %s. Source Container: %s. Target Container: %s",
            envelope.fileName,
            sourceContainer.getBlobContainerName(),
            targetContainer.getBlobContainerName()
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
                copy(targetBlob, sourceBlob, loggingContext);
                sourceBlob.delete();
                envelopeRepository.markAsDeleted(envelope.id);
                logger.info("Rejected file successfully handled. " + loggingContext);
            }
        } catch (Exception exc) {
            logger.error("Error handling rejected file. {}", loggingContext, exc);
        }
    }

    private void copy(BlobClient targetBlob, BlobClient sourceBlob, String loggingContext) {
        logger.info(
            "File copying from url: {}, to url {} ",
            sourceBlob.getBlockBlobClient().getBlobUrl(),
            targetBlob.getBlockBlobClient().getBlobUrl()
        );

        targetBlob
            .getBlockBlobClient().copyFromUrl(sourceBlob.getBlobUrl() + "?" + createSas(sourceBlob));

        logger.info("File successfully uploaded to rejected container. " + loggingContext);
    }

    private String createSas(BlobClient sourceBlob) {
        var expiryTime =
            OffsetDateTime.of(LocalDateTime.now().plus(5, ChronoUnit.MINUTES), ZoneOffset.UTC);

        var blobServiceSasSignatureValues =
            new BlobServiceSasSignatureValues(
                expiryTime,
                new BlobContainerSasPermission().setReadPermission(true)
            );

        return sourceBlob.generateSas(blobServiceSasSignatureValues);
    }
}
