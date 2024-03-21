package uk.gov.hmcts.reform.blobrouter.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.clients.response.ZipFileResponse;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.events.NewEnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeCompletedOrNotStaleException;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;

import java.time.Instant;
import java.util.UUID;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Comparator.naturalOrder;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.REJECTED;
import static uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode.ERR_STALE_ENVELOPE;
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.DELETED;
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.MANUALLY_MARKED_AS_DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.events.EventType.MANUALLY_MARKED_AS_REJECTED;

/**
 * The `EnvelopeActionService` class in Java provides methods to process stale envelopes by checking their state,
 * retrieving zip files, and moving them to rejected or dispatched states based on the response.
 */
@Service
public class EnvelopeActionService {

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeEventRepository envelopeEventRepository;
    private final BulkScanProcessorClient bulkScanProcessorClient;
    private final long envelopeStaleTimeoutHr;

    public EnvelopeActionService(
        EnvelopeRepository envelopeRepository,
        EnvelopeEventRepository envelopeEventRepository,
        BulkScanProcessorClient bulkScanProcessorClient,
        @Value("${envelope-stale-timeout-hr}") long envelopeStaleTimeoutHr
    ) {
        this.envelopeRepository = envelopeRepository;
        this.envelopeEventRepository = envelopeEventRepository;
        this.bulkScanProcessorClient = bulkScanProcessorClient;
        this.envelopeStaleTimeoutHr = envelopeStaleTimeoutHr;
    }

    /**
     * The `completeStaleEnvelope` method processes a stale envelope by checking its state, retrieving a zip file, and
     * moving the envelope to either rejected or dispatched based on the zip file response.
     *
     * @param envelopeId The `completeStaleEnvelope` method takes a parameter
     *                   `envelopeId` of type `UUID`. This method is responsible for completing the
     *                   processing of a stale envelope by checking its state, retrieving a zip file using a
     *                   bulk scan processor client, and then moving the envelope to either a rejected or
     *                   dispatched state.
     */
    @Transactional
    public void completeStaleEnvelope(UUID envelopeId) {
        Envelope envelope = envelopeRepository.find(envelopeId)
            .orElseThrow(
                () -> new EnvelopeNotFoundException("Envelope with id " + envelopeId + " not found")
            );
        validateEnvelopeState(envelope);

        ZipFileResponse response = bulkScanProcessorClient.getZipFile(envelope.fileName);
        if (isZipFileUnknownToProcessor(response)) {
            moveEnvelopeToRejected(envelopeId);
        } else {
            moveEnvelopeToDispatched(envelopeId);
        }
    }

    /**
     * This function moves an envelope to a rejected state with a specific reason.
     *
     * @param envelopeId The `envelopeId` parameter is a unique identifier for the envelope
     *                   that needs to be moved to the rejected state.
     */
    private void moveEnvelopeToRejected(UUID envelopeId) {
        moveEnvelopeToFinalState(
            envelopeId,
            REJECTED,
            MANUALLY_MARKED_AS_REJECTED,
            "Manually marked as rejected due to stale state"
        );
    }

    /**
     * The function `moveEnvelopeToDispatched` moves an envelope to the dispatched state and marks it as manually
     * dispatched due to a stale state.
     *
     * @param envelopeId A unique identifier for the envelope that needs to be moved to the dispatched state.
     */
    private void moveEnvelopeToDispatched(UUID envelopeId) {
        moveEnvelopeToFinalState(
            envelopeId,
            DISPATCHED,
            MANUALLY_MARKED_AS_DISPATCHED,
            "Manually marked as dispatched due to stale state"
        );
    }

