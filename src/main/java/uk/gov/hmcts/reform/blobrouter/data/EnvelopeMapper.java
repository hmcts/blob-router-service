package uk.gov.hmcts.reform.blobrouter.data;

import org.springframework.jdbc.core.RowMapper;
import uk.gov.hmcts.reform.blobrouter.data.model.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class EnvelopeMapper implements RowMapper<Envelope> {

    @Override
    public Envelope mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Envelope(
            UUID.fromString(rs.getString("id")),
            rs.getString("container"),
            rs.getString("file_name"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("file_created_at").toInstant(),
            rs.getTimestamp("dispatched_at").toInstant(),
            Status.valueOf(rs.getString("status")),
            rs.getBoolean("is_deleted")
        );
    }
}
