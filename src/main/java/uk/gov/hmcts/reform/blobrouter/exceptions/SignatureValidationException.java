package uk.gov.hmcts.reform.blobrouter.exceptions;

public class SignatureValidationException extends RuntimeException {

    private static final long serialVersionUID = 5566976005036215658L;

    public SignatureValidationException(Throwable t) {
        super(t);
    }

    public SignatureValidationException(String message) {
        super(message);
    }
}
