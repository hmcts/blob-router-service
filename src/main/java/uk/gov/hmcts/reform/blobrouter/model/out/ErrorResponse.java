package uk.gov.hmcts.reform.blobrouter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResponse {

    @JsonProperty("message")
    public final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }
}
