package uk.gov.hmcts.reform.blobrouter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public class SasTokenResponse {

    @Schema(
        name = "SAS Token",
        description = "Shared access token to access blobs"
    )
    @JsonProperty("sas_token")
    public final String sasToken;

    public SasTokenResponse(String sasToken) {
        this.sasToken = sasToken;
    }
}
