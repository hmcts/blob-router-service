package uk.gov.hmcts.reform.blobrouter.reconciliation.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.Envelope;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.SupplierStatement;

import java.util.Map;

import static java.util.stream.Collectors.toList;

@Service
@EnableConfigurationProperties(ServiceConfiguration.class)
public class ReconciliationMapper {

    private final ObjectMapper objectMapper;
    private final Map<String, StorageConfigItem> storageConfig; // container-specific configuration, by container name

    public ReconciliationMapper(ObjectMapper objectMapper, ServiceConfiguration serviceConfiguration) {
        this.objectMapper = objectMapper;
        storageConfig =  serviceConfiguration.getStorageConfig();
    }

    public ReconciliationStatement convertToReconciliationStatement(
        EnvelopeSupplierStatement envelopeSupplierStatement,
        String targetStorage
    ) throws JsonProcessingException {

        SupplierStatement supplierStatement =
            objectMapper.readValue(envelopeSupplierStatement.content, SupplierStatement.class);

        return new ReconciliationStatement(
            envelopeSupplierStatement.date,
            supplierStatement
                .envelopes
                .stream()
                .filter(e -> filterByContainer(e, targetStorage))
                .map(this::mapToReportedZipFile)
                .collect(toList())
        );
    }

    private ReportedZipFile mapToReportedZipFile(Envelope envelope) {
        return new ReportedZipFile(
            envelope.zipFileName,
            envelope.container,
            envelope.rescanFor,
            envelope.scannableItemDcns,
            envelope.paymentDcns
        );
    }

    private boolean filterByContainer(Envelope envelope, String targetContainer) {
        return storageConfig
            .get(envelope.container)
            .getTargetContainer()
            .equals(targetContainer);
    }
}
