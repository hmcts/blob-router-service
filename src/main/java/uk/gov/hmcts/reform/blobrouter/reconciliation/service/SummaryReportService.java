package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.NewReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.SupplierStatementRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.SupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReportItem;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class SummaryReportService {

    private static final Logger logger = getLogger(SummaryReportService.class);

    private final SupplierStatementRepository repository;
    private final ReconciliationReportRepository reconciliationReportRepository;
    private final ObjectMapper objectMapper;
    private final EnvelopeService envelopeService;
    private final Map<String, StorageConfigItem> storageConfig; // container-specific configuration, by container name

    public SummaryReportService(
        SupplierStatementRepository repository,
        ReconciliationReportRepository reconciliationReportRepository,
        ObjectMapper objectMapper,
        EnvelopeService envelopeService,
        ServiceConfiguration serviceConfiguration
    ) {
        this.repository = repository;
        this.reconciliationReportRepository = reconciliationReportRepository;
        this.objectMapper = objectMapper;
        this.envelopeService = envelopeService;
        this.storageConfig =  serviceConfiguration.getStorageConfig();
    }

    public void process(LocalDate date) throws JsonProcessingException {
        Optional<EnvelopeSupplierStatement> optSupplierStatement = repository.findLatest(date);

        if (!optSupplierStatement.isPresent()) {
            logger.error("No supplier statement found for {}", date);
            return;
        }

        EnvelopeSupplierStatement envelopeSupplierStatement = optSupplierStatement.get();
        SupplierStatement supplierStatement = objectMapper
            .readValue(envelopeSupplierStatement.content, SupplierStatement.class);

        var envelopeList = envelopeService.getEnvelopes(date);

        if (CollectionUtils.isEmpty(envelopeList)) {
            logger.info("No envelopes find for {} ", date);
        }

        Map<TargetStorageAccount, List<uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.Envelope>>
            supplierEnvelopesMap = supplierStatement
            .envelopes
            .stream()
            .collect(groupingBy(e -> storageConfig.get(e.container).getTargetStorageAccount()));

        Map<TargetStorageAccount, List<Envelope>> processedEnvelopesMap =
            envelopeList
                .stream()
                .collect(groupingBy(e -> storageConfig.get(e.container).getTargetStorageAccount()));

        for (var targetStorage : TargetStorageAccount.values()) {
            try {

                var processedEnvelopes = processedEnvelopesMap.get(targetStorage);
                var supplierEnvelopes = supplierEnvelopesMap.get(targetStorage);
                SummaryReport summaryReport = createSummaryReport(
                    processedEnvelopes == null ? emptyList() : processedEnvelopes,
                    supplierEnvelopes == null ? emptyList() : supplierEnvelopes
                );

                String summaryContent = objectMapper.writeValueAsString(summaryReport);
                var report = new NewReconciliationReport(
                    envelopeSupplierStatement.id,
                    targetStorage.name(),
                    summaryContent,
                    null,
                    "1.0"
                );
                reconciliationReportRepository.save(report);
            } catch (Exception ex) {
                logger.error(
                    "Error creating summary report. Account: {}, supplier Id: {}, date: {}",
                    targetStorage,
                    envelopeSupplierStatement.id,
                    date
                );
            }
        }
    }

    private SummaryReport createSummaryReport(List<Envelope> processedEnvelopes,
        List<uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.Envelope> supplierEnvelopes) {

        int actualCount = processedEnvelopes.size();
        int reportedCount = supplierEnvelopes.size();

        List<SummaryReportItem> receivedButNotReported = processedEnvelopes
            .stream()
            .filter(e ->
                supplierEnvelopes.stream().noneMatch(s -> isEqualFile(e, s)))
            .map(e -> new SummaryReportItem(e.fileName, e.container))
            .collect(Collectors.toList());

        List<SummaryReportItem> reportedButNotProcessed = supplierEnvelopes
            .stream()
            .filter(s ->
                processedEnvelopes.stream().noneMatch(e -> isEqualFile(e, s)))
            .map(s -> new SummaryReportItem(s.zipFileName, s.container))
            .collect(Collectors.toList());

        return new SummaryReport(actualCount, receivedButNotReported, reportedCount, reportedButNotProcessed);
    }

    private boolean isEqualFile(
        Envelope envelope,
        uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.Envelope supplierReportedEnvelope
    ) {
        return envelope.fileName.equals(supplierReportedEnvelope.zipFileName)
            && envelope.container.equals(supplierReportedEnvelope.container);
    }

}
