package uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ReconciliationReportRowMapper implements RowMapper<ReconciliationReport> {

    @Override
    public ReconciliationReport mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ReconciliationReport(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("envelope_supplier_statement_id")),
            rs.getString("account"),
            rs.getString("summaryContent"),
            rs.getString("detailedContent"),
            rs.getString("content_type_version"),
            toDateTime(rs.getTimestamp("sent_at")),
            toDateTime(rs.getTimestamp("created_at"))
        );
    }

    private LocalDateTime toDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
