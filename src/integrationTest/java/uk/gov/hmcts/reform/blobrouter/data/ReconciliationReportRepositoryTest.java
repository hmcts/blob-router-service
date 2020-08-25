package uk.gov.hmcts.reform.blobrouter.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.NewReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationContent;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.SupplierStatementRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.NewEnvelopeSupplierStatement;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.validation.ClockProvider;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
public class ReconciliationReportRepositoryTest {

    private static final String ACCOUNT = "account";
    private static final String VERSION = "v1";
    private static final NewEnvelopeSupplierStatement NEW_STATEMENT = new NewEnvelopeSupplierStatement(
        now(),
        "{\"content\":\"some_content\"}",
        "supplier version"
    );

    @Autowired SupplierStatementRepository statementRepo;
    @Autowired ReconciliationReportRepository reportRepo;
    @Autowired ObjectMapper objectMapper;
    @Autowired ClockProvider clockProvider;
    @Autowired DbHelper dbHelper;

    @AfterEach
    void tearDown() {
        dbHelper.deleteAll();
    }

    @Test
    void should_save_and_find_report() throws Exception {
        // given
        UUID statementId = statementRepo.save(new NewEnvelopeSupplierStatement(LocalDate.now(), "{}", "v1.0"));

        var report =
            new NewReconciliationReport(
                statementId,
                "account",
                "{ \"x\": 123 }",
                "v1.0"
            );

        var start = LocalDateTime.now(clockProvider.getClock());

        // when
        UUID id = reportRepo.save(report);
        var reportInDb = reportRepo.findById(id);

        var finish = LocalDateTime.now(clockProvider.getClock());

        // then
        assertThat(reportInDb).hasValueSatisfying(r -> {
            assertThat(r.id).isEqualTo(id);
            assertThat(r.supplierStatementId).isEqualTo(report.supplierStatementId);
            assertThat(r.account).isEqualTo(report.account);
            assertThat(r.content).isEqualTo(report.content);
            assertThat(r.contentTypeVersion).isEqualTo(report.contentTypeVersion);
            assertThat(r.createdAt).isAfter(start);
            assertThat(r.createdAt).isBefore(finish);
            assertThat(r.sentAt).isNull();
        });
    }

    @Test
    void should_throw_exception_if_invalid_json_is_passed() throws Exception {
        // given
        UUID statementId = statementRepo.save(new NewEnvelopeSupplierStatement(LocalDate.now(), "{}", "v1.0"));

        var report =
            new NewReconciliationReport(
                statementId,
                "account",
                "*A(DSOSpasodi",
                "v1.0"
            );

        // when
        var exc = catchThrowable(() -> reportRepo.save(report));

        // then
        assertThat(exc).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(exc.getMessage()).contains("invalid input syntax for type json");
    }

    @Test
    void should_throw_exception_if_referenced_statement_does_not_exist() throws Exception {
        // given
        var notExistingStatementId = UUID.randomUUID();

        var report =
            new NewReconciliationReport(
                notExistingStatementId,
                "account",
                "{ \"x\": 123 }",
                "v1.0"
            );

        // when
        var exc = catchThrowable(() -> reportRepo.save(report));

        // then
        assertThat(exc).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(exc.getMessage()).contains("violates foreign key constraint");
    }

    @Test
    void should_return_empty_optional_if_report_does_not_exist() {
        // given
        UUID uuid = UUID.randomUUID();

        // when
        Optional<ReconciliationReport> report = reportRepo.findById(uuid);

        // then
        assertThat(report).isEmpty();
    }

    @Test
    void should_not_find_anything_when_db_is_empty() {
        // when
        Optional<ReconciliationContent> report = reportRepo.getLatestReconciliationReport(now(), ACCOUNT);

        // then
        assertThat(report).isEmpty();
    }

    @Test
    void should_not_find_anything_when_conditions_do_not_match() {
        // given
        saveNewReportsAndGetLastId("{}");

        // when
        Optional<ReconciliationContent> report = reportRepo.getLatestReconciliationReport(now().minusDays(1), ACCOUNT);

        // then
        assertThat(report).isEmpty();
    }

    @Test
    void should_find_a_report_when_conditions_are_met() {
        // given
        var expectedReportContent = "{}";
        var id = saveNewReportsAndGetLastId(expectedReportContent);

        // when
        Optional<ReconciliationContent> report = reportRepo.getLatestReconciliationReport(now(), ACCOUNT);

        // then
        assertThat(report)
            .isNotEmpty()
            .get()
            .usingRecursiveComparison()
            .isEqualTo(new ReconciliationContent(id, expectedReportContent, VERSION));
    }

    @Test
    void should_find_only_latest_report_when_conditions_are_met() {
        // given
        var expectedReportContent = "[]";
        var id = saveNewReportsAndGetLastId("{}", expectedReportContent);

        // when
        Optional<ReconciliationContent> report = reportRepo.getLatestReconciliationReport(now(), ACCOUNT);

        // then
        assertThat(report)
            .isNotEmpty()
            .get()
            .usingRecursiveComparison()
            .isEqualTo(new ReconciliationContent(id, expectedReportContent, VERSION));
    }

    // Only last ID/report matters
    private UUID saveNewReportsAndGetLastId(String... reportContents) {
        UUID lastId = null;

        try {
            for (String contents : reportContents) {
                var statementId = statementRepo.save(NEW_STATEMENT);
                var report = new NewReconciliationReport(statementId, ACCOUNT, contents, VERSION);
                lastId = reportRepo.save(report);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save reports for testing", exception);
        }

        return lastId;
    }
}
