package uk.gov.hmcts.reform.blobrouter.data.events;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * The `EnvelopeEventMapper` class in Java implements the `RowMapper` interface to map rows from a `ResultSet` to
 * `EnvelopeEvent` objects.
 */
@Component
public class EnvelopeEventMapper implements RowMapper<EnvelopeEvent> {
    /**
     * The `mapRow` function maps a row from a ResultSet to an EnvelopeEvent object in Java.
     *
     * @param rs The `rs` parameter in the `mapRow` method is a `ResultSet` object, which represents
     *      a set of results from a database query. It provides access to the data returned by the query,
     *      allowing you to retrieve values from the columns of the result set.
     * @param rowNum The `rowNum` parameter in the `mapRow` method represents the current row number being
     *      processed by the ResultSet. It is an integer value that indicates the position of the current row
     *      within the result set.
     * @return An EnvelopeEvent object is being returned.
     */
    @Override
    public EnvelopeEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new EnvelopeEvent(
            rs.getLong("id"),
            UUID.fromString(rs.getString("envelope_id")),
            EventType.valueOf(rs.getString("type")),
            rs.getString("error_code") != null ? ErrorCode.valueOf(rs.getString("error_code")) : null,
            rs.getString("notes"),
            rs.getTimestamp("created_at").toInstant()
        );
    }
}
