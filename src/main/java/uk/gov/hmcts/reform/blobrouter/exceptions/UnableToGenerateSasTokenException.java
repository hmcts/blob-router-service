package uk.gov.hmcts.reform.blobrouter.exceptions;

public class UnableToGenerateSasTokenException extends RuntimeException {

    private static final long serialVersionUID = -2633962330074436013L;

    public UnableToGenerateSasTokenException(Throwable e) {
        super(e);
    }
}
