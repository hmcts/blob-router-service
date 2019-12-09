package uk.gov.hmcts.reform.blobrouter.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

/**
 * Default endpoints per application.
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Blob Router",
        version = "2",
        description = "API serving SAS Tokens for uploads to Azure Blob Storage",
        license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")
    )
)
@RestController
public class RootController {

    /**
     * Root GET endpoint.
     *
     * <p>Azure application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on.
     * This is the endpoint to deal with that and therefore silence the unnecessary 404s as a response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping("/")
    @Hidden
    public ResponseEntity<String> welcome() {
        return ok("Welcome to blob-router-service");
    }
}
