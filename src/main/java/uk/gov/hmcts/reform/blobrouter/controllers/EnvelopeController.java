package uk.gov.hmcts.reform.blobrouter.controllers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepositoryImpl;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeInfo;

@RestController
@RequestMapping(path = "/envelopes", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnvelopeController {

    private final EnvelopeRepositoryImpl envelopesRepo;

    public EnvelopeController(EnvelopeRepositoryImpl envelopesRepo) {
        this.envelopesRepo = envelopesRepo;
    }

    @GetMapping()
    public EnvelopeInfo findEnvelope(
        @RequestParam("file_name") String fileName,
        @RequestParam("container") String container
    ) {
        return envelopesRepo
            .find(fileName, container)
            .map(this::toResponse)
            .orElseThrow(EnvelopeNotFoundException::new);
    }

    private EnvelopeInfo toResponse(Envelope dbEnvelope) {
        return new EnvelopeInfo(
            dbEnvelope.id,
            dbEnvelope.container,
            dbEnvelope.fileName,
            dbEnvelope.createdAt,
            dbEnvelope.fileCreatedAt,
            dbEnvelope.dispatchedAt,
            dbEnvelope.status,
            dbEnvelope.isDeleted
        );
    }
}
