package uk.gov.hmcts.reform.blobrouter.services.storage;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.DuplicateFinder;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The `DuplicateFileHandler` class in Java handles duplicate files by finding them in source
 * containers and moving them to a rejected container while logging relevant information.
 */
@Component
public class DuplicateFileHandler {
    public static final String EVENT_MESSAGE = "Duplicate envelope";

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

    /**
     * The `handle` function iterates through enabled source containers, finds duplicates in
     * each container, and moves them to a rejected container.
     */
    public void handle() {
        serviceConfiguration
            .getEnabledSourceContainers()
            .forEach(container -> duplicateFinder
                .findIn(container)
                .forEach(duplicate -> {
                    moveToRejectedContainer(container, duplicate);
                }));
    }

    /**
     * The `moveToRejectedContainer` function moves a duplicate file to a rejected container
     * and logs relevant information.
     *
     * @param container The `container` parameter in the `moveToRejectedContainer` method represents
     *                  the destination container where the duplicate file will be moved to. It is the
     *                  location within the system where rejected files are stored.
     * @param duplicate The `duplicate` parameter in the `moveToRejectedContainer` method represents an
     *                  instance of the `Duplicate` class from the `DuplicateFinder` class. It contains
     *                  information about a duplicate file, such as the container it belongs to, file name,
     *                  blob creation timestamp, and file size.
     */
    private void moveToRejectedContainer(String container, DuplicateFinder.Duplicate duplicate) {
        try {
            logger.info(
                "Moving duplicate file to rejected container. Container: {}, File name: {}",
                    container,
                duplicate.fileName
            );
            var id = envelopeService.createNewEnvelope(
                duplicate.container,
                duplicate.fileName,
                duplicate.blobCreatedAt,
                duplicate.fileSize
            );

            envelopeService.markAsRejected(id, ErrorCode.ERR_ZIP_PROCESSING_FAILED, EVENT_MESSAGE);
            blobMover.moveToRejectedContainer(duplicate.fileName, container);

        } catch (Exception exc) {
            logger.error(
                "Error moving duplicate file. Container: {}. File name: {}",
                    container,
                duplicate.fileName,
                exc
            );
        }
    }
}
