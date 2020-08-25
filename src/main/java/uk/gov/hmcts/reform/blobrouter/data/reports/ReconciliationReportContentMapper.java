package uk.gov.hmcts.reform.blobrouter.data.reports;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class ReconciliationReportContentMapper implements RowMapper<ReconciliationReportContent> {

    @Override
    public ReconciliationReportContent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ReconciliationReportContent(
            UUID.fromString(rs.getString("id")),
            rs.getString("content"),
            rs.getString("content_type_version")
        );
    }
}
