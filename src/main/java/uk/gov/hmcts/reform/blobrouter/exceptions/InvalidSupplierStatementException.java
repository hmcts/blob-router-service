package uk.gov.hmcts.reform.blobrouter.exceptions;

public class InvalidSupplierStatementException extends RuntimeException {
    public InvalidSupplierStatementException(String message, Exception cause) {
        super(message, cause);
    }

    public InvalidSupplierStatementException(String message) {
        super(message);
    }
}
