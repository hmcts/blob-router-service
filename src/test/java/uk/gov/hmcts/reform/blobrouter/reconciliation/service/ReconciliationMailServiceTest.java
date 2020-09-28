package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.SupplierStatementRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.DiscrepancyItem;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationReportResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReportItem;
import uk.gov.hmcts.reform.blobrouter.services.email.EmailSender;
import uk.gov.hmcts.reform.blobrouter.services.email.SendEmailException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CFT;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CRIME;


@ExtendWith(MockitoExtension.class)
class ReconciliationMailServiceTest {

    @Mock
    private SupplierStatementRepository repository;

    @Mock
    private ReconciliationReportRepository reconciliationReportRepository;

    @Mock
    private ReconciliationCsvWriter reconciliationCsvWriter;

    @Mock
    private EmailSender emailSender;

    @Mock
    private ObjectMapper objectMapper;

    private ReconciliationMailService service;

    private static final String mailFrom = "from@f.com";
    private static final String[] mailRecipients = {"r1@d.com"};
    private static final List<TargetStorageAccount> AVAILABLE_ACCOUNTS =
        List.of(CFT, CRIME);

    private static final String BULKSCAN_NO_REPORT = "CFT Scanning Reconciliation NO REPORT RECEIVED";
    private static final String CRIME_NO_REPORT = "CRIME Scanning Reconciliation NO REPORT RECEIVED";

    @BeforeEach
    void setUp() {
        service = new ReconciliationMailService(
            repository,
            reconciliationReportRepository,
            reconciliationCsvWriter,
            emailSender,
            objectMapper,
            mailFrom,
            mailRecipients
        );
    }

    @Test
    void should_send_mail_when_the_supplier_statement_is_missing() throws SendEmailException {

        // given
        LocalDate date = LocalDate.now();
        given(repository.findLatestByDate(date)).willReturn(Optional.empty());

        // when
        service.process(date, AVAILABLE_ACCOUNTS);

        // then
        verify(repository).findLatestByDate(date);
        verifyNoMoreInteractions(repository);
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailSender, times(2))
            .sendMessageWithAttachments(
                titleCaptor.capture(),
                eq("No Report received for " + date),
                eq(mailFrom),
                eq(mailRecipients),
                eq(Collections.emptyMap())
            );

