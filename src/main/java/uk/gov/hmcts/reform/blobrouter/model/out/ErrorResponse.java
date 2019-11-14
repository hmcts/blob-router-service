package uk.gov.hmcts.reform.blobrouter.model.out;

public class ErrorResponse {

    public final String message;

    public final Throwable cause;

    public ErrorResponse(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }
}
