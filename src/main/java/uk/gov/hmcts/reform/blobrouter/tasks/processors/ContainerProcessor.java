package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobServiceClient;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ContainerProcessor {

    private static final Logger logger = getLogger(ContainerProcessor.class);

    private final BlobServiceClient storageClient;
    private final BlobProcessor blobProcessor;
    private final BlobReadinessChecker blobReadinessChecker;
    private final LeaseAcquirer leaseAcquirer;
    private final EnvelopeService envelopeService;

    public ContainerProcessor(
        BlobServiceClient storageClient,
        BlobProcessor blobProcessor,
        BlobReadinessChecker blobReadinessChecker,
        LeaseAcquirer leaseAcquirer,
        EnvelopeService envelopeService
    ) {
        this.storageClient = storageClient;
        this.blobProcessor = blobProcessor;
        this.blobReadinessChecker = blobReadinessChecker;
        this.leaseAcquirer = leaseAcquirer;
        this.envelopeService = envelopeService;
    }

    public void process(String containerName) {
        logger.info("Processing container {}", containerName);
        logger.info("storageClient {}", storageClient);
        logger.info("blobProcessor {}", blobProcessor);
        logger.info("blobReadinessChecker {}", blobReadinessChecker);
        logger.info("leaseAcquirer {}", leaseAcquirer);
        logger.info("envelopeService {}", envelopeService);

        throw new InvalidZipArchiveException("test exception");
    }
}
