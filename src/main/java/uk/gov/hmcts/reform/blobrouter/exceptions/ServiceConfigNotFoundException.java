package uk.gov.hmcts.reform.blobrouter.exceptions;

public class ServiceConfigNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -2342424471035548224L;

    public ServiceConfigNotFoundException(String message) {
        super(message);
    }
}
