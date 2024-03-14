package uk.gov.hmcts.reform.blobrouter.data.reports;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The `EnvelopeSummaryMapper` class in Java implements `RowMapper` to map a row from a `ResultSet` to
 * an `EnvelopeSummary` object.
 */
@Component
public class EnvelopeSummaryMapper implements RowMapper<EnvelopeSummary> {

    /**
     * The `mapRow` function maps a row from a ResultSet to an EnvelopeSummary object in Java.
     *
     * @param rs The `rs` parameter in the `mapRow` method is a `ResultSet` object, which represents a set of
     *           results from a database query. It provides access to the data returned by the query and allows you
     *           to retrieve values from the columns in the result set.
     * @param rowNum The `rowNum` parameter in the `mapRow` method represents the current row number
     *               being processed by the ResultSet. It is an integer value that indicates the position of the
     *               current row within the ResultSet.
     * @return An EnvelopeSummary object is being returned.
     */
    @Override
    public EnvelopeSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new EnvelopeSummary(
            rs.getString("container"),
            rs.getString("file_name"),
            rs.getTimestamp("file_created_at").toInstant(),
            rs.getTimestamp("dispatched_at") == null
                ? null : rs.getTimestamp("dispatched_at").toInstant(),
            Status.valueOf(rs.getString("status")),
            rs.getBoolean("is_deleted")
        );
    }
}
