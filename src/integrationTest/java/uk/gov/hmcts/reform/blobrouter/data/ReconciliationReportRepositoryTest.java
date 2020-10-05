package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.config.TestClockProvider;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.NewReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.SupplierStatementRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.NewEnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.util.TimeZones;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.validation.ClockProvider;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
@Import(TestClockProvider.class)
public class ReconciliationReportRepositoryTest {

    private static final String ACCOUNT = "account";
    private static final String VERSION = "v1";
    private static final NewEnvelopeSupplierStatement NEW_STATEMENT = new NewEnvelopeSupplierStatement(
        now(),
        "{\"content\":\"some_content\"}",
        "supplier version"
    );

    @Autowired private SupplierStatementRepository statementRepo;
    @Autowired private ReconciliationReportRepository reportRepo;
    @Autowired private ClockProvider clockProvider;
    @Autowired private DbHelper dbHelper;

    @BeforeEach
    void setup() {
        //if no stopped instant is provided the current time will be used
        TestClockProvider.stoppedInstant = null;
    }
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
                "{ \"report\": \"detailed_report\" }",
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
            assertThat(r.summaryContent).isEqualTo(report.summaryContent);
            assertThat(r.detailedContent).isEqualTo(report.detailedContent);
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
                "{  \"report\": \"detailed_report\" }",
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
        Optional<ReconciliationReport> report = reportRepo.getLatestReconciliationReport(now(), ACCOUNT);

