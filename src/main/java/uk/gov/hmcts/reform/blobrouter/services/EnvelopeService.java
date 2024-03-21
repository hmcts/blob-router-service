package uk.gov.hmcts.reform.blobrouter.services;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;
import uk.gov.hmcts.reform.blobrouter.data.events.NewEnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidRequestParametersException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * The `EnvelopeService` class in Java provides methods for managing envelopes, including creating, updating, marking as
 * dispatched or rejected, and retrieving envelope information.
 */
@Service
public class EnvelopeService {

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeEventRepository eventRepository;

    public EnvelopeService(
        EnvelopeRepository envelopeRepository,
        EnvelopeEventRepository eventRepository
    ) {
        this.envelopeRepository = envelopeRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * This function retrieves the last envelope associated with a specific blob name and container name in a read-only
     * transaction.
     *
     * @param blobName BlobName is a string parameter that represents the name of a blob in a storage container.
     * @param containerName The `containerName` parameter refers to the name of the
     *                      container where the envelope is stored.  It is used to specify the location
     *                      or directory within which the envelope with the given `blobName` is stored.
     * @return An Optional containing the last Envelope found with the specified blobName and containerName is being
     *      returned.
     */
    @Transactional(readOnly = true)
    public Optional<Envelope> findLastEnvelope(String blobName, String containerName) {
        return envelopeRepository.findLast(blobName, containerName);
    }

    /**
     * This function retrieves an envelope that is not in the "created" status based on the provided file name and
     * container name.
     *
     * @param fileName The `fileName` parameter is a string that represents the name of a file.
     * @param containerName The `containerName` parameter is a String that represents the name of
     *                      the container where the envelope is stored.
     * @return An Optional object containing an Envelope entity that is not in the "Created" status,
     *      based on the provided file name and container name.
     */
    @Transactional(readOnly = true)
    public Optional<Envelope> findEnvelopeNotInCreatedStatus(String fileName, String containerName) {
        return envelopeRepository.findEnvelopeNotInCreatedStatus(fileName, containerName);
    }

    /**
     * This Java function finds and returns an Envelope entity by its UUID identifier in a read-only transaction.
     *
     * @param id The `id` parameter is a unique identifier of type `UUID`
     *           used to search for a specific `Envelope` entity in the database.
     * @return An Optional containing an Envelope object is being returned.
     */
    @Transactional(readOnly = true)
    public Optional<Envelope> findEnvelope(UUID id) {
        return envelopeRepository.find(id);
    }

    /**
     * The `createNewEnvelope` method creates a new envelope with specified details and triggers an event for file
     * processing start.
     *
     * @param containerName The `containerName` parameter represents the name of the container where the blob is stored.
     * @param blobName The `blobName` parameter in the `createNewEnvelope` method represents the name of the
     *                 blob being stored in the envelope. It is a string value that identifies the specific
     *                 blob within the container.
     * @param blobCreationDate The `blobCreationDate` parameter in the `createNewEnvelope` method represents
     *                        the date and time when the blob was created. It is of type
     *                         `Instant`, which is a class in Java that represents a point in time.
     *                         You can create an `Instant` object using methods like `Instant.now`.
     * @param fileSize The `fileSize` parameter in the `createNewEnvelope` method represents the size of
     *                 the file in bytes that is being stored in the envelope. It is used to specify the
     *                 size of the file being processed.
     * @return The method `createNewEnvelope` is returning a `UUID` value.
     */
    @Transactional
    public UUID createNewEnvelope(String containerName, String blobName, Instant blobCreationDate, Long fileSize) {
        UUID id = envelopeRepository
            .insert(
                new NewEnvelope(containerName, blobName, blobCreationDate, null, Status.CREATED, fileSize)
            );

        eventRepository.insert(new NewEnvelopeEvent(id, EventType.FILE_PROCESSING_STARTED, null, null));

        return id;
    }

    @Transactional(readOnly = true)
    public List<Envelope> getReadyToDeleteRejections() {
        return envelopeRepository.find(Status.REJECTED, false);
    }

    /**
     * This function retrieves a list of envelopes with a status of DISPATCHED that are ready to
     * be deleted for a specific container.
     *
     * @param containerName The `containerName` parameter is a String that represents the name of
     *                      the container for which you want to retrieve the list of ready-to-delete
     *                      dispatches.
     * @return A list of Envelope objects that are ready to be deleted from the database.
     */
    @Transactional(readOnly = true)
    public List<Envelope> getReadyToDeleteDispatches(String containerName) {
        return envelopeRepository.find(Status.DISPATCHED, containerName, false);
    }

    /**
     * The `markAsDispatched` method updates the status of an envelope to DISPATCHED, sets the dispatch date time, and
     * inserts a new event into the event repository, handling the case where the envelope is not found.
     *
     * @param id The `id` parameter in the `markAsDispatched` method is a unique identifier of type
     *          `UUID` that is used to find and update an envelope in the system.
     */
    @Transactional
    public void markAsDispatched(UUID id) {
        envelopeRepository
            .find(id)
            .ifPresentOrElse(
                env -> {
                    envelopeRepository.updateStatus(id, Status.DISPATCHED);
                    envelopeRepository.updateDispatchDateTime(id, now());
                    eventRepository.insert(new NewEnvelopeEvent(id, EventType.DISPATCHED, null, null));
                },
                () -> {
                    throw new EnvelopeNotFoundException("Envelope with ID: " + id + " not found");
                }
            );
    }

    /**
     * The `markAsRejected` method marks an envelope as rejected, updates its status,
     * sets pending notification, and logs a rejection event.
     *
     * @param id The `id` parameter is a unique identifier (UUID) for the envelope that needs to be
     *           marked as rejected.
     * @param errorCode The `errorCode` parameter in the `markAsRejected` method is used to specify the
     *                  error code associated with the rejection of the envelope. It is of type `ErrorCode`
     *                  and is passed as an argument when calling the method. The error code provides
     *                  additional information about the reason for the rejection.
     * @param reason The `reason` parameter in the `markAsRejected` method is a string that represents the
     *               reason for rejecting the envelope with the specified ID. This reason could provide
     *               additional context or explanation for why the envelope was rejected.
     */
    @Transactional
    public void markAsRejected(UUID id, ErrorCode errorCode, String reason) {
        envelopeRepository
            .find(id)
            .ifPresentOrElse(
                env -> {
                    envelopeRepository.updateStatus(id, Status.REJECTED);
                    envelopeRepository.updatePendingNotification(id, true); // notification pending
                    eventRepository.insert(new NewEnvelopeEvent(id, EventType.REJECTED, errorCode, reason));
                },
                () -> {
                    throw new EnvelopeNotFoundException("Envelope with ID: " + id + " not found");
                }
            );
    }

    /**
     * The function marks a pending notification as sent by updating the status in the envelope
     * repository and inserting a new event in the event repository.
     *
     * @param id The `id` parameter is a unique identifier (UUID) that is used to identify a specific
     *           notification or envelope that needs to be marked as sent.
     */
    @Transactional
    public void markPendingNotificationAsSent(UUID id) {
        envelopeRepository.updatePendingNotification(id, false);
        eventRepository.insert(new NewEnvelopeEvent(id, EventType.NOTIFICATION_SENT, null, null));
    }

    /**
     * This function marks an envelope as deleted in the database and inserts a new event related to the deletion.
     *
     * @param envelope The `envelope` parameter is an object of type `Envelope` which is being
     *                 passed to the `markEnvelopeAsDeleted` method.
     */
    @Transactional
    public void markEnvelopeAsDeleted(Envelope envelope) {
        envelopeRepository.markAsDeleted(envelope.id);
        eventRepository.insert(new NewEnvelopeEvent(envelope.id, EventType.DELETED, null, null));
    }

    /**
     * This Java function saves a new event related to an envelope with the specified details.
     *
     * @param envelopeId EnvelopeId is a unique identifier for the envelope associated with the event.
     * @param eventType The `eventType` parameter in the `saveEvent` method represents the type of
     *                  event being saved. It is an enum or a predefined constant that categorizes
     *                  the event being recorded.
     * @param notes The `notes` parameter in the `saveEvent` method is a string that represents any
     *              additional information or comments related to the event being saved. It is used to
     *              provide context or details about the event that is being recorded in the system.
     */
    @Transactional
    public void saveEvent(UUID envelopeId, EventType eventType, String notes) {
        eventRepository.insert(new NewEnvelopeEvent(envelopeId, eventType, null, notes));
    }

    /**
     * This function saves a new event related to a specific envelope in a transactional manner.
     *
     * @param envelopeId EnvelopeId is a unique identifier for an envelope. It is typically a
     *                   UUID (Universally Unique Identifier) that uniquely identifies a specific
     *                   envelope in the system.
     * @param eventType The `eventType` parameter in the `saveEvent` method represents the type of
     *                  event being saved for a specific envelope. It could be an enum or a string
     *                  indicating the type of event that occurred, such as
     *                  "CREATED", "UPDATED", "DELETED", etc.
     */
    @Transactional
    public void saveEvent(UUID envelopeId, EventType eventType) {
        eventRepository.insert(new NewEnvelopeEvent(envelopeId, eventType, null, null));
    }

    /**
     * The function `getEnvelopes` retrieves envelopes and their associated events based on specified parameters.
     *
     * @param blobName The `blobName` parameter in the `getEnvelopes` method represents the name of the blob
     *                 associated with the envelopes you are trying to retrieve. It is used as a filter
     *                 criteria to search for envelopes in the repository.
     * @param containerName The `containerName` parameter in the `getEnvelopes` method refers to the name
     *                      of the container where the envelopes are stored. This parameter is used to
     *                      filter the envelopes based on the specified container name when retrieving
     *                      them from the repository.
     * @param date The `date` parameter in the `getEnvelopes` method is used to filter envelopes based
     *             on a specific date. If a date is provided, only envelopes that match the given date
     *             will be retrieved.
     * @return A list of Tuple2 objects, where each Tuple2 contains an Envelope object and a list of
     *      EnvelopeEvent objects associated with that Envelope.
     */
    @Transactional(readOnly = true)
    public List<Tuple2<Envelope, List<EnvelopeEvent>>> getEnvelopes(
        String blobName,
        String containerName,
        LocalDate date
    ) {
        if (StringUtils.isEmpty(blobName) && date == null) {
            throw new InvalidRequestParametersException("'file_name' or 'date' must not be null or empty");
        }

        List<Envelope> envelopes = envelopeRepository
            .findEnvelopes(blobName, containerName, date);

        if (envelopes.isEmpty()) {
            return emptyList();
        } else {
            List<UUID> envelopeIds = envelopes.stream().map(e -> e.id).collect(toList());

            List<EnvelopeEvent> envelopeEvents = eventRepository.findForEnvelopes(envelopeIds);

            Map<UUID, List<EnvelopeEvent>> eventsByEnvelopeIds = envelopeEvents
                .stream()
                .collect(groupingBy(envelopeEvent -> envelopeEvent.envelopeId));

            return envelopes
                .stream()
                .map(envelope -> Tuples.of(
                    envelope,
                    getEnvelopeEvents(eventsByEnvelopeIds, envelope)
                ))
                .collect(toList());
        }
    }

    /**
     * This Java function retrieves a list of Envelope objects for a specific date from a repository in a read-only
     * transaction.
     *
     * @param date The `date` parameter in the `getEnvelopes` method represents the specific date for which
     *             you want to retrieve envelopes. The method retrieves a list of envelopes from the database
     *             based on this date.
     * @return A list of Envelope objects is being returned.
     */
    @Transactional(readOnly = true)
    public List<Envelope> getEnvelopes(LocalDate date) {
        var envelopes = envelopeRepository.findEnvelopes(null, null, date);
        return envelopes.isEmpty() ? emptyList() : ImmutableList.copyOf(envelopes);
    }

    /**
     * This function retrieves a list of Envelope objects by a given DCN prefix within a specified date range.
     *
     * @param dcnPrefix The `dcnPrefix` parameter is a string that represents a prefix
     *                  used to filter envelopes based on their Document Control Number (DCN).
     * @param fromDate The `fromDate` parameter represents the starting date from which you want to
     *                 search for envelopes. It is used in the `getEnvelopesByDcnPrefix` method to filter
     *                 envelopes based on their creation date.
     * @param toDate The `toDate` parameter in the `getEnvelopesByDcnPrefix` method represents the end date
     *               range for filtering envelopes based on their creation date. Envelopes created on
     *               or before this `toDate` value will be included in the result set.
     * @return A list of Envelope objects that match the given DCN prefix and fall within the specified
     *      date range is being returned. If no envelopes are found, an empty list is returned.
     */
    @Transactional(readOnly = true)
    public List<Envelope> getEnvelopesByDcnPrefix(String dcnPrefix, LocalDate fromDate, LocalDate toDate) {
        var envelopes = envelopeRepository.findEnvelopesByDcnPrefix(dcnPrefix, fromDate, toDate);
        return envelopes.isEmpty() ? emptyList() : ImmutableList.copyOf(envelopes);
    }

    /**
     * This function retrieves a list of EnvelopeEvents associated with a specific Envelope from a map of EnvelopeEvent
     * lists by Envelope IDs.
     *
     * @param eventsByEnvelopeIds The `eventsByEnvelopeIds` parameter is a `Map` that maps `UUID` keys to
     *                            lists of `EnvelopeEvent` objects. It is used to store a collection of
     *                            `EnvelopeEvent` objects associated with each `UUID` key.
     * @param envelope The `getEnvelopeEvents` method takes in a `Map` called `eventsByEnvelopeIds` which
     *                 maps `UUID` keys to lists of `EnvelopeEvent` values, and an `Envelope` object
     *                 called `envelope`.
     * @return The method `getEnvelopeEvents` returns a list of `EnvelopeEvent` objects associated with the
     *      given `Envelope` object from the provided `eventsByEnvelopeIds` map. If there are no events
     *      associated with the envelope ID, an empty list is returned.
     */
    private List<EnvelopeEvent> getEnvelopeEvents(
        Map<UUID, List<EnvelopeEvent>> eventsByEnvelopeIds,
        Envelope envelope
    ) {
        List<EnvelopeEvent> envelopeEvents = eventsByEnvelopeIds.get(envelope.id);
        return envelopeEvents == null ? emptyList() : envelopeEvents;
    }
}
