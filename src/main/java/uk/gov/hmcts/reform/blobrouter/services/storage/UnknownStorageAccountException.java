package uk.gov.hmcts.reform.blobrouter.services.storage;

public class UnknownStorageAccountException extends RuntimeException {

    public UnknownStorageAccountException(String message) {
        super(message);
    }
}