        // then
        assertThat(report).isEmpty();
    }

    @Test
    void should_not_find_anything_when_conditions_do_not_match() {
        // given
        saveNewReportAndGetId("{}", "{ \"x\": 123 }", now().minusDays(1));

        // when
        Optional<ReconciliationReport> report = reportRepo.getLatestReconciliationReport(now(), ACCOUNT);

        // then
        assertThat(report).isEmpty();
    }

    @Test
    void should_find_a_report_when_conditions_are_met() {
        // given
        var expectedSummaryContent = "{}";
        var expectedDetailedContent = "{ \"x\": 123 }";
        var id = saveNewReportAndGetId(expectedSummaryContent, expectedDetailedContent, now().minusDays(1));

        // when
        Optional<ReconciliationReport> report = reportRepo.getLatestReconciliationReport(now().minusDays(1), ACCOUNT);

        // then
        assertThat(report)
            .isNotEmpty()
            .get()
            .satisfies(actualReport -> {
                assertThat(actualReport.id).isEqualTo(id);
                assertThat(actualReport.summaryContent).isEqualTo(expectedSummaryContent);
                assertThat(actualReport.detailedContent).isEqualTo(expectedDetailedContent);
                assertThat(actualReport.contentTypeVersion).isEqualTo(VERSION);
            });
    }

    @Test
    void should_find_only_latest_report_when_conditions_are_met() {
        // given
        saveNewReportAndGetId("{}", "{ \"x\": 983 }", now().minusDays(1));
        var expectedReportContent = "[]";
        var expectedDetailedContent = "{ \"x\": \"123x\" }";
        var id = saveNewReportAndGetId(expectedReportContent, expectedDetailedContent, now().minusDays(1));

        // latest but different report date
        saveNewReportAndGetId("{ \"qx\": \"6fff3x\" }", "{ \"x1\": 11983 }", now());

        // when
        Optional<ReconciliationReport> report = reportRepo.getLatestReconciliationReport(now().minusDays(1), ACCOUNT);

        // then
        assertThat(report)
            .isNotEmpty()
            .get()
            .satisfies(actualReport -> {
                assertThat(actualReport.id).isEqualTo(id);
                assertThat(actualReport.summaryContent).isEqualTo(expectedReportContent);
                assertThat(actualReport.detailedContent).isEqualTo(expectedDetailedContent);
                assertThat(actualReport.contentTypeVersion).isEqualTo(VERSION);
            });
    }

    @Test
    void should_find_latest_report_by_datetime_even_if_was_generated_on_another_day() {
        // given
        // original report was generated yesterday
        TestClockProvider.stoppedInstant = ZonedDateTime.now(TimeZones.EUROPE_LONDON_ZONE_ID).minusDays(1).toInstant();
        saveNewReportAndGetId("{}", "{ \"x\": 983 }", now().minusDays(1));

        // newest supplier statement by date new report was regenerated today
        TestClockProvider.stoppedInstant = ZonedDateTime.now(TimeZones.EUROPE_LONDON_ZONE_ID).toInstant();
        var newReport = saveNewReportAndGetId("{}", "{ \"x\": 666 }", now().minusDays(1));

        // there is existing report, which is no longer relevant
        TestClockProvider.stoppedInstant = ZonedDateTime.now(TimeZones.EUROPE_LONDON_ZONE_ID).minusMinutes(5).toInstant();
        saveNewReportAndGetId("{}", "{ \"x\": 666 }", now().minusDays(1));

        // when
        Optional<ReconciliationReport> report = reportRepo.getLatestReconciliationReport(now().minusDays(1), ACCOUNT);

        // then
        assertThat(report)
            .isNotEmpty()
            .get()
            .satisfies(actualReport -> assertThat(actualReport.id).isEqualTo(newReport));
    }

    private UUID saveNewReportAndGetId(String summaryContent, String detailedContent, LocalDate reportDate) {
        try {
            var statementId = statementRepo.save(
                new NewEnvelopeSupplierStatement(
                    reportDate,
                    "{\"content\":\"some_content\"}",
                    "supplier version"
                )
            );
            var report = new NewReconciliationReport(statementId, ACCOUNT, summaryContent, detailedContent, VERSION);

            return reportRepo.save(report);
        } catch (SQLException exception) {
            throw new RuntimeException("Could not save reports for testing", exception);
        }
    }

    @Test
    void should_only_update_detailed_content_when_updateDetailedContent_called() throws SQLException {
        // given
        var statementId = statementRepo.save(NEW_STATEMENT);

        var existingReport = new NewReconciliationReport(
            statementId,
            ACCOUNT,
            "{ \"x\": 123 }",
            "{}",
            VERSION
        );

        var reportId = reportRepo.save(existingReport);
        String expectedContent = "{ \"report\": \"detailed_report\" }";

        reportRepo.updateDetailedContent(reportId, expectedContent);

        // when
        Optional<ReconciliationReport> reportOption = reportRepo.findById(reportId);

        assertThat(reportOption).isNotEmpty();
        var updatedReport = reportOption.get();

        // then
        assertThat(updatedReport.detailedContent).isEqualTo(expectedContent);

        assertThat(existingReport)
            .usingRecursiveComparison()
            .ignoringFields("detailedContent")
            .isEqualTo(updatedReport);
    }


    @Test
    void should_throw_exception_if_invalid_json_is_passed_to_updateDetailedContent() throws Exception {
        // given
        var statementId = statementRepo.save(NEW_STATEMENT);

        var existingReport = new NewReconciliationReport(
            statementId,
            ACCOUNT,
            "{ \"x\": 123 }",
            "{}",
            VERSION
        );

        var reportId = reportRepo.save(existingReport);
        String invalidContent = "{ \"report\": detailed_report }";

        // when
        var exc = catchThrowable(() -> reportRepo.updateDetailedContent(reportId, invalidContent));

        // then
        assertThat(exc).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(exc.getMessage()).contains("invalid input syntax for type json");
    }

    @Test
    void should_return_list_of_reports_by_supplier_statement_id_findByStatementId()
        throws SQLException {
        // given
        var statementId = statementRepo.save(NEW_STATEMENT);
        var reportId1 = reportRepo
            .save(new NewReconciliationReport(statementId, ACCOUNT, "{ \"x\": 123 }", "{}", VERSION));

        var existingReport1 =
            new ReconciliationReport(
                reportId1,
                statementId,
                ACCOUNT,
                "{ \"x\": 123 }",
                "{}",
                VERSION,
                null,
                LocalDateTime.now()
            );

        var reportId2 = reportRepo
            .save(new NewReconciliationReport(statementId, "account3", "{}", "{}", VERSION));
        var existingReport2 =
            new ReconciliationReport(
                reportId2,
                statementId,
                "account3",
                "{}",
                "{}",
                VERSION,
                null,
                LocalDateTime.now()
            );

        // when
        var reportList = reportRepo.findByStatementId(statementId);

        // then
        assertThat(reportList.size()).isEqualTo(2);
        assertThat(reportList)
            .usingRecursiveComparison()
            .ignoringFields("createdAt").isEqualTo(List.of(existingReport1, existingReport2));
    }

    @Test
    void should_only_update_sent_at_when_updateSentAt_called() throws SQLException {
        // given
        var statementId = statementRepo.save(NEW_STATEMENT);
        var reportId = reportRepo.save(
            new NewReconciliationReport(
                statementId,
                ACCOUNT,
                "{ \"x\": 123 }",
                "{}",
                VERSION
            )
        );

        var start = LocalDateTime.now(clockProvider.getClock());

        var reportBeforeUpdate = reportRepo.findById(reportId).get();
        assertThat(reportBeforeUpdate.sentAt).isNull();

        // when
        reportRepo.updateSentAt(reportId);
        var finish = LocalDateTime.now(clockProvider.getClock());

        // then
        var updatedReport = reportRepo.findById(reportId).get();
        assertThat(updatedReport.sentAt).isAfter(start);
        assertThat(updatedReport.sentAt).isBefore(finish);
        assertThat(reportBeforeUpdate)
            .usingRecursiveComparison()
            .ignoringFields("sentAt")
            .isEqualTo(updatedReport);
    }

    @Test
    void should_find_report_when_report_find_for_given_date_by_findByDate() throws SQLException {
        // given
        LocalDate date = LocalDate.now();

        var statementId = statementRepo.save(NEW_STATEMENT);
        var reportId = reportRepo.save(
            new NewReconciliationReport(
                statementId,
                ACCOUNT,
                "{ \"x\": 123 }",
                "{}",
                VERSION
            )
        );
        // when
        var reportList = reportRepo.findByDate(date);

        // then
        assertThat(reportList.size()).isEqualTo(1);
        var report = reportList.get(0);
        assertThat(report.createdAt.toLocalDate()).isEqualTo(date);
        assertThat(report.detailedContent).isEqualTo("{}");
        assertThat(report.summaryContent).isEqualTo("{ \"x\": 123 }");
        assertThat(report.sentAt).isNull();
        assertThat(report.id).isEqualTo(reportId);
        assertThat(report.account).isEqualTo(ACCOUNT);
        assertThat(report.contentTypeVersion).isEqualTo(VERSION);
        assertThat(report.supplierStatementId).isEqualTo(statementId);
    }

    @Test
    void should_find_reports_genarated_across_two_days_for_given_date_by_findByDate() {
        // given
        LocalDate yesterday = LocalDate.now().minusDays(1);
        // first report was generated yesterday
        TestClockProvider.stoppedInstant = ZonedDateTime.now(TimeZones.EUROPE_LONDON_ZONE_ID).minusDays(1).toInstant();
        UUID firstReportId = saveNewReportAndGetId("{ \"x\": 123 }", null, yesterday);

        //another report was generated today (for yesterday)
        TestClockProvider.stoppedInstant = ZonedDateTime.now(TimeZones.EUROPE_LONDON_ZONE_ID).toInstant();
        UUID secondReportId = saveNewReportAndGetId("{ \"x\": 666 }", null, yesterday);

        // when
        var reportList = reportRepo.findByDate(yesterday);

        // then
        assertThat(reportList.size()).isEqualTo(2);
        assertThat(reportList)
            .extracting("id")
            .containsExactlyInAnyOrder(firstReportId, secondReportId);
    }

    @Test
    void should_return_empty_list_when_no_report_for_given_date_by_findByDate() throws SQLException {
        // given
        LocalDate date = LocalDate.of(2020, 9, 10);

        var statementId = statementRepo.save(NEW_STATEMENT);
        var reportId = reportRepo.save(
            new NewReconciliationReport(
                statementId,
                ACCOUNT,
                "{}",
                "{}",
                VERSION
            )
        );

        assertThat(reportRepo.findById(reportId))
            .isNotEmpty()
            .get()
            .extracting("createdAt")
            .usingComparator((a, b) -> ((LocalDateTime) a).toLocalDate().compareTo((LocalDate) b))
            .isEqualTo(LocalDate.now());
        // when
        var reportList = reportRepo.findByDate(date);
        // then
        assertThat(reportList.size()).isEqualTo(0);
    }

}
