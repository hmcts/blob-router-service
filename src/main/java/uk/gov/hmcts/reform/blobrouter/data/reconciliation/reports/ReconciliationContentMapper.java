package uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationContent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class ReconciliationContentMapper implements RowMapper<ReconciliationContent> {

    @Override
    public ReconciliationContent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ReconciliationContent(
            UUID.fromString(rs.getString("id")),
            rs.getString("content"),
            rs.getString("content_type_version")
        );
    }
}
