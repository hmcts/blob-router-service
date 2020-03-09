package uk.gov.hmcts.reform.blobrouter.controllers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.events.EventRecord;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeEventResponse;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeInfo;
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
    public EnvelopeInfo findEnvelope(
        @RequestParam("file_name") String fileName,
        @RequestParam("container") String container
    ) {
        return envelopeService
            .getEnvelopeInfo(fileName, container)
            .map(tuple -> toResponse(tuple.getT1(), tuple.getT2()))
            .orElseThrow(EnvelopeNotFoundException::new);
    }

    private EnvelopeInfo toResponse(Envelope dbEnvelope, List<EventRecord> dbEventRecords) {
        return new EnvelopeInfo(
            dbEnvelope.id,
            dbEnvelope.container,
            dbEnvelope.fileName,
            dbEnvelope.createdAt,
            dbEnvelope.fileCreatedAt,
            dbEnvelope.dispatchedAt,
            dbEnvelope.status,
            dbEnvelope.isDeleted,
            dbEventRecords
                .stream()
                .map(e -> new EnvelopeEventResponse(e.id, e.createdAt, e.event.name(), e.notes))
                .collect(toList())
        );
    }
}
