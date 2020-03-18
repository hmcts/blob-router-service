package uk.gov.hmcts.reform.blobrouter.services.storage;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.DuplicateFinder;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class DuplicateFileHandler {

    private static final Logger logger = getLogger(DuplicateFileHandler.class);

    private final EnvelopeService envelopeService;
    private final BlobMover blobMover;
    private final DuplicateFinder duplicateFinder;
    private final ServiceConfiguration serviceConfiguration;

    // region contructor
    public DuplicateFileHandler(
        EnvelopeService envelopeService,
        BlobMover blobMover,
        DuplicateFinder duplicateFinder,
        ServiceConfiguration serviceConfiguration
    ) {
        this.envelopeService = envelopeService;
        this.blobMover = blobMover;
        this.duplicateFinder = duplicateFinder;
        this.serviceConfiguration = serviceConfiguration;
    }
    // endregion

    public void handle() {
        serviceConfiguration
            .getEnabledSourceContainers()
            .forEach(container -> {
                duplicateFinder
                    .findIn(container)
                    .forEach(duplicate -> {
                        try {
                            logger.info(
                                "Moving duplicate file to rejected container. Container: {}, File name: {}",
                                container,
                                duplicate.fileName
                            );
                            var id = envelopeService.createNewEnvelope(
                                duplicate.container,
                                duplicate.fileName,
                                duplicate.blobCreatedAt
                            );

                            envelopeService.markAsRejected(id, "Duplicate");
                            blobMover.moveToRejectedContainer(duplicate.fileName, container);

                        } catch (Exception exc) {
                            logger.error(
                                "Error moving duplicate file. Container: {}. File name: {}",
                                container,
                                duplicate.fileName,
                                exc
                            );
                        }
                    });
            });
    }
}
