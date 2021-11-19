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
