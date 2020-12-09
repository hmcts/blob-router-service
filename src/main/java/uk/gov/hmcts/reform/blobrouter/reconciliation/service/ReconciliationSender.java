package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationReportResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.services.email.MessageSender;
import uk.gov.hmcts.reform.blobrouter.services.email.SendEmailException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ReconciliationSender {

    private static final Logger logger = getLogger(ReconciliationSender.class);

    private final MessageSender emailSender;

    private final ReconciliationCsvWriter reconciliationCsvWriter;
    private final String mailFrom;
    private final String[] mailRecipients;
    private final boolean skipEmptyReports;

    private static final String ATTACHMENT_SUMMARY_PREFIX = "Summary-Report-";
    private static final String ATTACHMENT_DETAILED_PREFIX = "Detailed-report-";
    private static final String ATTACHMENT_SUFFIX = ".csv";

    public ReconciliationSender(
        MessageSender emailSender,
        ReconciliationCsvWriter reconciliationCsvWriter,
        @Value("${reconciliation.report.mail-from}") String mailFrom,
        @Value("${reconciliation.report.mail-recipients}") String[] mailRecipients,
        @Value("${reconciliation.report.skip-empty}") boolean skipEmptyReports
    ) {
        this.emailSender = emailSender;
        this.reconciliationCsvWriter = reconciliationCsvWriter;
        this.mailFrom = mailFrom;
        this.mailRecipients = mailRecipients;
        this.skipEmptyReports = skipEmptyReports;
    }

    public void sendReconciliationReport(
        LocalDate date,
        TargetStorageAccount account,
        SummaryReport summaryReport,
        ReconciliationReportResponse detailedReport
    ) throws IOException, SendEmailException {
        Map<String, File> attachments = new HashMap<>();

        if (willReportsBeAttached(summaryReport, detailedReport)) {
            File file = reconciliationCsvWriter.writeSummaryReconciliationToCsv(summaryReport);
            attachments.put(
                getReportAttachmentName(ATTACHMENT_SUMMARY_PREFIX, date),
                file
            );

            if (detailedReport != null) {
                File detailedReportFile = reconciliationCsvWriter
                    .writeDetailedReconciliationToCsv(detailedReport);
                attachments.put(
                    getReportAttachmentName(ATTACHMENT_DETAILED_PREFIX, date),
                    detailedReportFile
                );
            }

            emailSender.sendMessageWithAttachments(
                createTitle(account, summaryReport, detailedReport),
                "",
                mailFrom,
                mailRecipients,
                attachments
            );
        } else {
            logger.info("");
        }
    }

    private boolean willReportsBeAttached(SummaryReport summaryReport, ReconciliationReportResponse detailedReport) {
        return !skipEmptyReports
            || !CollectionUtils.isEmpty(summaryReport.receivedButNotReported)
            || !CollectionUtils.isEmpty(summaryReport.reportedButNotReceived)
            || isDetailedReportNotNullOrEmpty(detailedReport);
    }

    private boolean isDetailedReportNotNullOrEmpty(ReconciliationReportResponse detailedReport) {
        return detailedReport != null && !CollectionUtils.isEmpty(detailedReport.items);
    }

    private String createTitle(
        TargetStorageAccount account,
        SummaryReport summaryReport,
        ReconciliationReportResponse detailedReport
    ) {
        return (CollectionUtils.isEmpty(summaryReport.receivedButNotReported)
            && CollectionUtils.isEmpty(summaryReport.reportedButNotReceived))
            && noMissMatchInDetailedReport(detailedReport)
            ? account.name() + " Scanning Reconciliation NO ERROR"
            : account.name() + " Scanning Reconciliation MISMATCH";
    }

    private boolean noMissMatchInDetailedReport(ReconciliationReportResponse detailedReport) {
        return Objects.isNull(detailedReport) || CollectionUtils.isEmpty(detailedReport.items);
    }

    private String getReportAttachmentName(String attachmentPrefix, LocalDate reportDate) {
        return attachmentPrefix + reportDate + ATTACHMENT_SUFFIX;
    }
}
