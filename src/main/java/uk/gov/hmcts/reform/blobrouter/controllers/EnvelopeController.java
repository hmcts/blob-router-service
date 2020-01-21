package uk.gov.hmcts.reform.blobrouter.controllers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.data.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.exceptions.EnvelopeNotFoundException;

@RestController
@RequestMapping(path = "/envelopes", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnvelopeController {

    private final EnvelopeRepository envelopesRepo;

    public EnvelopeController(EnvelopeRepository envelopesRepo) {
        this.envelopesRepo = envelopesRepo;
    }

    @GetMapping()
    public Envelope findEnvelope(
        @RequestParam("file_name") String fileName,
        @RequestParam("container") String container
    ) {
        return envelopesRepo
            .find(fileName, container)
            .orElseThrow(EnvelopeNotFoundException::new);
    }
}
