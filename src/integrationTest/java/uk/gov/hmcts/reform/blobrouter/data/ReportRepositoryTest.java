package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.NewReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reports.ReconciliationReportContent;
import uk.gov.hmcts.reform.blobrouter.data.reports.ReportRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
class ReportRepositoryTest {

    private static final String ACCOUNT = "account";

    @Autowired private ReconciliationReportRepository reconciliationRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private DbHelper dbHelper;

    @AfterEach
    void tearDown() {
        dbHelper.deleteAll();
    }

    @Test
    void should_not_find_anything_when_db_is_empty() {
        // when
        List<ReconciliationReportContent> report = reportRepository.getReconciliationReport(now(), ACCOUNT);

        // then
        assertThat(report).isEmpty();
    }

    @Test
    void should_not_find_anything_when_conditions_do_not_match() {
        // given
        saveNewReports(new NewReconciliationReport(UUID.randomUUID(), ACCOUNT, "{}", "v1"));

        // when
        List<ReconciliationReportContent> report = reportRepository
            .getReconciliationReport(now().minusDays(1), ACCOUNT);

        // then
        assertThat(report).isEmpty();
    }

    @Test
    void should_find_a_report_when_conditions_are_met() {
        // given
        var id = UUID.randomUUID();
        var expectedReport = new NewReconciliationReport(id, ACCOUNT, "{}", "v1");
        saveNewReports(expectedReport);

        // when
        List<ReconciliationReportContent> report = reportRepository.getReconciliationReport(now(), ACCOUNT);

        // then
        assertThat(report)
            .hasSize(1)
            .first()
            .usingRecursiveComparison()
            .isEqualTo(new ReconciliationReportContent(id, expectedReport.content, expectedReport.contentTypeVersion));
    }

    @Test
    void should_find_only_latest_report_when_conditions_are_met() {
        // given
        var id = UUID.randomUUID();
        var skippedReport = new NewReconciliationReport(UUID.randomUUID(), ACCOUNT, "{}", "v1");
        var expectedReport = new NewReconciliationReport(id, ACCOUNT, "[]", "v1");
        saveNewReports(skippedReport, expectedReport);

        // when
        List<ReconciliationReportContent> report = reportRepository.getReconciliationReport(now(), ACCOUNT);

        // then
        assertThat(report)
            .hasSize(1)
            .first()
            .usingRecursiveComparison()
            .isEqualTo(new ReconciliationReportContent(id, expectedReport.content, expectedReport.contentTypeVersion));
    }

    private void saveNewReports(NewReconciliationReport... reports) {
        try {
            for (NewReconciliationReport report : reports) {
                reconciliationRepository.save(report);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save reports for testing", exception);
        }
    }
}
