package uk.gov.hmcts.reform.blobrouter.data.reports;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ReportRepository {
    private static final String EXCLUDED_CONTAINER = "bulkscan";
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EnvelopeSummaryMapper mapper;
    private final EnvelopeCountSummaryMapper summaryMapper;
    private final ServiceConfiguration serviceConfiguration;

    public ReportRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        EnvelopeSummaryMapper mapper,
        EnvelopeCountSummaryMapper summaryMapper,
        ServiceConfiguration serviceConfiguration
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
        this.summaryMapper = summaryMapper;
        this.serviceConfiguration = serviceConfiguration;
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

    public List<EnvelopeCountSummaryReportItem> getReportFor(LocalDate date) {
        var envelopeCountSummaryReportItemsList = jdbcTemplate.query(
            "SELECT"
                + "  container AS Container,"
                + "  date(created_at) AS date,"
                + "  count(*) AS received,"
                + "  SUM(CASE WHEN Envelopes.status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected"
                + "  FROM"
                + "  Envelopes"
                + "  GROUP BY container, date(created_at)"
                + "  HAVING date(created_at) = :date",
            new MapSqlParameterSource()
                .addValue("date", date),
            this.summaryMapper
        );
        //extract container names
        var containerNames = extractContainerNames(envelopeCountSummaryReportItemsList);
        //add EnvelopeCountSummaryReportItems for containers with no envelopes
        updateEnvelopeCountSummaryReportItemsList(containerNames, envelopeCountSummaryReportItemsList, date);
        return envelopeCountSummaryReportItemsList;
    }

    private List<String> extractContainerNames(List<EnvelopeCountSummaryReportItem> items) {
        List<String> containerNames = new ArrayList<>();
        items.stream().forEach(i -> containerNames.add(i.container));
        return containerNames;
    }

    private void updateEnvelopeCountSummaryReportItemsList(
        List<String> containerNames,
        List<EnvelopeCountSummaryReportItem> items,
        LocalDate date
    ) {
        var zeroEnvelopeContainers = serviceConfiguration.getSourceContainers();
        for (var cont : zeroEnvelopeContainers) {
            if (!containerNames.contains(cont)) {
                items.add(new EnvelopeCountSummaryReportItem(0, 0, cont, date));
            }
        }
    }
}
