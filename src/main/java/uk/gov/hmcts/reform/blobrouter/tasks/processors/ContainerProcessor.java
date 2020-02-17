package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.EventRecordRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEventRecord;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;

import java.time.Instant;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.FILE_PROCESSING_STARTED;

@Component
public class ContainerProcessor {

    private static final Logger logger = getLogger(ContainerProcessor.class);

    private final BlobServiceClient storageClient;
    private final BlobProcessor blobProcessor;
    private final BlobReadinessChecker blobReadinessChecker;
    private final EventRecordRepository eventRecordRepository;

    public ContainerProcessor(
        BlobServiceClient storageClient,
        BlobProcessor blobProcessor,
        BlobReadinessChecker blobReadinessChecker,
        EventRecordRepository eventRecordRepository
    ) {
        this.storageClient = storageClient;
        this.blobProcessor = blobProcessor;
        this.blobReadinessChecker = blobReadinessChecker;
        this.eventRecordRepository = eventRecordRepository;
    }

    public void process(String containerName) {
        logger.info("Processing container {}", containerName);

        try {
            BlobContainerClient containerClient = storageClient.getBlobContainerClient(containerName);

            containerClient
                .listBlobs()
                .forEach(blob -> processIfReady(blob, containerName));

            logger.info("Finished processing container {}", containerName);
        } catch (Exception exception) {
            logger.error("Error occurred while processing {} container", containerName, exception);
        }
    }

    private void processIfReady(BlobItem blob, String containerName) {
        Instant blobCreationDate = blob.getProperties().getLastModified().toInstant();

        if (blobReadinessChecker.isReady(blobCreationDate)) {
            eventRecordRepository.insert(
                new NewEventRecord(containerName, blob.getName(), FILE_PROCESSING_STARTED)
            );
            blobProcessor.process(blob.getName(), containerName);
        } else {
            logger.info(
                "Blob not ready to be processed yet, skipping. File name: {}. Container: {}",
                blob.getName(),
                containerName
            );
        }
    }
}
