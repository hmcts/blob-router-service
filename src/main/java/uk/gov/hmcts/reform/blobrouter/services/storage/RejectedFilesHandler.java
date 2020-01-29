package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobClient;
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

    void run() {
        List<Envelope> rejectedEnvelopes = envelopeRepository.find(Status.REJECTED, false);

        logger.info("Found {} rejected envelopes", rejectedEnvelopes.size());

        rejectedEnvelopes
            .stream()
            .collect(groupingBy(e -> e.container))
            .keySet()
            .forEach(container -> {

                logger.info("Moving rejected files from container {}", container);

                var sourceContainer = storageClient.getBlobContainerClient(container);
                var targetContainer = storageClient.getBlobContainerClient(container + REJECTED_CONTAINER_SUFFIX);

                rejectedEnvelopes
                    .stream()
                    .collect(groupingBy(env -> env.container))
                    .get(container)
                    .forEach(envelope -> {
                        try {
                            BlobClient sourceBlob = sourceContainer.getBlobClient(envelope.fileName);
                            BlobClient targetBlob = targetContainer.getBlobClient(envelope.fileName);

                            byte[] blobContent = download(sourceBlob);
                            upload(targetBlob, blobContent);
                            sourceBlob.delete();
                            envelopeRepository.markAsDeleted(envelope.id);

                        } catch (Exception exc) {
                            logger.error(
                                "Error moving file to rejected container. File name: {}. Container: {}",
                                envelope.fileName,
                                container,
                                exc
                            );
                        }
                    });
            });
    }

    private byte[] download(BlobClient blobClient) throws IOException {
        try (var outputStream = new ByteArrayOutputStream()) {
            blobClient.download(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void upload(BlobClient blobClient, byte[] blobContent) {
        blobClient
            .getBlockBlobClient()
            .upload(
                new ByteArrayInputStream(blobContent),
                blobContent.length
            );
    }
}
