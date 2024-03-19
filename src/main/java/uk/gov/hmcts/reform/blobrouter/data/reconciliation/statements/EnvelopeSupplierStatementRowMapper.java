package uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The `EnvelopeSupplierStatementRowMapper` class in Java implements a `RowMapper` interface to map a row from a
 * `ResultSet` to an `EnvelopeSupplierStatement` object.
 */
@Component
public class EnvelopeSupplierStatementRowMapper implements RowMapper<EnvelopeSupplierStatement> {

    /**
     * The `mapRow` function maps a row from a ResultSet to an EnvelopeSupplierStatement object in Java.
     *
     * @param rs The `rs` parameter in the `mapRow` method is a `ResultSet` object, which represents a set of
     *      results from a database query. It provides access to the data returned by the query and allows you to
     *      retrieve values from the columns in the result set.
     * @param rowNum The `rowNum` parameter in the `mapRow` method represents the current row number being
     *      processed by the ResultSet. It starts from 0 for the first row and increments by 1 for each subsequent
     *      row in the ResultSet.
     * @return An EnvelopeSupplierStatement object is being returned.
     */
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
