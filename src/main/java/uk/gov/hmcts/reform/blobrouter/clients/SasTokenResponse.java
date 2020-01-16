package uk.gov.hmcts.reform.blobrouter.clients;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SasTokenResponse {

    @JsonProperty("sas_token")
    public final String sasToken;

    public SasTokenResponse(String sasToken) {
        this.sasToken = sasToken;
    }
}
