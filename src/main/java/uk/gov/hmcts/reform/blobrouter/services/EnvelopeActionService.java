package uk.gov.hmcts.reform.blobrouter.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEventRepository;
import uk.gov.hmcts.reform.blobrouter.model.out.BlobInfo;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@Service
public class EnvelopeActionService {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeEventRepository envelopeEventRepository;
    private final long creationTimeoutHr;

    public EnvelopeActionService(
        EnvelopeRepository envelopeRepository,
        EnvelopeEventRepository envelopeEventRepository,
        @Value("${creation-stale-timeout-hr}") long creationTimeoutHr
    ) {
        this.envelopeRepository = envelopeRepository;
        this.envelopeEventRepository = envelopeEventRepository;
        this.creationTimeoutHr = creationTimeoutHr;
    }

    public List<BlobInfo> rejectStaleEnvelope(UUID envelopeId, int staleTimeHr) {
        return envelopeRepository
            .getIncompleteEnvelopesBefore(now().minus(staleTimeHr, HOURS))
            .stream()
            .map(envelope -> new BlobInfo(
                     envelope.container,
                     envelope.fileName,
                     toLocalTimeZone(envelope.createdAt)
                 )
            )
            .collect(toList());
    }

    private static String toLocalTimeZone(Instant instant) {
        return dateTimeFormatter.format(ZonedDateTime.ofInstant(instant, EUROPE_LONDON_ZONE_ID));
    }


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
        return between(lastEventTimeStamp, now()).toHours() > creationTimeoutHr;
    }
}
