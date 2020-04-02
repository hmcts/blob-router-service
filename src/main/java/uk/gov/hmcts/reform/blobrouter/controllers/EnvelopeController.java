package uk.gov.hmcts.reform.blobrouter.controllers;

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

import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(path = "/envelopes", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnvelopeController {

    private final EnvelopeService envelopeService;

    public EnvelopeController(EnvelopeService envelopeService) {
        this.envelopeService = envelopeService;
    }

    @GetMapping()
    public SearchResult findEnvelopes(
        @RequestParam("file_name") String fileName,
        @RequestParam("container") String container
    ) {
        return new SearchResult(
            envelopeService
                .getEnvelopeInfo(fileName, container)
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
            dbEnvelope.createdAt,
            dbEnvelope.fileCreatedAt,
            dbEnvelope.dispatchedAt,
            dbEnvelope.status,
            dbEnvelope.isDeleted,
            dbEnvelope.pendingNotification,
            dbEventRecords
                .stream()
                .map(e -> new EnvelopeEventResponse(e.id, e.createdAt, e.type.name(), e.notes))
                .collect(toList())
        );
    }
}
