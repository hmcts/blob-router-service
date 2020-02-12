package uk.gov.hmcts.reform.blobrouter.exceptions;

public class InvalidConfigException extends RuntimeException {
    public InvalidConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
