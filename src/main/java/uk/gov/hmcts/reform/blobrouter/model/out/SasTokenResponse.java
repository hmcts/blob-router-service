package uk.gov.hmcts.reform.blobrouter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public class SasTokenResponse {

    @JsonProperty("sas_token")
    @Schema(title = "SAS Token", name = "sas_token", description = "Shared access token to access blobs")
    public final String sasToken;

    public SasTokenResponse(String sasToken) {
        this.sasToken = sasToken;
    }
}
