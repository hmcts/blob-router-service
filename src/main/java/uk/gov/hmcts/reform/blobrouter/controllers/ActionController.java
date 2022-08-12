package uk.gov.hmcts.reform.blobrouter.controllers;

import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeActionService;

import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.blobrouter.util.AuthValidator.validateAuthorization;

@RestController
@RequestMapping(path = "/actions")
public class ActionController {
    private static final Logger logger = getLogger(ActionController.class);

    private final EnvelopeActionService envelopeActionService;

    private final String apiKey;

    public ActionController(
        EnvelopeActionService envelopeActionService,
        @Value("${actions.api-key}") String apiKey
    ) {
        this.envelopeActionService = envelopeActionService;
        this.apiKey = apiKey;
    }

    @PutMapping(path = "/complete/{id}")
    @Operation(summary = "Reprocess envelope by ID")
    public ResponseEntity<Void> complete(
        @RequestHeader(value = AUTHORIZATION, required = false) String authHeader,
        @PathVariable UUID id
    ) {
        validateAuthorization(authHeader, apiKey);

        envelopeActionService.completeStaleEnvelope(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
