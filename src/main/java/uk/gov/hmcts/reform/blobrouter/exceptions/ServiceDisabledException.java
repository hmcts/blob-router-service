package uk.gov.hmcts.reform.blobrouter.exceptions;

public class ServiceDisabledException extends RuntimeException {
    public ServiceDisabledException(String message) {
        super(message);
    }
}
