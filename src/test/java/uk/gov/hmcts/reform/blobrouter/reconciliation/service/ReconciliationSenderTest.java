package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import static java.util.Collections.emptyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    private static final LocalDate reportForDate = LocalDate.now();

    @ParameterizedTest
    @MethodSource("summaryReportTest")
    void should_send_mail_when_just_summary_report_not_empty(
        TargetStorageAccount account,
        SummaryReport summaryReport,
        String subject
    ) throws IOException, SendEmailException {
        // given
        reconciliationSender = new ReconciliationSender(
            emailSender,
            reconciliationCsvWriter,
            mailFrom,
            mailRecipients
        );


        var summaryReportFile = mock(File.class);
        given(reconciliationCsvWriter.writeSummaryReconciliationToCsv(summaryReport))
            .willReturn(summaryReportFile);

        // when
        reconciliationSender.sendReconciliationReport(reportForDate, account, summaryReport, null);

        // then
        verify(emailSender, times(1))
            .sendMessageWithAttachments(
                subject,
                "",
                mailFrom,
                mailRecipients,
                Map.of("Summary-Report-" + reportForDate + ".csv", summaryReportFile)
            );
        verifyNoMoreInteractions(emailSender);
    }

    @Test
    void should_send_mail_when_reconciliation_report_has_both_reports()
        throws Exception {

        // given
        reconciliationSender = new ReconciliationSender(
            emailSender,
            reconciliationCsvWriter,
            mailFrom,
            mailRecipients
        );


        var summaryReport = new SummaryReport(
            120,
            120,
            List.of(new SummaryReportItem("12312.31312.312.zip", "crime")),
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
        reconciliationSender.sendReconciliationReport(reportForDate, CFT, summaryReport, detailedReport);

        // then
        verify(emailSender, times(1))
            .sendMessageWithAttachments(
                "[MISMATCH] CFT Scanning Reconciliation " + reportForDate,
                "",
                mailFrom,
                mailRecipients,
                Map.of(
                "Summary-Report-" + reportForDate + ".csv", summaryReportFile,
                "Detailed-report-" + reportForDate + ".csv", detailedReportFile
               )
            );
        verifyNoMoreInteractions(emailSender);
    }

    @Test
    void should_send_mail_with_no_empty_summary_report_when_reconciliation_report_has_just_detailed_report()
        throws Exception {

        // given
        reconciliationSender = new ReconciliationSender(
            emailSender,
            reconciliationCsvWriter,
            mailFrom,
            mailRecipients
        );

        var summaryReport = new SummaryReport(
            120,
            120,
            emptyList(),
            emptyList()
        );

        var detailedReport = new ReconciliationReportResponse(List.of(mock(DiscrepancyItem.class)));
        var detailedReportFile = mock(File.class);
        given(reconciliationCsvWriter.writeDetailedReconciliationToCsv(detailedReport))
            .willReturn(detailedReportFile);

        // when
        reconciliationSender.sendReconciliationReport(reportForDate, CFT, summaryReport, detailedReport);

        // then
        verify(emailSender, times(1))
            .sendMessageWithAttachments(
                "[MISMATCH] CFT Scanning Reconciliation " + reportForDate,
                "",
                mailFrom,
                mailRecipients,
                Map.of(
                    "Detailed-report-" + reportForDate + ".csv", detailedReportFile
                )
            );
        verifyNoMoreInteractions(emailSender);
    }


    @Test
    void should_send_mail_when_summary_report_and_detailed_report_are_empty()
        throws Exception {

        // given
        reconciliationSender = new ReconciliationSender(
            emailSender,
            reconciliationCsvWriter,
            mailFrom,
            mailRecipients
        );

        var summaryReport = new SummaryReport(
            120,
            120,
            emptyList(),
            emptyList()
        );

        var detailedReport = new ReconciliationReportResponse(emptyList());

        // when
        reconciliationSender.sendReconciliationReport(reportForDate, CFT, summaryReport, detailedReport);

        // then
        verify(emailSender, times(1))
            .sendMessageWithAttachments(
                "[NO ERROR] CFT Scanning Reconciliation " + reportForDate,
                "",
                mailFrom,
                mailRecipients,
                emptyMap()
            );

        verifyNoMoreInteractions(emailSender);
    }

    private static Object[][] summaryReportTest() {
        return new Object[][]{
            new Object[]{
                CRIME,
                new SummaryReport(
                    120,
                    120,
                    List.of(new SummaryReportItem("12312.31312.312.zip", "crime")),
                    emptyList()
                ),
                "[MISMATCH] CRIME Scanning Reconciliation " + reportForDate
            },
            new Object[]{
                CRIME,
                new SummaryReport(
                    120,
                    120,
                    emptyList(),
                    List.of(new SummaryReportItem("2.31312.312.zip", "crime"))
                ),
                "[MISMATCH] CRIME Scanning Reconciliation " + reportForDate
            },
            new Object[]{
                CRIME,
                new SummaryReport(
                    120,
                    120,
                    List.of(new SummaryReportItem("12312.31312.312.zip", "crime")),
                    emptyList()
                ),
                "[MISMATCH] CRIME Scanning Reconciliation " + reportForDate
            },
            new Object[]{
                CRIME,
                new SummaryReport(
                    120,
                    120,
                    emptyList(),
                    List.of(new SummaryReportItem("2.31312.312.zip", "crime"))
                ),
                "[MISMATCH] CRIME Scanning Reconciliation " + reportForDate
            }
        };
    }
}
