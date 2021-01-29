package uk.gov.hmcts.reform.blobrouter.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.model.out.BlobInfo;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.blobrouter.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@Service
public class IncompleteEnvelopesService {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final EnvelopeRepository envelopeRepository;

    public IncompleteEnvelopesService(EnvelopeRepository envelopeRepository) {
        this.envelopeRepository = envelopeRepository;
    }

    public List<BlobInfo> getIncompleteEnvelopes(int staleTimeHr) {
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
}
