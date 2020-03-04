package uk.gov.hmcts.reform.blobrouter.data.eventsv2;

import org.springframework.jdbc.core.RowMapper;
import uk.gov.hmcts.reform.blobrouter.data.events.EventType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class EnvelopeEventMapper implements RowMapper<EnvelopeEvent> {
    @Override
    public EnvelopeEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new EnvelopeEvent(
            rs.getLong("id"),
            UUID.fromString(rs.getString("envelope_id")),
            EventType.valueOf(rs.getString("event")),
            rs.getString("notes"),
            rs.getTimestamp("created_at").toInstant()
        );
    }
}
