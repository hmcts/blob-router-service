package uk.gov.hmcts.reform.blobrouter.config;

/**
 * The class `PendingMigrationException` extends `RuntimeException` and represents an exception for pending database
 * migrations.
 */
public class PendingMigrationException extends RuntimeException {
    public PendingMigrationException(String message) {
        super(message);
    }
}
