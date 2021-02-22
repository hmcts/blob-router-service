package uk.gov.hmcts.reform.blobrouter.exceptions;

public class EnvelopeCompletedOrNotStaleException extends RuntimeException {

    public EnvelopeCompletedOrNotStaleException(String message) {
        super(message);
    }
}
