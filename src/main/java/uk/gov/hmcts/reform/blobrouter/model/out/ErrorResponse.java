package uk.gov.hmcts.reform.blobrouter.model.out;

public class ErrorResponse {

    public final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }
}
