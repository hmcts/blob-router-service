package uk.gov.hmcts.reform.blobrouter.data.reports;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class EnvelopeCountSummaryMapper implements RowMapper<EnvelopeCountSummaryReportItem> {

    @Override
    public EnvelopeCountSummaryReportItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new EnvelopeCountSummaryReportItem(
            rs.getInt("received"),
            rs.getInt("rejected"),
            rs.getString("container"),
            rs.getTimestamp("date").toLocalDateTime().toLocalDate()
        );
    }
}
