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

@Service
public class IncompleteEnvelopesService {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final EnvelopeRepository envelopeRepository;
    private static final Logger log = LoggerFactory.getLogger(IncompleteEnvelopesService.class);

    public IncompleteEnvelopesService(EnvelopeRepository envelopeRepository) {
        this.envelopeRepository = envelopeRepository;
    }

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

    private static String toLocalTimeZone(Instant instant) {
        return dateTimeFormatter.format(ZonedDateTime.ofInstant(instant, EUROPE_LONDON_ZONE_ID));
    }
}
