package uk.gov.hmcts.reform.blobrouter.data.events;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class EnvelopeEventMapper implements RowMapper<EnvelopeEvent> {
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
