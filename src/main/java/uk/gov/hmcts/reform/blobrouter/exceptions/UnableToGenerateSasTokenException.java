package uk.gov.hmcts.reform.blobrouter.exceptions;

public class UnableToGenerateSasTokenException extends RuntimeException {

    private static final long serialVersionUID = -2633962330074436013L;

    public UnableToGenerateSasTokenException(String message, Throwable e) {
        super(message, e);
    }
}
