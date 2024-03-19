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

/**
 * The `ReconciliationSender` class in Java is responsible for sending
 * reconciliation reports via email based on provided data and reports.
 */
@Component
public class ReconciliationSender {

    private static final Logger logger = getLogger(ReconciliationSender.class);

    private static final String NO_ERROR_SUBJECT_FORMAT = "[NO ERROR] %s Scanning Reconciliation %s";
    private static final String MISMATCH_SUBJECT_FORMAT = "[MISMATCH] %s Scanning Reconciliation %s";
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

    /**
     * The `sendReconciliationReport` method sends an email with reconciliation reports based on the provided date,
     * account, summary report, and detailed report.
     *
     * @param date The `date` parameter in the `sendReconciliationReport` method represents the date for which the
     *             reconciliation report is being sent. It is of type `LocalDate`,
     *             which is a date without a time zone in the ISO-8601
     *             calendar system, such as 2022-01-31
     * @param account The `account` parameter in the `sendReconciliationReport` method represents the
     *                target storage account for which the reconciliation report is being sent. It is
     *                used to identify the specific account for which the
     *                reconciliation report is generated and sent via email.
     * @param summaryReport The `sendReconciliationReport` method you provided is responsible for sending
     *                      reconciliation reports via email. It takes in several parameters including the
     *                      `summaryReport` and `detailedReport` which are used to generate the content
     *                      of the reconciliation report.
     * @param detailedReport The `detailedReport` parameter in the `sendReconciliationReport` method is
     *                       of type `ReconciliationReportResponse`. This parameter likely contains
     *                       detailed information about the reconciliation report, such as specific
     *                       discrepancies or detailed transaction data that needs to be included in
     *                       the reconciliation report email.
     */
    public void sendReconciliationReport(
        LocalDate date,
        TargetStorageAccount account,
        SummaryReport summaryReport,
        ReconciliationReportResponse detailedReport
    ) throws IOException, SendEmailException {
        Map<String, File> attachments = new HashMap<>();

        logger.info("Sending email with reconciliation report for {}, {}", account, date);

        if (isAnyReportNotEmpty(summaryReport, detailedReport)) {
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
                String.format(MISMATCH_SUBJECT_FORMAT, account, date.toString()),
                EMPTY_BODY,
                mailFrom,
                mailRecipients,
                attachments
            );

            logger.info("Email with reconciliation report has been sent for {}, {}", account, date);
        } else {
            emailSender.sendMessageWithAttachments(
                String.format(NO_ERROR_SUBJECT_FORMAT, account, date.toString()),
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

    /**
     * The function checks if either the summary report is not empty or the detailed report is not null or empty.
     *
     * @param summaryReport SummaryReport is an object that contains summarized data or information.
     * @param detailedReport The `detailedReport` parameter is of type `ReconciliationReportResponse`, which likely
     *                       contains detailed information related to reconciliation.
     *                       The method `isAnyReportNotEmpty` checks if either the
     *                       `summaryReport` or the `detailedReport` is not empty or null.
     * @return The method is returning a boolean value, which is determined by whether the
     *      summary report is not empty or the detailed report is not null or empty.
     */
    private boolean isAnyReportNotEmpty(
        SummaryReport summaryReport,
        ReconciliationReportResponse detailedReport
    ) {
        return isSummaryReportNotEmpty(summaryReport)
            || isDetailedReportNotNullOrEmpty(detailedReport);
    }

    /**
     * The function checks if either the list of received but not reported
     * items or the list of reported but not received items in a SummaryReport object is not empty.
     *
     * @param summaryReport SummaryReport is a class or data structure that
     *                      contains two lists: receivedButNotReported and reportedButNotReceived.
     *                      The method isSummaryReportNotEmpty checks if either of these lists is not
     *                      empty and returns true in that case.
     * @return The method is returning a boolean value, which is determined by whether the
     *      `receivedButNotReported` or `reportedButNotReceived` lists in the `summaryReport` object are not empty.
     */
    private boolean isSummaryReportNotEmpty(SummaryReport summaryReport) {
        return !CollectionUtils.isEmpty(summaryReport.receivedButNotReported)
            || !CollectionUtils.isEmpty(summaryReport.reportedButNotReceived);
    }

    /**
     * The function checks if a detailed report is not null and its items collection is not empty.
     *
     * @param detailedReport The `isDetailedReportNotNullOrEmpty` method checks if the `detailedReport` parameter is not
     *      null and if the `items` collection inside it is not empty. This method ensures
     *      that the detailed report is both present and contains some items for further processing.
     * @return A boolean value is being returned, indicating whether the detailed report is not null and not empty.
     */
    private boolean isDetailedReportNotNullOrEmpty(ReconciliationReportResponse detailedReport) {
        return detailedReport != null && !CollectionUtils.isEmpty(detailedReport.items);
    }

    /**
     * The function `getReportAttachmentName` concatenates an attachment prefix, report date, and a constant suffix to
     * create a report attachment name.
     *
     * @param attachmentPrefix The `attachmentPrefix` parameter is a String that
     *                         represents the prefix to be added to the report attachment name.
     * @param reportDate The `reportDate` parameter is of type `LocalDate`, which represents a date
     *                   without a time zone in the ISO-8601 calendar system, such as 2022-01-31.
     * @return The method `getReportAttachmentName` returns a String that concatenates the `attachmentPrefix`,
     *      `reportDate`, and `ATTACHMENT_SUFFIX`.
     */
    private String getReportAttachmentName(String attachmentPrefix, LocalDate reportDate) {
        return attachmentPrefix + reportDate + ATTACHMENT_SUFFIX;
    }
}
