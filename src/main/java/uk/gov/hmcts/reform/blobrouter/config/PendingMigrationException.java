package uk.gov.hmcts.reform.blobrouter.config;

public class PendingMigrationException extends RuntimeException {
    public PendingMigrationException(String message) {
        super(message);
    }
}
