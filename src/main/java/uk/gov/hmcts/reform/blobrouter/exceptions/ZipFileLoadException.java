package uk.gov.hmcts.reform.blobrouter.exceptions;

public class ZipFileLoadException extends RuntimeException {

    public ZipFileLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
