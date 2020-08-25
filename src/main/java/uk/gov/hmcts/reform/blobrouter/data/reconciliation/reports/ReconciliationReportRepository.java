package uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.NewReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationContent;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.validation.ClockProvider;

import static uk.gov.hmcts.reform.blobrouter.data.Utils.toJson;

@Repository
public class ReconciliationReportRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ReconciliationReportRowMapper rowMapper;
    private final ReconciliationContentMapper contentMapper;
    private final Clock clock;

    public ReconciliationReportRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        ReconciliationReportRowMapper rowMapper,
        ReconciliationContentMapper contentMapper,
        ClockProvider clockProvider
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
        this.contentMapper = contentMapper;
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

    public Optional<ReconciliationContent> getReconciliationReport(LocalDate forDate, String account) {
        try {
            ReconciliationContent report = jdbcTemplate.queryForObject(
                "SELECT id, content, content_type_version "
                    + "FROM envelope_reconciliation_reports "
                    + "WHERE account = :account"
                    + "  AND DATE(created_at) = :date "
                    + "ORDER BY created_at DESC "
                    + "LIMIT 1",
                new MapSqlParameterSource()
                    .addValue("date", forDate)
                    .addValue("account", account),
                contentMapper
            );

            return Optional.ofNullable(report);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
