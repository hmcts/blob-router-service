package uk.gov.hmcts.reform.blobrouter.clients.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The SasTokenResponse class represents a response object containing a SAS token in Java.
 */
public class SasTokenResponse {

    public final String sasToken;

    public SasTokenResponse(@JsonProperty("sas_token") String sasToken) {
        this.sasToken = sasToken;
    }
}