        var titles = titleCaptor.getAllValues();
        assertThat(titles).containsAll(List.of(BULKSCAN_NO_REPORT, CRIME_NO_REPORT));
        verifyNoMoreInteractions(emailSender);
    }

    @Test
    void should_not_send_mail_when_reconciliation_report_is_missing() {

        // given
        LocalDate date = LocalDate.now();
        given(repository.findLatestByDate(date))
            .willReturn(Optional.of(mock(EnvelopeSupplierStatement.class)));

        given(reconciliationReportRepository.getLatestReconciliationReport(any(), any()))
            .willReturn(Optional.empty());

        // when
        service.process(date, AVAILABLE_ACCOUNTS);

        // then
        verify(repository).findLatestByDate(date);
        verifyNoMoreInteractions(repository);
        ArgumentCaptor<String> accountCaptor = ArgumentCaptor.forClass(String.class);

        verify(reconciliationReportRepository, times(2))
            .getLatestReconciliationReport(eq(date), accountCaptor.capture());

        var accounts = accountCaptor.getAllValues();
        assertThat(accounts).containsAll(List.of(CFT.name(), CRIME.name()));
        verifyNoMoreInteractions(emailSender);
    }

    @Test
    void should_continue_with_next_account_if_one_gets_exception()
        throws JsonProcessingException, SendEmailException {

        // given
        LocalDate date = LocalDate.now();
        given(repository.findLatestByDate(date))
            .willReturn(Optional.of(mock(EnvelopeSupplierStatement.class)));

        var reconciliationReport = new ReconciliationReport(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "CRIME",
            "{}",
            "{\"a\":1}",
            "1.0",
            null,
            LocalDateTime.now()
        );

        given(reconciliationReportRepository.getLatestReconciliationReport(date, CFT.name()))
            .willThrow(new RuntimeException("BULKSCAN report failed"));

        given(reconciliationReportRepository.getLatestReconciliationReport(date, CRIME.name()))
            .willReturn(Optional.of(reconciliationReport));


        given(objectMapper.readValue(reconciliationReport.summaryContent, SummaryReport.class))
            .willReturn(mock(SummaryReport.class));

        // when
        service.process(date, AVAILABLE_ACCOUNTS);

        // then
        verify(repository).findLatestByDate(date);
        verifyNoMoreInteractions(repository);
        ArgumentCaptor<String> accountCaptor = ArgumentCaptor.forClass(String.class);

        verify(reconciliationReportRepository, times(2))
            .getLatestReconciliationReport(eq(date), accountCaptor.capture());

        var accounts = accountCaptor.getAllValues();
        assertThat(accounts).containsAll(List.of(CFT.name(), CRIME.name()));
        verify(emailSender, times(1))
            .sendMessageWithAttachments(anyString(), anyString(),anyString(), any(), any());
        verifyNoMoreInteractions(emailSender);
        verify(reconciliationReportRepository).updateSentAt(reconciliationReport.id);
        verifyNoMoreInteractions(reconciliationReportRepository);
    }

    @Test
    void should_not_send_mail_when_reconciliation_report_summary_field_is_empty() {
        // given
        LocalDate date = LocalDate.now();
        given(repository.findLatestByDate(date))
            .willReturn(Optional.of(mock(EnvelopeSupplierStatement.class)));

        given(reconciliationReportRepository.getLatestReconciliationReport(any(), any()))
            .willReturn(Optional.of(mock(ReconciliationReport.class)));

        // when
        service.process(date, AVAILABLE_ACCOUNTS);

        // then
        verify(repository).findLatestByDate(date);
        verifyNoMoreInteractions(repository);
        ArgumentCaptor<String> accountCaptor = ArgumentCaptor.forClass(String.class);
        verify(reconciliationReportRepository, times(2))
            .getLatestReconciliationReport(eq(date), accountCaptor.capture());

        var accounts = accountCaptor.getAllValues();
        assertThat(accounts).containsAll(List.of(CFT.name(), CRIME.name()));
        verifyNoMoreInteractions(emailSender);
    }

    @Test
    void should_not_send_mail_when_reconciliation_report_send_at_field_is_not_empty() {
        // given
        LocalDate date = LocalDate.now();
        given(repository.findLatestByDate(date))
            .willReturn(Optional.of(mock(EnvelopeSupplierStatement.class)));

        var reconciliationReport = new ReconciliationReport(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "BULKSCAN",
            "{}",
            "{}",
            "1.0",
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        given(reconciliationReportRepository.getLatestReconciliationReport(any(), any()))
            .willReturn(Optional.of(reconciliationReport));

        // when
        service.process(date, List.of(CFT));

        // then
        verify(repository).findLatestByDate(date);
        verifyNoMoreInteractions(repository);
        verify(reconciliationReportRepository, times(1))
            .getLatestReconciliationReport(eq(date), eq(CFT.name()));
        verifyNoMoreInteractions(emailSender);
    }

    @ParameterizedTest
    @MethodSource("summaryReportTest")
    void should_send_summary_report_mail_when_reconciliation_report_has_just_summary_report(
        TargetStorageAccount account, ReconciliationReport reconciliationReport,
        SummaryReport summaryReport, String title
    ) throws IOException, SendEmailException {
        // given
        LocalDate date = LocalDate.now();
        given(repository.findLatestByDate(date))
            .willReturn(Optional.of(mock(EnvelopeSupplierStatement.class)));

        given(reconciliationReportRepository.getLatestReconciliationReport(any(), any()))
            .willReturn(Optional.of(reconciliationReport));

        given(objectMapper.readValue(reconciliationReport.summaryContent, SummaryReport.class))
            .willReturn(summaryReport);

        var summaryReportFile = mock(File.class);
        given(reconciliationCsvWriter.writeSummaryReconciliationToCsv(summaryReport))
            .willReturn(summaryReportFile);

        // when
        service.process(date, List.of(account));

        // then
        verify(repository).findLatestByDate(date);
        verifyNoMoreInteractions(repository);
        verify(reconciliationReportRepository, times(1))
            .getLatestReconciliationReport(eq(date), eq(account.name()));
        verify(objectMapper).readValue(anyString(), eq(SummaryReport.class));
        verifyNoMoreInteractions(objectMapper);
        verify(emailSender, times(1))
            .sendMessageWithAttachments(
                eq(title),
                eq(""),
                eq(mailFrom),
                eq(mailRecipients),
                eq(Map.of("Summary-Report-" + date + ".csv", summaryReportFile))
            );
        verifyNoMoreInteractions(emailSender);
        verify(reconciliationReportRepository).updateSentAt(reconciliationReport.id);
    }

    private static Object[][] summaryReportTest() {
        return new Object[][]{
            new Object[]{
                CFT,
                new ReconciliationReport(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "BULKSCAN",
                    "{}",
                    null,
                    "1.0",
                    null,
                    LocalDateTime.now()
                ),
                mock(SummaryReport.class),
                "CFT Scanning Reconciliation NO ERROR"
            },
            new Object[]{
                CRIME,
                new ReconciliationReport(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "CRIME",
                    "{}",
                    null,
                    "1.0",
                    null,
                    LocalDateTime.now()
                ),
                new SummaryReport(
                    120,
                    120,
                    List.of(new SummaryReportItem("12312.31312.312.zip", "crime")),
                    emptyList()
                ),
                "CRIME Scanning Reconciliation MISMATCH"
            },
            new Object[]{
                CRIME,
                new ReconciliationReport(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "CRIME",
                    "{}",
                    null,
                    "1.0",
                    null,
                    LocalDateTime.now()
                ),
                new SummaryReport(
                    120,
                    120,
                    emptyList(),
                    List.of(new SummaryReportItem("2.31312.312.zip", "crime"))
                ),
                "CRIME Scanning Reconciliation MISMATCH"
            }
        };
    }

    @Test
    void should_send_summary_report_and_detailed_report_when_reconciliation_report_has_both_reports()
        throws IOException, SendEmailException {

        // given
        var reconciliationReport = new ReconciliationReport(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "CRIME",
            "{}",
            "{\"a\":1}",
            "1.0",
            null,
            LocalDateTime.now()
        );

        LocalDate date = LocalDate.now();
        given(repository.findLatestByDate(date))
            .willReturn(Optional.of(mock(EnvelopeSupplierStatement.class)));

        given(reconciliationReportRepository.getLatestReconciliationReport(any(), any()))
            .willReturn(Optional.of(reconciliationReport));

        var summaryReport = mock(SummaryReport.class);
        given(objectMapper.readValue(reconciliationReport.summaryContent, SummaryReport.class))
            .willReturn(summaryReport);

        var detailedReport = new ReconciliationReportResponse(List.of(mock(DiscrepancyItem.class)));
        given(objectMapper.readValue(reconciliationReport.detailedContent, ReconciliationReportResponse.class))
            .willReturn(detailedReport);

        var summaryReportFile = mock(File.class);
        given(reconciliationCsvWriter.writeSummaryReconciliationToCsv(summaryReport))
            .willReturn(summaryReportFile);

        var detailedReportFile = mock(File.class);
        given(reconciliationCsvWriter.writeDetailedReconciliationToCsv(detailedReport))
            .willReturn(detailedReportFile);

        // when
        service.process(date, List.of(CFT));

        // then
        verify(repository).findLatestByDate(date);
        verifyNoMoreInteractions(repository);
        verify(reconciliationReportRepository, times(1))
            .getLatestReconciliationReport(eq(date), eq(CFT.name()));
        verify(objectMapper).readValue(anyString(), eq(SummaryReport.class));
        verify(objectMapper).readValue(anyString(), eq(ReconciliationReportResponse.class));
        verifyNoMoreInteractions(objectMapper);
        verify(emailSender, times(1))
            .sendMessageWithAttachments(
                eq("CFT Scanning Reconciliation MISMATCH"),
                eq(""),
                eq(mailFrom),
                eq(mailRecipients),
                eq(Map.of(
                    "Summary-Report-" + date + ".csv", summaryReportFile,
                    "Detailed-report-" + date + ".csv", detailedReportFile
                    )
                )
            );
        verifyNoMoreInteractions(emailSender);
        verify(reconciliationReportRepository).updateSentAt(reconciliationReport.id);
    }

}
