package uk.gov.hmcts.reform.blobrouter.data.reports;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The EnvelopeCountSummaryMapper class implements RowMapper to map ResultSet rows to EnvelopeCountSummaryReportItem
 * objects.
 */
@Component
public class EnvelopeCountSummaryMapper implements RowMapper<EnvelopeCountSummaryReportItem> {

    /**
     * The `mapRow` function maps a row from a ResultSet to an EnvelopeCountSummaryReportItem object by
     * extracting values for received, rejected, container, and date fields.
     *
     * @param rs ResultSet rs is the result set obtained from a database query, which contains the data retrieved
     *           from the database based on the executed SQL query.
     * @param rowNum The `rowNum` parameter in the `mapRow` method represents the current row number being processed
     *               by the ResultSet. It starts from 0 for the first row and increments by 1 for each subsequent row
     *               in the ResultSet.
     * @return An EnvelopeCountSummaryReportItem object is being returned, which is created using the values
     *      retrieved from the ResultSet rs. The EnvelopeCountSummaryReportItem constructor is called with the
     *      values of "received", "rejected", "container", and "date" columns from the ResultSet.
     */
    @Override
    public EnvelopeCountSummaryReportItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new EnvelopeCountSummaryReportItem(
            rs.getInt("received"),
            rs.getInt("rejected"),
            rs.getString("container"),
            rs.getTimestamp("date").toLocalDateTime().toLocalDate()
        );
    }
}
