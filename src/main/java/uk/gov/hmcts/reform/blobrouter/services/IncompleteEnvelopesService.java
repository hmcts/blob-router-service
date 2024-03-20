package uk.gov.hmcts.reform.blobrouter.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.model.out.IncompleteEnvelopeInfo;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

/**
 * The `IncompleteEnvelopesService` class in Java provides methods to retrieve and delete incomplete envelopes based on
 * specified stale time criteria.
 */
@Service
public class IncompleteEnvelopesService {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final EnvelopeRepository envelopeRepository;
    private static final Logger log = LoggerFactory.getLogger(IncompleteEnvelopesService.class);

    public IncompleteEnvelopesService(EnvelopeRepository envelopeRepository) {
        this.envelopeRepository = envelopeRepository;
    }

    /**
     * This Java function retrieves incomplete envelopes that were created before a specified stale time in
     * hours and maps them to IncompleteEnvelopeInfo objects.
     *
     * @param staleTimeHr The `staleTimeHr` parameter represents the number of hours before the current
     *                    time that is considered as stale. This method retrieves a list of
     *                    incomplete envelopes that were created before a certain time threshold
     *                    based on the `staleTimeHr` parameter.
     * @return A list of `IncompleteEnvelopeInfo` objects is being returned.
     */
    public List<IncompleteEnvelopeInfo> getIncompleteEnvelopes(int staleTimeHr) {
        return envelopeRepository
            .getIncompleteEnvelopesBefore(now().minusHours(staleTimeHr))
            .stream()
            .map(envelope -> new IncompleteEnvelopeInfo(
                     envelope.container,
                     envelope.fileName,
                     envelope.id,
                     toLocalTimeZone(envelope.createdAt)
                 )
            )
            .collect(toList());
    }

    /**
     * This Java function deletes incomplete envelopes based on a specified stale time and a list of envelope IDs to
     * remove.
     *
     * @param staleTimeHr The `staleTimeHr` parameter represents the time threshold in hours before which
     *                    envelopes are considered stale and should be deleted. This
     *                    method `deleteIncompleteEnvelopes` takes this parameter along with a list of
     *                    envelope IDs (`envelopesToRemove`) that need to be deleted if they are considered.
     * @param envelopesToRemove The `envelopesToRemove` parameter is a list of strings containing the
     *                          UUIDs of envelopes that need to be removed. These UUIDs are converted to
     *                          `UUID` objects using the `UUID::fromString` method before being
     *                          processed further in the method.
     * @return The method `deleteIncompleteEnvelopes` returns an integer value representing the number
     *      of envelopes that have been deleted.
     */
    public int deleteIncompleteEnvelopes(int staleTimeHr, List<String> envelopesToRemove) {
        List<UUID> envelopeIds = envelopesToRemove.stream()
            .map(UUID::fromString)
            .toList();

        if (!envelopeIds.isEmpty()) {
            int numberOfEnvelopesDeleted = envelopeRepository.deleteEnvelopesBefore(
                now().minusHours(staleTimeHr),
                envelopeIds
            );
            log.info("{} rows have been deleted: {}", numberOfEnvelopesDeleted, envelopesToRemove);
            return numberOfEnvelopesDeleted;
        } else {
            log.info("No stale envelopes found within criteria.");
            return 0;
        }
    }

    /**
     * The function `toLocalTimeZone` converts an `Instant` to a string representation in the Europe/London time zone.
     *
     * @param instant The `instant` parameter is an object representing a point in time in the UTC time zone.
     * @return The method `toLocalTimeZone` returns a string representation of the provided `Instant`
     *      object converted to the Europe/London time zone using the `dateTimeFormatter`.
     */
    private static String toLocalTimeZone(Instant instant) {
        return dateTimeFormatter.format(ZonedDateTime.ofInstant(instant, EUROPE_LONDON_ZONE_ID));
    }
}
