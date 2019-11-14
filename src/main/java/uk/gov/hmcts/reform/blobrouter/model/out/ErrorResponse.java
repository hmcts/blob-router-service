package uk.gov.hmcts.reform.blobrouter.model.out;

public class ErrorResponse {

    public final String message;

    public final Class<?> cause;

    public ErrorResponse(String message, Class<?> cause) {
        this.message = message;
        this.cause = cause;
    }
}
