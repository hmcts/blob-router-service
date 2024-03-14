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

/**
 * The `ReconciliationReportRepository` class in Java provides methods for saving, retrieving,
 * and updating reconciliation reports in a database using JDBC template.
 */
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

    /**
     * The `save` function inserts a new reconciliation report into a database table and returns the
     * generated UUID for the report.
     *
     * @param report The `save` method you provided is responsible for saving a `NewReconciliationReport` object into a
     *      database table named `envelope_reconciliation_reports`. The method generates a random UUID as the
     *      ID for the report, inserts the report data into the table, and returns the generated UUID.
     * @return The method `save` is returning a randomly generated UUID that is assigned to the `id` variable before
     *      inserting a new record into the `envelope_reconciliation_reports` table in the database.
     */
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

    /**
     * This Java function retrieves a ReconciliationReport object by its UUID identifier using JDBC template
     * and returns it wrapped in an Optional, handling empty results with Optional.empty().
     *
     * @param id The `id` parameter in the `findById` method is a `UUID` type that represents the unique identifier
     *      of the reconciliation report that you want to retrieve from the database.
     * @return An Optional containing a ReconciliationReport object is being returned. If the report is found in the
     *      database, it will be wrapped in an Optional and returned. If the report is not
     *      found (EmptyResultDataAccessException is caught), an empty Optional will be returned.
     */
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

    /**
     * This Java function retrieves reconciliation reports by date from a database using JDBC template.
     *
     * @param date The `date` parameter is used to filter the reconciliation reports based on the date of the envelope
     *      supplier statement. The method `findByDate` retrieves a list of `ReconciliationReport` objects
     *      from the database where the `ess.date` matches the provided `date` parameter.
     * @return A List of ReconciliationReport objects is being returned.
     */
    public List<ReconciliationReport> findByDate(LocalDate date) {
        return jdbcTemplate.query(
            "SELECT er.* "
                + "FROM envelope_reconciliation_reports er "
                + "INNER JOIN envelope_supplier_statements ess ON ess.id = er.envelope_supplier_statement_id "
                + "WHERE ess.date = :date "
                + "ORDER BY created_at DESC",
            new MapSqlParameterSource("date", date),
            this.rowMapper
        );
    }

    /**
     * This Java function retrieves the latest reconciliation report for a specific date and account from
     * a database using JDBC template.
     *
     * @param forDate The `forDate` parameter represents the date for which you want to retrieve the latest
     *      reconciliation report. This date is used in the SQL query to filter the results based on the
     *      `ess.date` column in the `envelope_supplier_statements` table.
     * @param account The `account` parameter in the `getLatestReconciliationReport` method is used to specify
     *      the account for which you want to retrieve the latest reconciliation report. It is a String type
     *      parameter that represents the account identifier or name associated with the reconciliation report
     *      you are looking for.
     * @return An Optional containing the latest ReconciliationReport for the specified date and account, or an empty
     *      Optional if no report is found.
     */
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

    /**
     * The function updates the detailed content of an envelope reconciliation report in a database using a
     * provided UUID and new detailed content.
     *
     * @param id The `id` parameter is of type `UUID` and represents the unique identifier of the envelope
     *      reconciliation report that needs to be updated with new detailed content.
     * @param newDetailedContent The `newDetailedContent` parameter in the `updateDetailedContent` method is a
     *      String that represents the updated detailed content that you want to set for a specific envelope
     *      reconciliation report identified by the `id` parameter.
     */
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

    /**
     * The function `findByStatementId` retrieves reconciliation reports based on a given supplier statement ID.
     *
     * @param supplierStatementId The `supplierStatementId` parameter is a UUID representing the identifier
     *      of a supplier statement. This method `findByStatementId` is querying a database table named
     *      `envelope_reconciliation_reports` to retrieve reconciliation reports based on the provided
     *      `supplierStatementId`.
     * @return A list of `ReconciliationReport` objects that match the `envelope_supplier_statement_id` with
     *      the provided `supplierStatementId`.
     */
    public List<ReconciliationReport> findByStatementId(UUID supplierStatementId) {
        return jdbcTemplate.query(
            "SELECT * FROM envelope_reconciliation_reports "
                + "WHERE envelope_supplier_statement_id = :id",
            new MapSqlParameterSource("id", supplierStatementId),
            this.rowMapper
        );
    }

    /**
     * The `updateSentAt` function updates the `sent_at` field in the `envelope_reconciliation_reports` table with the
     * current timestamp for a specific `id`.
     *
     * @param id The `id` parameter is of type `UUID`, which stands for Universally Unique Identifier. It is a 128-bit
     *      value typically used as a unique identifier for entities in a system.
     */
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
