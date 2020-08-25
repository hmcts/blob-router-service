package uk.gov.hmcts.reform.blobrouter.data.reports;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Repository
public class ReportRepository {

    private static final String EXCLUDED_CONTAINER = "bulkscan";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EnvelopeSummaryMapper mapper;
    private final ReconciliationReportContentMapper reconciliationMapper;

    public ReportRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        EnvelopeSummaryMapper mapper,
        ReconciliationReportContentMapper reconciliationMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
        this.reconciliationMapper = reconciliationMapper;
    }

    public List<EnvelopeSummary> getEnvelopeSummary(Instant from, Instant to) {
        return jdbcTemplate.query(
            "SELECT container, file_name, file_created_at, dispatched_at, status, is_deleted "
                + "FROM envelopes "
                + "WHERE container <> '" + EXCLUDED_CONTAINER + "' "
                + "  AND file_created_at >= :from "
                + "  AND file_created_at < :to "
                + "ORDER BY file_created_at",
            new MapSqlParameterSource()
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to)),
            this.mapper
        );
    }

    /**
     * Return as a list so appropriate message can be returned to viewer.
     * @param forDate For which date report should be retrieved
     * @param account Report account
     * @return empty list or single element
     */
    public List<ReconciliationReportContent> getReconciliationReport(LocalDate forDate, String account) {
        return jdbcTemplate.query(
            "SELECT id, content, content_type_version "
                + "FROM envelope_reconciliation_reports "
                + "WHERE account = :account"
                + "  AND DATE(created_at) = :date "
                + "ORDER BY created_at DESC "
                + "LIMIT 1",
            new MapSqlParameterSource()
                .addValue("date", forDate)
                .addValue("account", account),
            reconciliationMapper
        );
    }
}
