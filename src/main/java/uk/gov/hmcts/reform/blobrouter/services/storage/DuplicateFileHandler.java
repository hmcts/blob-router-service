package uk.gov.hmcts.reform.blobrouter.services.storage;

import org.slf4j.Logger;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.DuplicateFinder;

import static org.slf4j.LoggerFactory.getLogger;

public class DuplicateFileHandler {

    private static final Logger logger = getLogger(DuplicateFileHandler.class);

    private final EnvelopeService envelopeService;
    private final BlobMover blobMover;
    private final DuplicateFinder duplicateFinder;
    private final ServiceConfiguration serviceConfiguration;


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

    public void handle() {
        serviceConfiguration
            .getEnabledSourceContainers()
            .forEach(container -> {
                duplicateFinder
                    .findIn(container)
                    .forEach(blobItem -> blobMover.moveToRejectedContainer(blobItem.getName(), container));
            });
    }
}
