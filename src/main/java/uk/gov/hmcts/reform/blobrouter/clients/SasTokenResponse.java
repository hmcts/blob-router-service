package uk.gov.hmcts.reform.blobrouter.clients;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SasTokenResponse {

    public final String sasToken;

    public SasTokenResponse(@JsonProperty("sas_token") String sasToken) {
        this.sasToken = sasToken;
    }
}