    /**
     * The `moveEnvelopeToFinalState` function updates the status of an envelope,
     * creates events for the status change and deletion, and marks the envelope as deleted.
     *
     * @param envelopeId The `envelopeId` parameter is a unique identifier for the envelope that needs
     *                   to be processed.
     * @param status The `status` parameter in the `moveEnvelopeToFinalState` method represents the final
     *               status that the envelope will be updated to. It is used to update the status of the
     *               envelope in the `envelopeRepository`.
     * @param eventType The `eventType` parameter in the `moveEnvelopeToFinalState` method represents the
     *                  type of event that will be created for the given envelope. It is used to specify
     *                  the type of event that occurred during the transition of the envelope to its final state.
     * @param message The `message` parameter in the `moveEnvelopeToFinalState` method is a string that provides
     *                additional information or context related to the event being processed. It is used to provide
     *                details or reasons for the status change or event creation within the method.
     */
    private void moveEnvelopeToFinalState(
        UUID envelopeId,
        Status status,
        EventType eventType,
        String message
    ) {
        envelopeRepository.updateStatus(envelopeId, status);
        createEvent(
            envelopeId,
            eventType,
            message
        );
        envelopeRepository.markAsDeleted(envelopeId);
        createEvent(
            envelopeId,
            DELETED,
            "Manually marked as deleted due to stale state"
        );
    }

    /**
     * The function checks if a ZipFileResponse object has empty envelopes and events lists.
     *
     * @param response The `response` parameter is of type `ZipFileResponse`, which likely
     *                 contains information about a zip file, such as envelopes and events.
     *                 The method `isZipFileUnknownToProcessor` checks if the `envelopes` and `events`
     *                 lists within the `response` are empty.
     * @return The method is returning a boolean value, which is determined by whether the `envelopes`
     *      and `events` lists in the `ZipFileResponse` object are empty. If both lists are empty,
     *      the method will return `true`, indicating that the zip file is unknown to the processor.
     */
    private boolean isZipFileUnknownToProcessor(ZipFileResponse response) {
        return response.envelopes.isEmpty() && response.events.isEmpty();
    }

    /**
     * The `createEvent` function creates a new `NewEnvelopeEvent` object with the given
     * parameters and inserts it into the `envelopeEventRepository`.
     *
     * @param envelopeId A unique identifier for the envelope.
     * @param eventType The `eventType` parameter in the `createEvent` method represents the type of event
     *                  being created. It is an enum or a predefined constant that specifies the
     *                  category or classification of the event. Examples of event types could be
     *                  `CREATED`, `UPDATED`, `DELETED`, `ERROR`, etc.
     * @param message The `message` parameter in the `createEvent` method is a string that contains
     *                information or details related to the event being created. It is used to provide
     *                additional context or description about the event that is being recorded in the system.
     */
    private void createEvent(
        UUID envelopeId,
        EventType eventType,
        String message
    ) {
        NewEnvelopeEvent event = new NewEnvelopeEvent(
            envelopeId,
            eventType,
            ERR_STALE_ENVELOPE,
            message
        );
        envelopeEventRepository.insert(event);
    }

    /**
     * The function `validateEnvelopeState` checks if an envelope is dispatched, rejected, or not stale, and throws an
     * exception if it is completed or not stale.
     *
     * @param envelope The `validateEnvelopeState` method takes an `Envelope` object as a
     *                 parameter. The method checks the status of the envelope and whether it is stale.
     *                 If the envelope status is `DISPATCHED` or `REJECTED`, or if the envelope is not
     *                 stale, it throws an `EnvelopeCompletedOrNotStaleException` exception.
     */
    private void validateEnvelopeState(Envelope envelope) {
        if (envelope.status == DISPATCHED || envelope.status == REJECTED
            || !isStale(envelope)) {
            throw new EnvelopeCompletedOrNotStaleException(
                "Envelope with id " + envelope.id + " is completed or not stale"
            );
        }
    }

    /**
     * The function `isStale` checks if an envelope is stale based on its status and the timestamp of its last event.
     *
     * @param envelope An object representing an envelope with a status and an ID.
     * @return The method `isStale` returns a boolean value indicating whether the given `Envelope` is considered stale
     *      based on certain conditions.
     */
    private boolean isStale(Envelope envelope) {
        if (envelope.status != Status.CREATED) {
            return false;
        }

        Instant lastEventTimeStamp = envelopeEventRepository
            .findForEnvelope(envelope.id)
            .stream()
            .map(event -> event.createdAt)
            .max(naturalOrder())
            .orElseThrow(); // no events for the envelope is normally impossible
        return between(lastEventTimeStamp, now()).toHours() > envelopeStaleTimeoutHr;
    }
}
