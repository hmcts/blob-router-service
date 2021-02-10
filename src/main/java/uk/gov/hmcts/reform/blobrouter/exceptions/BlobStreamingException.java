package uk.gov.hmcts.reform.blobrouter.exceptions;

public class BlobStreamingException extends RuntimeException {

    public BlobStreamingException(String message, Throwable cause) {
        super(message, cause);
    }
}
