package uk.gov.hmcts.reform.blobrouter.exceptions;

public class EnvelopeNotFoundException extends RuntimeException {

    public EnvelopeNotFoundException() {
    }

    public EnvelopeNotFoundException(String message) {
        super(message);
    }
}
