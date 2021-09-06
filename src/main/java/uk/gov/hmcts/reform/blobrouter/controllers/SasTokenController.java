package uk.gov.hmcts.reform.blobrouter.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.model.out.SasTokenResponse;
import uk.gov.hmcts.reform.blobrouter.services.SasTokenGeneratorService;

@RestController
@RequestMapping(path = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
public class SasTokenController {

    private static final String SEPARATORS = "[\n|\r|\t]";

    private final SasTokenGeneratorService tokenGeneratorService;

    public SasTokenController(SasTokenGeneratorService tokenGeneratorService) {
        this.tokenGeneratorService = tokenGeneratorService;
    }

    @GetMapping(path = "/{serviceName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SasTokenResponse> getSasToken(@PathVariable String serviceName) {
        String sasToken = tokenGeneratorService.generateSasToken(serviceName.replaceAll(SEPARATORS, "_"));

        return ResponseEntity.ok(new SasTokenResponse(sasToken));
    }
}
