package uk.gov.hmcts.reform.blobrouter.exceptions;

public class InvalidZipArchiveException extends RuntimeException {

    private static final long serialVersionUID = 6200831488713697422L;

    public InvalidZipArchiveException(String message) {
        super(message);
    }

    public InvalidZipArchiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
