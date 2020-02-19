package uk.gov.hmcts.reform.blobrouter.exceptions;

public class BlobProcessingException extends RuntimeException {
    public BlobProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
