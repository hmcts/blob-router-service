package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.DiscrepancyItem;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationReportResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReportItem;
import uk.gov.hmcts.reform.blobrouter.services.email.EmailSender;
import uk.gov.hmcts.reform.blobrouter.services.email.SendEmailException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CFT;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CRIME;

@ExtendWith(MockitoExtension.class)
class ReconciliationSenderTest {
    private ReconciliationSender reconciliationSender;

    @Mock
    private EmailSender emailSender;

    @Mock
    private ReconciliationCsvWriter reconciliationCsvWriter;

    private static final String mailFrom = "from@f.com";
    private static final String[] mailRecipients = {"r1@d.com"};

    @ParameterizedTest
    @MethodSource("summaryReportTest")
    void should_send_mail_when_just_summary_report_not_empty_or_skip_empty_report_false(
        TargetStorageAccount account,
        SummaryReport summaryReport,
        String title,
        boolean skipEmptyReports
    ) throws IOException, SendEmailException {
        // given
        reconciliationSender = new ReconciliationSender(
            emailSender,
            reconciliationCsvWriter,
            mailFrom,
            mailRecipients,
            skipEmptyReports
        );

        LocalDate date = LocalDate.now();

        var summaryReportFile = mock(File.class);
        given(reconciliationCsvWriter.writeSummaryReconciliationToCsv(summaryReport))
            .willReturn(summaryReportFile);

        // when
        reconciliationSender.sendReconciliationReport(date, account, summaryReport, null);

        // then
        verify(emailSender, times(1))
            .sendMessageWithAttachments(
                eq(title),
                eq(""),
                eq(mailFrom),
                eq(mailRecipients),
                eq(Map.of("Summary-Report-" + date + ".csv", summaryReportFile))
            );
        verifyNoMoreInteractions(emailSender);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void should_send_mail_when_reconciliation_report_has_both_reports_disregarding_value_of_skip_empty_report(
        boolean skipEmptyReports
    ) throws Exception {

        // given
        reconciliationSender = new ReconciliationSender(
            emailSender,
            reconciliationCsvWriter,
            mailFrom,
            mailRecipients,
            skipEmptyReports
        );

        LocalDate date = LocalDate.now();
        var summaryReport = mock(SummaryReport.class);

        var detailedReport = new ReconciliationReportResponse(List.of(mock(DiscrepancyItem.class)));

        var summaryReportFile = mock(File.class);
        given(reconciliationCsvWriter.writeSummaryReconciliationToCsv(summaryReport))
            .willReturn(summaryReportFile);

        var detailedReportFile = mock(File.class);
        given(reconciliationCsvWriter.writeDetailedReconciliationToCsv(detailedReport))
            .willReturn(detailedReportFile);

        // when
        reconciliationSender.sendReconciliationReport(date, CFT, summaryReport, detailedReport);

        // then
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
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void should_send_mail_when_reconciliation_report_has_just_detailed_report_disregarding_value_of_skip_empty_report(
        boolean skipEmptyReports
    ) throws Exception {

        // given
        reconciliationSender = new ReconciliationSender(
            emailSender,
            reconciliationCsvWriter,
            mailFrom,
            mailRecipients,
            skipEmptyReports
        );

        LocalDate date = LocalDate.now();
        var summaryReport = new SummaryReport(
            120,
            120,
            emptyList(),
            emptyList()
        );
        var summaryReportFile = mock(File.class);
        given(reconciliationCsvWriter.writeSummaryReconciliationToCsv(summaryReport))
            .willReturn(summaryReportFile);

        var detailedReport = new ReconciliationReportResponse(List.of(mock(DiscrepancyItem.class)));
        var detailedReportFile = mock(File.class);
        given(reconciliationCsvWriter.writeDetailedReconciliationToCsv(detailedReport))
            .willReturn(detailedReportFile);

        // when
        reconciliationSender.sendReconciliationReport(date, CFT, summaryReport, detailedReport);

        // then
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
    }


    @Test
    void should_not_send_mail_when_summary_report_and_detailed_report_are_empty_and_skip_empty_report_is_true()
        throws Exception {

        // given
        reconciliationSender = new ReconciliationSender(
            emailSender,
            reconciliationCsvWriter,
            mailFrom,
            mailRecipients,
            true
        );

        LocalDate date = LocalDate.now();
        var summaryReport = new SummaryReport(
            120,
            120,
            emptyList(),
            emptyList()
        );

        var detailedReport = new ReconciliationReportResponse(emptyList());

        // when
        reconciliationSender.sendReconciliationReport(date, CFT, summaryReport, detailedReport);

        // then
        verifyNoInteractions(emailSender);
    }

    private static Object[][] summaryReportTest() {
        return new Object[][]{
            new Object[]{
                CFT,
                mock(SummaryReport.class),
                "CFT Scanning Reconciliation NO ERROR",
                false
            },
            new Object[]{
                CRIME,
                new SummaryReport(
                    120,
                    120,
                    List.of(new SummaryReportItem("12312.31312.312.zip", "crime")),
                    emptyList()
                ),
                "CRIME Scanning Reconciliation MISMATCH",
                false
            },
            new Object[]{
                CRIME,
                new SummaryReport(
                    120,
                    120,
                    emptyList(),
                    List.of(new SummaryReportItem("2.31312.312.zip", "crime"))
                ),
                "CRIME Scanning Reconciliation MISMATCH",
                false
            },
            new Object[]{
                CRIME,
                new SummaryReport(
                    120,
                    120,
                    List.of(new SummaryReportItem("12312.31312.312.zip", "crime")),
                    emptyList()
                ),
                "CRIME Scanning Reconciliation MISMATCH",
                true
            },
            new Object[]{
                CRIME,
                new SummaryReport(
                    120,
                    120,
                    emptyList(),
                    List.of(new SummaryReportItem("2.31312.312.zip", "crime"))
                ),
                "CRIME Scanning Reconciliation MISMATCH",
                true
            }
        };
    }
}
