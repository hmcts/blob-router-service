package uk.gov.hmcts.reform.blobrouter.data.rejectedenvelope;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class RejectedEnvelopeMapper implements RowMapper<RejectedEnvelope> {

    @Override
    public RejectedEnvelope mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new RejectedEnvelope(
            UUID.fromString(rs.getString("id")),
            rs.getString("container"),
            rs.getString("file_name"),
            rs.getString("errorDescription")
        );
    }
}
