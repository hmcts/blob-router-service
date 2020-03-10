package uk.gov.hmcts.reform.blobrouter.exceptions;

public class InvalidSasTokenException extends RuntimeException {

    public InvalidSasTokenException(String message) {
        super(message);
    }

}
