package uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The `ReconciliationReportRowMapper` class in Java implements a `RowMapper` interface to map database query results to
 * `ReconciliationReport` objects, with a method to convert `Timestamp` to `LocalDateTime`.
 */
@Component
public class ReconciliationReportRowMapper implements RowMapper<ReconciliationReport> {

    /**
     * The mapRow function maps a row from a ResultSet to a ReconciliationReport object in Java.
     *
     * @param rs ResultSet rs is a Java ResultSet object that represents a set of results from a database query. It
     *      contains the data retrieved from the database based on the query executed.
     * @param rowNum The `rowNum` parameter in the `mapRow` method represents the current row number being processed
     *      by the ResultSet. It is an integer value that indicates the position of the current row within
     *      the result set.
     * @return A new instance of `ReconciliationReport` is being returned, with the values extracted from
     *      the `ResultSet` `rs`.
     */
    @Override
    public ReconciliationReport mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ReconciliationReport(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("envelope_supplier_statement_id")),
            rs.getString("account"),
            rs.getString("summary_content"),
            rs.getString("detailed_content"),
            rs.getString("content_type_version"),
            toDateTime(rs.getTimestamp("sent_at")),
            toDateTime(rs.getTimestamp("created_at"))
        );
    }

    /**
     * The function `toDateTime` converts a `Timestamp` object to a `LocalDateTime` object in Java.
     *
     * @param timestamp The `timestamp` parameter is of type `Timestamp`, which is a class in Java that represents a
     *      specific point in time, including date and time information.
     * @return The method `toDateTime` returns a `LocalDateTime` object. If the input `timestamp` is `null`, then the
     *      method returns `null`. Otherwise, it converts the `timestamp` to a `LocalDateTime` object using the
     *      `toLocalDateTime()` method of the `Timestamp` class.
     */
    private LocalDateTime toDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
