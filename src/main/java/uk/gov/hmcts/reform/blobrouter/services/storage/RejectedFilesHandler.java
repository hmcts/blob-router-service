package uk.gov.hmcts.reform.blobrouter.services.storage;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

import java.util.List;

import static java.util.stream.Collectors.groupingBy;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class RejectedFilesHandler {

    private static final Logger logger = getLogger(RejectedFilesHandler.class);

    public static final String REJECTED_CONTAINER_SUFFIX = "-rejected";

    private final EnvelopeService envelopeService;
    private final BlobMover blobMover;

    public RejectedFilesHandler(
        EnvelopeService envelopeService,
        BlobMover blobMover
    ) {
        this.envelopeService = envelopeService;
        this.blobMover = blobMover;
    }

    /**
     * Handles files that were rejected. Ie:
     * - moves files to container for rejected files
     * - removes files from the original container
     * - marks envelopes in the DB as deleted
     */
    public void handle() {
        List<Envelope> rejectedEnvelopes = envelopeService.getReadyToDeleteRejections();

        logger.info("Found {} rejected envelopes", rejectedEnvelopes.size());

        rejectedEnvelopes
            .stream()
            .collect(groupingBy(e -> e.container))
            .forEach((container, envelopes) -> {
                logger.info("Started moving rejected files from container {}", container);

                envelopes.forEach(envelope -> {
                    try {
                        blobMover.moveToRejectedContainer(envelope.fileName, container);
                        envelopeService.markEnvelopeAsDeleted(envelope.id);
                    } catch (Exception exc) {
                        logger.error(
                            "Error handling rejected file. File name: {}. container: {}",
                            envelope.fileName,
                            container,
                            exc
                        );
                    }
                });

                logger.info("Finished moving rejected files from container {}", container);
            });
    }
}
