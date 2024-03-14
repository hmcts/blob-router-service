package uk.gov.hmcts.reform.blobrouter.data.envelopes;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * The EnvelopeMapper class implements RowMapper to map ResultSet data to an Envelope object in Java.
 */
@Component
public class EnvelopeMapper implements RowMapper<Envelope> {

    /**
     * The mapRow function maps a ResultSet to an Envelope object by extracting values from the ResultSet and creating a
     * new Envelope instance.
     *
     * @param rs The `rs` parameter in the `mapRow` method is a `ResultSet` object, which represents a set of results
     *      from a database query. It contains the data retrieved from the database based on the executed query.
     * @param rowNum The `rowNum` parameter in the `mapRow` method represents the current row number being processed
     *      by the ResultSetExtractor. It is an integer value that indicates the current row number starting from 0.
     * @return An Envelope object is being returned, which is created using the data retrieved from the ResultSet rs.
     */
    @Override
    public Envelope mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Envelope(
            UUID.fromString(rs.getString("id")),
            rs.getString("container"),
            rs.getString("file_name"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("file_created_at").toInstant(),
            rs.getTimestamp("dispatched_at") == null
                ? null : rs.getTimestamp("dispatched_at").toInstant(),
            Status.valueOf(rs.getString("status")),
            rs.getBoolean("is_deleted"),
            rs.getBoolean("pending_notification"),
            rs.getLong("file_size")
        );
    }
}
