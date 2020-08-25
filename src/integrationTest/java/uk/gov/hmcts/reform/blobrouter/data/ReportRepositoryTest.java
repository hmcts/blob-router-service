package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.NewReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.SupplierStatementRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.NewEnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.data.reports.ReconciliationReportContent;
import uk.gov.hmcts.reform.blobrouter.data.reports.ReportRepository;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
class ReportRepositoryTest {

    private static final String ACCOUNT = "account";
    private static final String VERSION = "v1";
    private static final NewEnvelopeSupplierStatement NEW_STATEMENT = new NewEnvelopeSupplierStatement(
        now(),
        "{\"content\":\"some_content\"}",
        "supplier version"
    );

    @Autowired private ReconciliationReportRepository reconciliationRepository;
    @Autowired private SupplierStatementRepository supplierStatementRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private DbHelper dbHelper;

    @AfterEach
    void tearDown() {
        dbHelper.deleteAll();
    }

    @Test
    void should_not_find_anything_when_db_is_empty() {
        // when
        Optional<ReconciliationReportContent> report = reportRepository.getReconciliationReport(now(), ACCOUNT);

        // then
        assertThat(report).isEmpty();
    }

    @Test
    void should_not_find_anything_when_conditions_do_not_match() {
        // given
        saveNewReportsAndGetLastId("{}");

        // when
        Optional<ReconciliationReportContent> report = reportRepository
            .getReconciliationReport(now().minusDays(1), ACCOUNT);

        // then
        assertThat(report).isEmpty();
    }

    @Test
    void should_find_a_report_when_conditions_are_met() {
        // given
        var expectedReportContent = "{}";
        var id = saveNewReportsAndGetLastId(expectedReportContent);

        // when
        Optional<ReconciliationReportContent> report = reportRepository.getReconciliationReport(now(), ACCOUNT);

        // then
        assertThat(report)
            .isNotEmpty()
            .get()
            .usingRecursiveComparison()
            .isEqualTo(new ReconciliationReportContent(id, expectedReportContent, VERSION));
    }

    @Test
    void should_find_only_latest_report_when_conditions_are_met() {
        // given
        var expectedReportContent = "[]";
        var id = saveNewReportsAndGetLastId("{}", expectedReportContent);

        // when
        Optional<ReconciliationReportContent> report = reportRepository.getReconciliationReport(now(), ACCOUNT);

        // then
        assertThat(report)
            .isNotEmpty()
            .get()
            .usingRecursiveComparison()
            .isEqualTo(new ReconciliationReportContent(id, expectedReportContent, VERSION));
    }

    // Only last ID/report matters
    private UUID saveNewReportsAndGetLastId(String... reportContents) {
        UUID lastId = null;

        try {
            for (String contents : reportContents) {
                var statementId = supplierStatementRepository.save(NEW_STATEMENT);
                var report = new NewReconciliationReport(statementId, ACCOUNT, contents, VERSION);
                lastId = reconciliationRepository.save(report);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save reports for testing", exception);
        }

        return lastId;
    }
}
