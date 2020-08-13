package uk.gov.hmcts.reform.blobrouter.data.reconciliation;

import org.postgresql.util.PGobject;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.model.NewReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.model.ReconciliationReport;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.validation.ClockProvider;

@Repository
public class ReconciliationReportRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ReconciliationReportRowMapper rowMapper;
    private final Clock clock;

    public ReconciliationReportRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        ReconciliationReportRowMapper rowMapper,
        ClockProvider clockProvider
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
        this.clock = clockProvider.getClock();
    }

    public UUID save(NewReconciliationReport report) throws SQLException {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO envelope_reconciliation_reports "
                + "(id, envelope_supplier_statement_id, account, content, content_type_version, created_at) "
                + "VALUES "
                + "(:id, :statementId, :account, :content, :contentTypeVersion, :createdAt)",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("statementId", report.supplierStatementId)
                .addValue("account", report.account)
                .addValue("content", toJson(report.content))
                .addValue("contentTypeVersion", report.contentTypeVersion)
                .addValue("createdAt", LocalDateTime.now(clock))
        );
        return id;
    }

    public Optional<ReconciliationReport> findById(UUID id) {
        try {
            ReconciliationReport report = jdbcTemplate.queryForObject(
                "SELECT * FROM envelope_reconciliation_reports WHERE id = :id",
                new MapSqlParameterSource("id", id),
                this.rowMapper
            );
            return Optional.of(report);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private PGobject toJson(String string) throws SQLException {
        var json = new PGobject();
        json.setType("json");
        json.setValue(string);
        return json;
    }
}
