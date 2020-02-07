package uk.gov.hmcts.reform.blobrouter.services.email;

public class SendEmailException extends Exception {
    public SendEmailException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
