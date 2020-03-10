package uk.gov.hmcts.reform.blobrouter.exceptions;

public class InvalidSasTokenException extends RuntimeException {

    public static final String EXPIRY_NOT_FOUND = "Invalid SAS, the SAS expiration time parameter not found.";

    public InvalidSasTokenException(String message) {
        super(message);
    }

}
