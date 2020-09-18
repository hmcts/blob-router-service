package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.SupplierStatementRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationReportResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.services.email.EmailSender;

import java.io.File;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

public class ReconciliationMailService {

    private static final Logger logger = getLogger(ReconciliationMailService.class);

    private final SupplierStatementRepository repository;
    private final ReconciliationReportRepository reconciliationReportRepository;
    private final ReconciliationCsvWriter reconciliationCsvWriter;
    private final EmailSender emailSender;
    private final ObjectMapper objectMapper;

    private final String mailFrom;
    private final String[] mailRecipients;

    public ReconciliationMailService(
        SupplierStatementRepository repository,
        ReconciliationReportRepository reconciliationReportRepository,
        ReconciliationCsvWriter reconciliationCsvWriter,
        EmailSender emailSender,
        ObjectMapper objectMapper,
        @Value("${reconciliation.report.mail-from") String mailFrom,
        @Value("${reconciliation.report.mail-recipients}") String[] recipients
    ) {
        this.repository = repository;
        this.reconciliationReportRepository = reconciliationReportRepository;
        this.reconciliationCsvWriter = reconciliationCsvWriter;
        this.emailSender = emailSender;
        this.objectMapper = objectMapper;
        this.mailFrom = mailFrom;
        this.mailRecipients = recipients;
    }

    public void process(LocalDate date, List<TargetStorageAccount> availableAccounts) {
        Optional<EnvelopeSupplierStatement> optionalEnvelopeSupplierStatement = repository
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
                report -> processReconciliationReport(report, account),
                () -> logger.error("No report created for account {} for date {}", account, date)
            );
    }

    private void processReconciliationReport(
        ReconciliationReport reconciliationReport,
        TargetStorageAccount account
    ) {
        try {
            if (reconciliationReport.summaryContent != null) {

                if (reconciliationReport.sentAt != null) {
                    logger.info(
                        "Skipping mailing process, Reconciliation Report send at {}, Report Id: {}",
                        reconciliationReport.sentAt,
                        reconciliationReport.id
                    );
                    return;
                }

                SummaryReport summaryReport =
                    objectMapper.readValue(reconciliationReport.summaryContent, SummaryReport.class);

                File file = reconciliationCsvWriter.writeSummaryReconciliationToCsv(summaryReport);

                Map<String, File> attachments = new HashMap<>();
                attachments.put("Summary Report", file);
                ReconciliationReportResponse detailedReport = null;
                if (reconciliationReport.detailedContent != null) {
                    detailedReport =
                        objectMapper.readValue(reconciliationReport.detailedContent,
                            ReconciliationReportResponse.class);

                    File detailedReportFile = reconciliationCsvWriter
                        .writeDetailedReconciliationToCsv(detailedReport);
                    attachments.put("Detailed report", detailedReportFile);
                }

                emailSender.sendMessageWithAttachments(
                    createTitle(account, summaryReport, detailedReport),
                    "",
                    mailFrom,
                    mailRecipients,
                    attachments
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
                "Sending reconciliation email failed for {}, for Reconciliation Report id: {}",
                account,
                reconciliationReport.id
            );
        }
    }

    private void sendMailNoSupplierStatement(LocalDate date, TargetStorageAccount account) {
        try {
            emailSender.sendMessageWithAttachments(
                account.name() + " Scanning Reconciliation NO REPORT RECEIVED",
                "No Report received for " + date,
                mailFrom,
                mailRecipients,
                Collections.emptyMap()
            );
        } catch (Exception ex) {
            logger.error(
                "Sending `No Supplier statement email` failed for {} for account {}",
                date,
                account
            );
        }
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
}
