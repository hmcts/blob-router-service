package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationMapper;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationReportResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationStatement;

import java.time.LocalDate;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class DetailedReportService {

    private static final Logger logger = getLogger(DetailedReportService.class);

    private final ReconciliationMapper reconciliationMapper;
    private final ReconciliationReportRepository reconciliationReportRepository;
    private final ReconciliationService reconciliationService;
    private final BulkScanProcessorClient bulkScanProcessorClient;
    private final ObjectMapper objectMapper;

    public DetailedReportService(
        ReconciliationMapper reconciliationMapper,
        ReconciliationReportRepository reconciliationReportRepository,
        ReconciliationService reconciliationService,
        BulkScanProcessorClient bulkScanProcessorClient,
        ObjectMapper objectMapper
    ) {
        this.reconciliationMapper = reconciliationMapper;
        this.reconciliationReportRepository = reconciliationReportRepository;
        this.reconciliationService = reconciliationService;
        this.bulkScanProcessorClient = bulkScanProcessorClient;
        this.objectMapper = objectMapper;
    }

    public void process(LocalDate date, TargetStorageAccount account) {

        Assert.isTrue(
            account == TargetStorageAccount.BULKSCAN,
            "Only BULKSCAN account can be processed."
        );

        var optionReconciliationReport =
            reconciliationReportRepository.getLatestReconciliationReport(date, account.name());

        if (optionReconciliationReport.isPresent()) {
            var reconciliationReport = optionReconciliationReport.get();

            if (Objects.nonNull(reconciliationReport.detailedContent)) {
                logger.info(
                    "Detailed report already processed. Report id: {}, supplier statement id: {}, created at: {}",
                    reconciliationReport.id,
                    reconciliationReport.supplierStatementId,
                    reconciliationReport.createdAt
                );
                return;
            }

            var optionalEnvelopeSupplierStatement = reconciliationService
                .getSupplierStatement(date);

            if (!optionalEnvelopeSupplierStatement.isPresent()) {
                logger.error("No supplier statement report for: {} but there is summary report.",
                    date);
                return;
            }
            var supplierStatement = optionalEnvelopeSupplierStatement.get();
            processByTargetStorage(supplierStatement, account, reconciliationReport);
        } else {
            logger.info("No summary report for account: {}, for {}", account, date);
        }
    }

    private void processByTargetStorage(
        EnvelopeSupplierStatement supplierStatement,
        TargetStorageAccount targetStorageAccount,
        ReconciliationReport reconciliationReport
    ) {
        try {
            ReconciliationStatement reconciliationStatement =
                reconciliationMapper
                    .convertToReconciliationStatement(supplierStatement, targetStorageAccount);

            ReconciliationReportResponse reconciliationReportResponse = bulkScanProcessorClient
                .postReconciliationReport(reconciliationStatement);

            String content = objectMapper.writeValueAsString(reconciliationReportResponse);

            reconciliationReportRepository.updateDetailedContent(reconciliationReport.id, content);
            logger.info(
                "Detailed reported created, Report Id: {}, Supplier statement Id: {}, "
                    + "targetStorageAccount: {}, Date:{}",
                reconciliationReport.id, supplierStatement.id, targetStorageAccount,
                supplierStatement.date);

        } catch (Exception ex) {
            logger.error("Processing Supplier Statement Id {} failed for account: {} ",
                supplierStatement.id,
                targetStorageAccount,
                ex
            );
        }
    }

}
