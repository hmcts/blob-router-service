package uk.gov.hmcts.reform.blobrouter.reconciliation.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.Envelope;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.SupplierStatement;

import java.util.Map;

import static java.util.stream.Collectors.toList;

@Service
public class ReconciliationStatementMapper {

    private final ObjectMapper objectMapper;
    private final Map<String, StorageConfigItem> storageConfig; // container-specific configuration, by container name

    public ReconciliationStatementMapper(ObjectMapper objectMapper, ServiceConfiguration serviceConfiguration) {
        this.objectMapper = objectMapper;
        storageConfig =  serviceConfiguration.getStorageConfig();
    }

    @SuppressWarnings("unchecked")
    public ReconciliationStatement convertToReconciliationStatement(
        EnvelopeSupplierStatement envelopeSupplierStatement, String targetContainer
    ) throws JsonProcessingException {

        SupplierStatement supplierStatement =
            objectMapper.readValue(envelopeSupplierStatement.content, SupplierStatement.class);

        return new ReconciliationStatement(
            envelopeSupplierStatement.date,
            supplierStatement
                .envelopes
                .stream()
                .filter(e -> filterByContainer(e, targetContainer))
                .map(this::mapToReportedZipFile).collect(toList())
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
        String envelopeTargetContainer = storageConfig.get(envelope.container).getTargetContainer();
        return envelopeTargetContainer.equals(targetContainer);
    }
}
