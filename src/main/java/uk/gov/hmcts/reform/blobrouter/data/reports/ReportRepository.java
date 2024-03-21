package uk.gov.hmcts.reform.blobrouter.data.reports;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;

/**
 * The `ReportRepository` class in Java provides methods to retrieve envelope summaries and generate reports based on
 * specified criteria from a database.
 */
@Repository
public class ReportRepository {
    private static final String EXCLUDED_CONTAINER = "bulkscan";
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EnvelopeSummaryMapper mapper;
    private final EnvelopeCountSummaryMapper summaryMapper;

    public ReportRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        EnvelopeSummaryMapper mapper,
        EnvelopeCountSummaryMapper summaryMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
        this.summaryMapper = summaryMapper;
    }

    /**
     * This function retrieves envelope summaries based on specified time range and excludes a specific container.
     *
     * @param from The `from` parameter is the starting point in time for the query to retrieve envelope summaries.
     *             It is of type `Instant`, which represents a moment on the timeline in UTC with a
     *             resolution of nanoseconds.
     * @param to The `to` parameter in the `getEnvelopeSummary` method represents the end timestamp for the query.
     *           It is used to filter envelopes based on the `file_created_at` timestamp so that only envelopes
     *           created before this timestamp are included in the result.
     * @return A List of EnvelopeSummary objects containing information such as container, file name, file created date,
     *      dispatched date, status, and deletion status for envelopes that meet the specified criteria.
     */
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
     * This Java function retrieves a report for a specific date and list of containers, querying a database
     * to get summary information on received and rejected envelopes.
     *
     * @param date The `date` parameter is of type `LocalDate` and represents the date for which the report is being
     *      generated.
     * @param containersList A list of container names for which you want to generate the report.
     * @return This method returns a list of `EnvelopeCountSummaryReportItem` objects based on the provided date
     *      and list of container names. The SQL query retrieves data about the number of envelopes received
     *      and rejected for each container on the specified date. The result is mapped to
     *      `EnvelopeCountSummaryReportItem` objects using the `summaryMapper`.
     */
    public List<EnvelopeCountSummaryReportItem> getReportFor(LocalDate date, List<String> containersList) {
        StringJoiner containers = new StringJoiner(", ");
        containersList.forEach(containerName -> containers.add("('" + containerName + "')"));
        return jdbcTemplate.query(
                  "SELECT A.container, (CASE WHEN G.received IS NULL THEN 0 ELSE G.received END) AS received,"
                      + " (CASE WHEN G.rejected IS NULL THEN 0 ELSE G.rejected END) AS rejected,:date as date"
                      + "  FROM(VALUES" + containers + ") AS A (container)"
                      + "  LEFT JOIN"
                      + "  (SELECT container as Container, date (created_at) AS date,"
                      + "  count(*) as received,"
                      + "  SUM (CASE WHEN Envelopes.status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected"
                      + "  FROM Envelopes"
                      + "  GROUP BY container, date(created_at)"
                      + "  HAVING date(created_at) = :date)"
                      + "  AS G on A.container = G.container",
            new MapSqlParameterSource()
                .addValue("date", date),
            this.summaryMapper
        );
    }
}
