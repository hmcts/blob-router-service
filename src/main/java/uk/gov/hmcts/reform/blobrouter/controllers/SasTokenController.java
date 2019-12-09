package uk.gov.hmcts.reform.blobrouter.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.blobrouter.model.out.ErrorResponse;
import uk.gov.hmcts.reform.blobrouter.model.out.SasTokenResponse;
import uk.gov.hmcts.reform.blobrouter.services.SasTokenGeneratorService;

@RestController
@RequestMapping(path = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "SAS Token Endpoint", description = "Endpoint for SAS Token related interactions")
public class SasTokenController {

    private final SasTokenGeneratorService tokenGeneratorService;

    public SasTokenController(SasTokenGeneratorService tokenGeneratorService) {
        this.tokenGeneratorService = tokenGeneratorService;
    }

    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Success",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SasTokenResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Service not configured / is disabled",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error while generating SAS Token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping(path = "/{serviceName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get SAS Token",
        description = "Get SAS Token to access blob storage",
        parameters = @Parameter(
            in = ParameterIn.PATH,
            name = "serviceName",
            description = "Service name matching blob storage container",
            example = "service-name"
        )
    )
    public ResponseEntity<SasTokenResponse> getSasToken(@PathVariable String serviceName) {
        String sasToken = tokenGeneratorService.generateSasToken(serviceName.replaceAll("[\n|\r|\t]", "_"));

        return ResponseEntity.ok(new SasTokenResponse(sasToken));
    }
}
