package uk.gov.hmcts.reform.blobrouter.data.notifications;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class NotificationEnvelopeMapper implements RowMapper<NotificationEnvelope> {

    @Override
    public NotificationEnvelope mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationEnvelope(
            rs.getString("container"),
            rs.getString("file_name"),
            rs.getString("error_type"),
            rs.getString("description")
        );
    }
}
