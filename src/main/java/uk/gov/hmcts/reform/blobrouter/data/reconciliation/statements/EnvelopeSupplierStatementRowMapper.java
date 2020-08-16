package uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class EnvelopeSupplierStatementRowMapper implements RowMapper<EnvelopeSupplierStatement> {

    @Override
    public EnvelopeSupplierStatement mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new EnvelopeSupplierStatement(
            UUID.fromString(rs.getString("id")),
            rs.getObject("date", LocalDate.class),
            rs.getString("content"),
            rs.getString("content_type_version"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
