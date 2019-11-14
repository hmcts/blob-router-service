package uk.gov.hmcts.reform.blobrouter.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.model.out.SasTokenResponse;
import uk.gov.hmcts.reform.blobrouter.services.SasTokenGeneratorService;

@RestController
@RequestMapping("/token")
public class SasTokenController {

    private final SasTokenGeneratorService tokenGeneratorService;

    public SasTokenController(SasTokenGeneratorService tokenGeneratorService) {
        this.tokenGeneratorService = tokenGeneratorService;
    }

    @GetMapping(path = "/{serviceName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Get SAS Token to access blob storage")
    @ApiResponse(code = 200, message = "Success")
    public ResponseEntity<SasTokenResponse> getSasToken(@PathVariable String serviceName) {
        String sasToken = tokenGeneratorService.generateSasToken(serviceName.replaceAll("[\n|\r|\t]", "_"));

        return ResponseEntity.ok(new SasTokenResponse(sasToken));
    }
}
