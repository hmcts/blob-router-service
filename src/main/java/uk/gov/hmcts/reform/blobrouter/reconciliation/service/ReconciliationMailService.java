package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.SupplierStatementRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationReportResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.services.email.MessageSender;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The `ReconciliationMailService` class in Java handles reconciliation reports, sending emails, and notifications for
 * missing supplier statements.
 */
@Service
public class ReconciliationMailService {

    private static final Logger logger = getLogger(ReconciliationMailService.class);

    private final SupplierStatementRepository supplierStatementRepository;
    private final ReconciliationReportRepository reconciliationReportRepository;
    private final MessageSender emailSender;
    private final ObjectMapper objectMapper;
    private final ReconciliationSender reconciliationSender;

    private final String mailFrom;
    private final String[] mailRecipients;

    private static final String NO_SUPPLIER_STATEMENT_RECEIVED_SUBJECT_FORMAT = "[NO SUPPLIER STATEMENT RECEIVED] %s "
        + "Scanning Reconciliation %s";
    private static final String NO_SUPPLIER_STATEMENT_RECEIVED_BODY_FORMAT = "No supplier statement received for %s";

    public ReconciliationMailService(
        SupplierStatementRepository supplierStatementRepository,
        ReconciliationReportRepository reconciliationReportRepository,
        MessageSender emailSender,
        ObjectMapper objectMapper,
        ReconciliationSender reconciliationSender,
        @Value("${reconciliation.report.mail-from}") String mailFrom,
        @Value("${reconciliation.report.mail-recipients}") String[] recipients
    ) {
        this.supplierStatementRepository = supplierStatementRepository;
        this.reconciliationReportRepository = reconciliationReportRepository;
        this.emailSender = emailSender;
        this.objectMapper = objectMapper;
        this.reconciliationSender = reconciliationSender;
        this.mailFrom = mailFrom;
        this.mailRecipients = recipients;
    }

    /**
     * The `process` method iterates through a list of available accounts, checks for
     * the latest supplier statement for a given date, and either generates a reconciliation
     * report or sends a mail if no statement is found.
     *
     * @param date The `date` parameter in the `process` method represents the date for
     *             which the reconciliation process is being performed. It is used to find the
     *             latest supplier statement and to process reconciliation reports for the available
     *             accounts on that date.
     * @param availableAccounts A list of `TargetStorageAccount` objects that are available for processing.
     */
    public void process(LocalDate date, List<TargetStorageAccount> availableAccounts) {
        Optional<EnvelopeSupplierStatement> optionalEnvelopeSupplierStatement = supplierStatementRepository
            .findLatest(date);

        for (var account : availableAccounts) {
            try {
                optionalEnvelopeSupplierStatement.ifPresentOrElse(
                    e -> getReconciliationReportAndProcess(date, account),
                    () -> sendMailNoSupplierStatement(date, account)
                );
            } catch (Exception ex) {
                logger.error("Error while processing reconciliation report mailing. Date: {}", date, ex);
            }
        }
    }

    /**
     * This function retrieves the latest reconciliation report for a specific date and account, then processes it if it
     * exists, otherwise logs an error message.
     *
     * @param date The `date` parameter is of type `LocalDate` and represents
     *       the date for which the reconciliation report is being retrieved and processed.
     * @param account The `account` parameter is of type `TargetStorageAccount`, which is used to specify the target
     *      storage account for which the reconciliation report needs to be retrieved and processed.
     */
    private void getReconciliationReportAndProcess(
        LocalDate date,
        TargetStorageAccount account
    ) {
        reconciliationReportRepository
            .getLatestReconciliationReport(date, account.name())
            .ifPresentOrElse(
                report -> processReconciliationReport(date, report, account),
                () -> logger.error("No report created for account {} for date {}", account, date)
            );
    }

    /**
     * The `processReconciliationReport` function processes a reconciliation report by handling summary and detailed
     * content, sending the report via email, and updating the sent timestamp.
     *
     * @param date The `date` parameter in the `processReconciliationReport` method represents the date for which the
     *      reconciliation report is being processed. It is of type `LocalDate` and is used to
     *      identify the specific date associated with the reconciliation report.
     * @param reconciliationReport The `processReconciliationReport` method takes in three parameters:
     * @param account The `account` parameter in the `processReconciliationReport` method represents the
     *      `TargetStorageAccount` object that is used to specify the target storage account for processing the
     *      reconciliation report. This object likely contains information about the target storage account
     *      such as its name, ID, configuration settings, or any other values.
     */
    private void processReconciliationReport(
        LocalDate date,
        ReconciliationReport reconciliationReport,
        TargetStorageAccount account
    ) {
        try {
            if (reconciliationReport.summaryContent != null) {

                if (reconciliationReport.sentAt != null) {
                    logger.info(
                        "Skipping mailing process, Reconciliation Report sent at {}, Report Id: {}",
                        reconciliationReport.sentAt,
                        reconciliationReport.id
                    );
                    return;
                }

                SummaryReport summaryReport =
                    objectMapper.readValue(reconciliationReport.summaryContent, SummaryReport.class);

                ReconciliationReportResponse detailedReport = null;
                if (reconciliationReport.detailedContent != null) {
                    detailedReport =
                        objectMapper.readValue(
                            reconciliationReport.detailedContent,
                            ReconciliationReportResponse.class
                        );
                }

                reconciliationSender.sendReconciliationReport(
                    date,
                    account,
                    summaryReport,
                    detailedReport
                );

                reconciliationReportRepository.updateSentAt(reconciliationReport.id);
            } else {
                logger.error(
                    "No summary report for account {}, Reconciliation report id: {}",
                    account,
                    reconciliationReport.id
                );
            }

        } catch (Exception ex) {
            logger.error(
                "Sending reconciliation email failed for {}, for Reconciliation Report id: {}, "
                    + "mailFrom {}, mailRecipients {}",
                account,
                reconciliationReport.id,
                mailFrom,
                mailRecipients,
                ex
            );
        }
    }

    /**
     * The `sendMailNoSupplierStatement` function sends an email notification if no supplier statement is received for a
     * specific date and account.
     *
     * @param date The `date` parameter in the `sendMailNoSupplierStatement` method represents the date for which the
     *      supplier statement is missing or not received.
     * @param account The `account` parameter in the `sendMailNoSupplierStatement` method represents the target storage
     *      account to which the email will be sent. It seems to have a method `name()`
     *      that retrieves the name of the account.
     */
    private void sendMailNoSupplierStatement(LocalDate date, TargetStorageAccount account) {
        try {
            emailSender.sendMessageWithAttachments(
                String.format(NO_SUPPLIER_STATEMENT_RECEIVED_SUBJECT_FORMAT, account.name(), date.toString()),
                String.format(NO_SUPPLIER_STATEMENT_RECEIVED_BODY_FORMAT, date),
                mailFrom,
                mailRecipients,
                Collections.emptyMap()
            );
        } catch (Exception ex) {
            logger.error(
                "Sending `No Supplier statement email` failed for {} for account {}",
                date,
                account,
                ex
            );
        }
    }
}
