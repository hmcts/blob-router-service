package uk.gov.hmcts.reform.blobrouter.exceptions;

public class InvalidRequestParametersException extends RuntimeException {
    public InvalidRequestParametersException(String message) {
        super(message);
    }
}
