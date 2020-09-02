package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.NewReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationMapper;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationReportResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationStatement;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.BULKSCAN;

@Service
public class ReconciliationProcessService {

    private static final Logger logger = getLogger(ReconciliationProcessService.class);

    private final ReconciliationMapper reconciliationMapper;
    private final ReconciliationService reconciliationService;
    private final BulkScanProcessorClient bulkScanProcessorClient;
    private final ObjectMapper objectMapper;
    private final ReconciliationReportRepository reconciliationReportRepository;

    public ReconciliationProcessService(
        ReconciliationMapper reconciliationMapper,
        ReconciliationService reconciliationService,
        BulkScanProcessorClient bulkScanProcessorClient,
        ObjectMapper objectMapper,
        ReconciliationReportRepository reconciliationReportRepository
    ) {
        this.reconciliationMapper = reconciliationMapper;
        this.reconciliationService = reconciliationService;
        this.bulkScanProcessorClient = bulkScanProcessorClient;
        this.objectMapper = objectMapper;
        this.reconciliationReportRepository = reconciliationReportRepository;
    }

    public void process(LocalDate date) {
        var optionalEnvelopeSupplierStatement = reconciliationService.getSupplierStatement(date);
        if (optionalEnvelopeSupplierStatement.isPresent()) {
            var supplierStatement = optionalEnvelopeSupplierStatement.get();
            for (var targetStorage : TargetStorageAccount.values()) {
                processByTargetStorage(supplierStatement, targetStorage);
            }
        } else {
            logger.info("No Supplier Statement for date: ", date);
        }
    }

    private void processByTargetStorage(
        EnvelopeSupplierStatement supplierStatement,
        TargetStorageAccount targetStorageAccount
    ) {
        try {
            ReconciliationStatement reconciliationStatement =
                reconciliationMapper
                    .convertToReconciliationStatement(supplierStatement, targetStorageAccount);
            switch (targetStorageAccount) {
                case BULKSCAN:
                    processBulkScan(reconciliationStatement, supplierStatement.id);
                    break;
                case CRIME:
                    logger.error("Crime reconciliation process skipped");
                    break;
                case PCQ:
                    logger.error("PCQ reconciliation processing skipped");
                    break;
                default:
                    logger.error("Unknown target storage type {} ", targetStorageAccount);
            }
        } catch (Exception ex) {
            logger.error("Processing Supplier Statement Id {} failed for account: {} ",
                supplierStatement.id,
                targetStorageAccount,
                ex
            );
        }
    }

    private void processBulkScan(
        ReconciliationStatement reconciliationStatement,
        UUID supplierStatementId
    ) throws JsonProcessingException, SQLException {

        ReconciliationReportResponse reconciliationReportResponse = bulkScanProcessorClient
            .postReconciliationReport(reconciliationStatement);

        String content = objectMapper.writeValueAsString(reconciliationReportResponse);
        var newReconciliationReport =
            new NewReconciliationReport(supplierStatementId, BULKSCAN.name(), content, "1.0");
        reconciliationReportRepository.save(newReconciliationReport);
    }


}
