package uk.gov.hmcts.reform.blobrouter.reconciliation.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.Envelope;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.SupplierStatement;

import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * The `ReconciliationMapper` class in Java provides methods to convert envelope data to reconciliation statements
 * based on container-specific configurations.
 */
@Service
@EnableConfigurationProperties(ServiceConfiguration.class)
public class ReconciliationMapper {

    private final ObjectMapper objectMapper;
    private final Map<String, StorageConfigItem> storageConfig; // container-specific configuration, by container name

    public ReconciliationMapper(ObjectMapper objectMapper, ServiceConfiguration serviceConfiguration) {
        this.objectMapper = objectMapper;
        storageConfig =  serviceConfiguration.getStorageConfig();
    }

    /**
     * The function `convertToReconciliationStatement` takes an `EnvelopeSupplierStatement` and a
     * `TargetStorageAccount`, converts the content to a `SupplierStatement`, filters and maps the envelopes
     * based on a condition, and returns a `ReconciliationStatement`.
     *
     * @param envelopeSupplierStatement An `EnvelopeSupplierStatement` object containing the content of a supplier
     *      statement in an envelope format, which needs to be converted to a `SupplierStatement` object
     *      using an `objectMapper`.
     * @param targetStorage The `targetStorage` parameter in the `convertToReconciliationStatement` method is of type
     *      `TargetStorageAccount`. It is used to filter the envelopes based on a specific container in the
     *      supplier statement before creating a reconciliation statement.
     * @return A ReconciliationStatement object is being returned, which is created using the date from the
     *      envelopeSupplierStatement and a list of reported zip files obtained by filtering and mapping the
     *      envelopes in the supplierStatement based on a target storage account.
     */
    public ReconciliationStatement convertToReconciliationStatement(
        EnvelopeSupplierStatement envelopeSupplierStatement,
        TargetStorageAccount targetStorage
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

    /**
     * The function `mapToReportedZipFile` creates a new `ReportedZipFile` object based on the data in the `Envelope`
     * object.
     *
     * @param envelope The `mapToReportedZipFile` method takes an `Envelope` object as a parameter and maps its
     *                 attributes to a `ReportedZipFile` object. The attributes of the `Envelope` object are used to
     *                 initialize the corresponding attributes of the `ReportedZipFile` object.
     * @return A new instance of ReportedZipFile is being returned, with the properties initialized using
     *      values from the Envelope object passed as a parameter to the method.
     */
    private ReportedZipFile mapToReportedZipFile(Envelope envelope) {
        return new ReportedZipFile(
            envelope.zipFileName,
            envelope.container.toLowerCase(),
            envelope.rescanFor,
            envelope.scannableItemDcns,
            envelope.paymentDcns
        );
    }

    /**
     * The function filters an envelope by container based on a target storage account.
     *
     * @param envelope The `envelope` parameter in the `filterByContainer` method represents an envelope object that
     *      contains information about a container. It likely has a property named `container` which is used to
     *      identify the container associated with the envelope.
     * @param targetStorageAccount TargetStorageAccount is an object representing the storage account where the envelope
     *      should be filtered. It contains information such as the storage account name, location, and other
     *      relevant details.
     * @return The method `filterByContainer` returns a boolean value indicating whether the target storage
     *      account of the specified envelope's container matches the provided `targetStorageAccount`.
     */
    private boolean filterByContainer(Envelope envelope, TargetStorageAccount targetStorageAccount) {
        return storageConfig
            .get(envelope.container.toLowerCase())
            .getTargetStorageAccount()
            .equals(targetStorageAccount);
    }
}
