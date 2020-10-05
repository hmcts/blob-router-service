package uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.NewReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.validation.ClockProvider;

import static uk.gov.hmcts.reform.blobrouter.data.Utils.toJson;

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
                + "(id, envelope_supplier_statement_id, account, "
                + "summary_content, detailed_content, content_type_version, created_at) "
                + "VALUES "
                + "(:id, :statementId, :account, :summaryContent, :detailedContent, :contentTypeVersion, :createdAt)",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("statementId", report.supplierStatementId)
                .addValue("account", report.account)
                .addValue("summaryContent", toJson(report.summaryContent))
                .addValue("detailedContent", toJson(report.detailedContent))
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

    public List<ReconciliationReport> findByDate(LocalDate date) {
        return jdbcTemplate.query(
            "SELECT * FROM envelope_reconciliation_reports "
                + "WHERE DATE(created_at) = :date "
                + "ORDER BY created_at DESC",
            new MapSqlParameterSource("date", date),
            this.rowMapper
        );
    }

    public Optional<ReconciliationReport> getLatestReconciliationReport(LocalDate forDate, String account) {
        try {
            ReconciliationReport report = jdbcTemplate.queryForObject(
                "SELECT er.*"
                    + " FROM envelope_reconciliation_reports er"
                    + " INNER JOIN envelope_supplier_statements ess ON ess.id = er.envelope_supplier_statement_id"
                    + " WHERE ess.date = :date AND er.account = :account"
                    + " ORDER BY er.created_at DESC"
                    + " LIMIT 1",
                new MapSqlParameterSource()
                    .addValue("date", forDate)
                    .addValue("account", account),
                rowMapper
            );

            return Optional.ofNullable(report);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void updateDetailedContent(UUID id, String newDetailedContent) throws SQLException {
        jdbcTemplate.update(
            "UPDATE envelope_reconciliation_reports "
                + "SET detailed_content = :newDetailedContent "
                + "WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("newDetailedContent", toJson(newDetailedContent))
        );
    }

    public List<ReconciliationReport> findByStatementId(UUID supplierStatementId) {
        return jdbcTemplate.query(
            "SELECT * FROM envelope_reconciliation_reports "
                + "WHERE envelope_supplier_statement_id = :id",
            new MapSqlParameterSource("id", supplierStatementId),
            this.rowMapper
        );
    }

    public void updateSentAt(UUID id) {
        jdbcTemplate.update(
            "UPDATE envelope_reconciliation_reports "
                + "SET sent_at = :sendAt "
                + "WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("sendAt", LocalDateTime.now(clock))
        );
    }
}
