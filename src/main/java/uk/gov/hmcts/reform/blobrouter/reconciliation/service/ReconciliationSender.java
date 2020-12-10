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

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ReconciliationSender {

    private static final Logger logger = getLogger(ReconciliationSender.class);

    private static final String NO_ERROR_SUBJECT_FORMAT = "[NO ERROR] %s Scanning Reconciliation";
    private static final String MISMATCH_SUBJECT_FORMAT = "[MISMATCH] %s Scanning Reconciliation";
    private static final String EMPTY_BODY = "";

    private final MessageSender emailSender;

    private final ReconciliationCsvWriter reconciliationCsvWriter;
    private final String mailFrom;
    private final String[] mailRecipients;

    private static final String ATTACHMENT_SUMMARY_PREFIX = "Summary-Report-";
    private static final String ATTACHMENT_DETAILED_PREFIX = "Detailed-report-";
    private static final String ATTACHMENT_SUFFIX = ".csv";

    public ReconciliationSender(
        MessageSender emailSender,
        ReconciliationCsvWriter reconciliationCsvWriter,
        @Value("${reconciliation.report.mail-from}") String mailFrom,
        @Value("${reconciliation.report.mail-recipients}") String[] mailRecipients
    ) {
        this.emailSender = emailSender;
        this.reconciliationCsvWriter = reconciliationCsvWriter;
        this.mailFrom = mailFrom;
        this.mailRecipients = mailRecipients;
    }

    public void sendReconciliationReport(
        LocalDate date,
        TargetStorageAccount account,
        SummaryReport summaryReport,
        ReconciliationReportResponse detailedReport
    ) throws IOException, SendEmailException {
        Map<String, File> attachments = new HashMap<>();

        logger.info("Sending email with reconciliation report for {}, {}", account, date);

        if (isThereAnyDiscrepancy(summaryReport, detailedReport)) {
            if (isSummaryReportNotEmpty(summaryReport)) {
                File file = reconciliationCsvWriter.writeSummaryReconciliationToCsv(summaryReport);
                attachments.put(
                    getReportAttachmentName(ATTACHMENT_SUMMARY_PREFIX, date),
                    file
                );
            }

            if (isDetailedReportNotNullOrEmpty(detailedReport)) {
                File detailedReportFile = reconciliationCsvWriter
                    .writeDetailedReconciliationToCsv(detailedReport);
                attachments.put(
                    getReportAttachmentName(ATTACHMENT_DETAILED_PREFIX, date),
                    detailedReportFile
                );
            }

            emailSender.sendMessageWithAttachments(
                String.format(MISMATCH_SUBJECT_FORMAT, account),
                EMPTY_BODY,
                mailFrom,
                mailRecipients,
                attachments
            );

            logger.info("Email with reconciliation report has been sent for {}, {}", account, date);
        } else {
            emailSender.sendMessageWithAttachments(
                String.format(NO_ERROR_SUBJECT_FORMAT, account),
                EMPTY_BODY,
                mailFrom,
                mailRecipients,
                attachments
            );

            logger.info(
                "Email with no reconciliation report has been sent for {}, {} "
                            + "because there are no discrepancies",
                        account,
                        date
            );
        }
    }

    private boolean isThereAnyDiscrepancy(
        SummaryReport summaryReport,
        ReconciliationReportResponse detailedReport
    ) {
        return isSummaryReportNotEmpty(summaryReport)
            || isDetailedReportNotNullOrEmpty(detailedReport);
    }

    private boolean isSummaryReportNotEmpty(SummaryReport summaryReport) {
        return !CollectionUtils.isEmpty(summaryReport.receivedButNotReported)
            || !CollectionUtils.isEmpty(summaryReport.reportedButNotReceived);
    }

    private boolean isDetailedReportNotNullOrEmpty(ReconciliationReportResponse detailedReport) {
        return detailedReport != null && !CollectionUtils.isEmpty(detailedReport.items);
    }

    private String getReportAttachmentName(String attachmentPrefix, LocalDate reportDate) {
        return attachmentPrefix + reportDate + ATTACHMENT_SUFFIX;
    }
}
