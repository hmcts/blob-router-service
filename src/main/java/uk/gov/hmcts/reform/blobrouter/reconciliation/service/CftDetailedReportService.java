package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
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
public class CftDetailedReportService {

    private static final Logger logger = getLogger(CftDetailedReportService.class);

    private final ReconciliationMapper reconciliationMapper;
    private final ReconciliationReportRepository reconciliationReportRepository;
    private final ReconciliationService reconciliationService;
    private final BulkScanProcessorClient bulkScanProcessorClient;
    private final ObjectMapper objectMapper;

    public CftDetailedReportService(
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

    /**
     * The `process` method retrieves the latest reconciliation report for a given date and processes it if it exists,
     * logging a message if no report is found.
     *
     * @param date The `process` method takes a `LocalDate` parameter named `date`. This method is responsible for
     *             processing reconciliation reports based on the provided date. It retrieves the latest
     *             reconciliation report for a specific storage account (in this case, "CFT") and
     *             then processes it if it exists.
     */
    public void process(LocalDate date) {
        // get the latest one and check its detailed content.
        // if there are older records without detailed report we do not need to process them.
        var optionReconciliationReport =
            reconciliationReportRepository.getLatestReconciliationReport(date, TargetStorageAccount.CFT.name());

        optionReconciliationReport.ifPresentOrElse(
            reconciliationReport -> processReconciliationReport(reconciliationReport, date),
            () -> logger.info("No summary report to create reconciliation detailed report for CFT, Date: {}", date)
        );
    }

    /**
     * The `processReconciliationReport` method processes a reconciliation report by checking if a detailed report
     * exists, logging information if it does, and creating a detailed report if a supplier statement is present.
     *
     * @param reconciliationReport The `reconciliationReport` parameter is an object of type `ReconciliationReport`,
     *                             which contains information related to a reconciliation report. It has properties
     *                             such as `detailedContent`, `supplierStatementId`, `id`, and `createdAt`.
     * @param date                 The `date` parameter in the `processReconciliationReport` method is of type
     *                             `LocalDate`. It is used to specify the date for which the reconciliation
     *                             report is being processed.
     */
    private void processReconciliationReport(ReconciliationReport reconciliationReport, LocalDate date) {

        if (Objects.nonNull(reconciliationReport.detailedContent)) {
            logger.info(
                "Reconciliation detailed report already processed."
                    + "Supplier Statement Id: {}, Report id: {}, Created at: {}",
                reconciliationReport.supplierStatementId,
                reconciliationReport.id,
                reconciliationReport.createdAt
            );
            return;
        }

        reconciliationService
            .getSupplierStatement(reconciliationReport.supplierStatementId)
            .ifPresentOrElse(
                supplierStatement -> createDetailedReport(
                    supplierStatement, reconciliationReport
                ),
                () -> logger.error("No supplier statement report for: {} but there is summary report.", date)
            );
    }

    /**
     * This function creates a detailed reconciliation report based on a supplier statement and updates the content in
     * the reconciliation report repository.
     *
     * @param supplierStatement    The `supplierStatement` parameter is an object of type `EnvelopeSupplierStatement`,
     *                             which likely contains information related to a supplier's statement such as ID, date,
     *                             and other relevant details.
     * @param reconciliationReport The `createDetailedReport` method takes in two parameters: `supplierStatement`
     *                             of type `EnvelopeSupplierStatement` and `reconciliationReport` of type
     *                             `ReconciliationReport`.
     */
    private void createDetailedReport(
        EnvelopeSupplierStatement supplierStatement,
        ReconciliationReport reconciliationReport
    ) {
        try {
            ReconciliationStatement reconciliationStatement =
                reconciliationMapper
                    .convertToReconciliationStatement(supplierStatement, TargetStorageAccount.CFT);

            ReconciliationReportResponse reconciliationReportResponse = bulkScanProcessorClient
                .postReconciliationReport(reconciliationStatement);

            String content = objectMapper.writeValueAsString(reconciliationReportResponse);

            reconciliationReportRepository.updateDetailedContent(reconciliationReport.id, content);
            logger.info(
                "Reconciliation detailed reported updated. "
                    + "Supplier Statement Id: {}, Report Id: {}, Account: {}, Date: {}",
                supplierStatement.id,
                reconciliationReport.id,
                TargetStorageAccount.CFT,
                supplierStatement.date
            );

        } catch (Exception ex) {
            logger.error(
                "Reconciliation detailed reported creation failed. "
                    + "Supplier Statement Id: {}, Reconciliation report Id: {}, Account: {}",
                supplierStatement.id,
                reconciliationReport.id,
                TargetStorageAccount.CFT,
                ex
            );
        }
    }
}
