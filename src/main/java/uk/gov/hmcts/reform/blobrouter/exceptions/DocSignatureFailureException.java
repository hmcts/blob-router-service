package uk.gov.hmcts.reform.blobrouter.exceptions;

/**
 * Exception representing a failure because of an invalid signature.
 */
public class DocSignatureFailureException extends RuntimeException {

    private static final long serialVersionUID = 7705142254662981720L;

    public DocSignatureFailureException(String message) {
        super(message);
    }

    public DocSignatureFailureException(String message, Exception cause) {
        super(message, cause);
    }
}

