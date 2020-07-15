package uk.gov.hmcts.reform.blobrouter.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.events.EnvelopeEvent;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeEventResponse;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeInfo;
import uk.gov.hmcts.reform.blobrouter.model.out.SearchResult;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static uk.gov.hmcts.reform.blobrouter.util.TimeUtils.toLocalTimeZone;

@RestController
@RequestMapping(path = "/envelopes", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnvelopeController {

    private final EnvelopeService envelopeService;

    public EnvelopeController(EnvelopeService envelopeService) {
        this.envelopeService = envelopeService;
    }

    @GetMapping()
    public SearchResult findEnvelopes(
        @RequestParam(name = "file_name", required = false) String fileName,
        @RequestParam(name = "container", required = false) String container,
        @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DATE) LocalDate date
    ) {
        return new SearchResult(
            envelopeService
                .getEnvelopes(fileName, container, date)
                .stream()
                .map(tuple -> toResponse(tuple.getT1(), tuple.getT2()))
                .collect(toList())
        );

    }

    private EnvelopeInfo toResponse(Envelope dbEnvelope, List<EnvelopeEvent> dbEventRecords) {
        return new EnvelopeInfo(
            dbEnvelope.id,
            dbEnvelope.container,
            dbEnvelope.fileName,
            toLocalTimeZone(dbEnvelope.createdAt),
            toLocalTimeZone(dbEnvelope.fileCreatedAt),
            toLocalTimeZone(dbEnvelope.dispatchedAt),
            dbEnvelope.status,
            dbEnvelope.isDeleted,
            dbEnvelope.pendingNotification,
            dbEventRecords
                .stream()
                .map(
                    e -> new EnvelopeEventResponse(
                        e.id, toLocalTimeZone(e.createdAt), e.type.name(), e.notes
                    )
                )
                .collect(toList())
        );
    }

}
