package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.TeamEnvelope;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;
import uk.gov.hmcts.reform.blobrouter.services.storage.LeaseAcquirer;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class TeamContainerProcessor {

    private static final Logger logger = getLogger(TeamContainerProcessor.class);

    private final BlobServiceClient storageClient;
    private final LeaseAcquirer leaseAcquirer;
    private final BlobVerifier blobVerifier;

    public TeamContainerProcessor(
        BlobServiceClient storageClient,
        LeaseAcquirer leaseAcquirer,
        BlobVerifier blobVerifier
    ) {
        this.storageClient = storageClient;
        this.leaseAcquirer = leaseAcquirer;
        this.blobVerifier = blobVerifier;
    }

    public List<TeamEnvelope> leaseAndGetEnvelopes(String containerName) {
        logger.info("Processing container {}", containerName);
        List<TeamEnvelope> envelopes;
        try {
            BlobContainerClient containerClient = storageClient.getBlobContainerClient(containerName);

            OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1);
            BlobContainerSasPermission permission = new BlobContainerSasPermission().setReadPermission(true);
            BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiryTime, permission)
                .setStartTime(OffsetDateTime.now());

            var sasToken = containerClient.generateSas(values);

            var itr = containerClient
                .listBlobs();

            envelopes = itr
                .stream()
                .filter(blobItem -> leaseForThisRequest(containerClient, blobItem))
                .map(blobItem -> createTeamEnvelope(containerClient, blobItem, sasToken))
                .collect(Collectors.toList());

            logger.info("Finished processing container {}", containerName);
        } catch (Exception exception) {
            envelopes = Collections.emptyList();
            logger.error("Error occurred while processing {} container", containerName, exception);
        }

        logger.info("Envelopes found:");
        envelopes.forEach(e -> logger.info(e.fileName));

        return envelopes;
    }

    private TeamEnvelope createTeamEnvelope(BlobContainerClient containerClient, BlobItem blobItem, String sasToken) {

        BlobClient blobClient = containerClient.getBlobClient(blobItem.getName());

        var props = blobItem.getProperties();
        return new TeamEnvelope(props.getETag(),
                                blobItem.getName(),
                                blobClient.getBlobUrl() + "?" + sasToken,
                                props.getCreationTime().toInstant(),
                                props.getContentLength(),
                                props.getContentType());
    }

    private boolean leaseForThisRequest(BlobContainerClient containerClient, BlobItem blobItem) {

        BlobClient blobClient = containerClient.getBlobClient(blobItem.getName());

        var verificationResult = blobVerifier.verifyZip(blobClient.getBlobName(), blobClient.openInputStream());
        if (!verificationResult.isOk) {
            logger.error(
                "Rejected Blob. File name: {}, Container: {}, Reason: {}",
                blobClient.getBlobName(),
                containerClient.getBlobContainerName(),
                verificationResult.errorDescription
            );
            // @todo we don't care for the sake of the prototype
            // return false;
        }

        leaseAcquirer.ifAcquiredOrElse(
            blobClient,
            () -> {},
            errorCode -> logger.info(
                "Cannot acquire a lease for blob - skipping. File name: {}, container: {}, error code: {}",
                blobClient.getBlobName(),
                blobClient.getContainerName(),
                errorCode
            ),
            false
        );
        return true;
    }
}
