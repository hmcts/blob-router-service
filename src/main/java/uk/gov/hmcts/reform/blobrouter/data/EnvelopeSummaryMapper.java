package uk.gov.hmcts.reform.blobrouter.data;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.model.EnvelopeSummary;
import uk.gov.hmcts.reform.blobrouter.data.model.Event;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class EnvelopeSummaryMapper implements RowMapper<EnvelopeSummary> {

    @Override
    public EnvelopeSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new EnvelopeSummary(
            rs.getString("container"),
            rs.getString("file_name"),
            rs.getTimestamp("file_created_at").toInstant(),
            rs.getTimestamp("file_dispatched_at") == null
                ? null : rs.getTimestamp("file_dispatched_at").toInstant(),
            Status.valueOf(rs.getString("status")),
            rs.getBoolean("is_deleted"),
            rs.getString("event") == null ? null : Event.valueOf(rs.getString("event")),
            rs.getString("notes"),
            rs.getTimestamp("event_created_at") == null
                ? null : rs.getTimestamp("event_created_at").toInstant()
        );
    }
}
