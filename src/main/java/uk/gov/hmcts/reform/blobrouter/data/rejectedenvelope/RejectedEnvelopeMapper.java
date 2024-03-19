package uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class RejectedEnvelopeMapper implements RowMapper<RejectedEnvelope> {

    /**
     * The `mapRow` function maps a row from a ResultSet to a RejectedEnvelope object in Java.
     *
     * @param rs The `rs` parameter in the `mapRow` method is a `ResultSet` object, which represents a set of results
     *           of a database query. It provides methods to retrieve data from the result set based on the current
     *           row pointer position.
     * @param rowNum The `rowNum` parameter in the `mapRow` method is an integer representing the current row
     *               number being processed by the ResultSet. It is used to keep track of the position of the
     *               current row within the ResultSet.
     * @return A RejectedEnvelope object is being returned.
     */
    @Override
    public RejectedEnvelope mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new RejectedEnvelope(
            UUID.fromString(rs.getString("id")),
            rs.getString("container"),
            rs.getString("file_name"),
            rs.getString("error_code") != null ? ErrorCode.valueOf(rs.getString("error_code")) : null,
            rs.getString("errorDescription")
        );
    }
}
