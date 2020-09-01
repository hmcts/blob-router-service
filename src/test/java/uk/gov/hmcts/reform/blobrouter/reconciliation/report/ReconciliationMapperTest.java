package uk.gov.hmcts.reform.blobrouter.reconciliation.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
class ReconciliationMapperTest {

    private ReconciliationMapper reconciliationMapper;
    private ServiceConfiguration serviceConfiguration = mock(ServiceConfiguration.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, StorageConfigItem> storageConfig = mock(Map.class);

    @BeforeEach
    void setUp() {
        given(serviceConfiguration.getStorageConfig()).willReturn(storageConfig);
        reconciliationMapper =
            new ReconciliationMapper(objectMapper, serviceConfiguration);
    }

    @Test
    void should_filter_by_target_container() throws IOException {
        // given
        setupStorageConfig();
        String content = Resources.toString(
            getResource("reconciliation/valid-supplier-statement.json"),
            UTF_8
        );

        LocalDate reportDate = LocalDate.now();
        EnvelopeSupplierStatement envelopeSupplierStatement = new EnvelopeSupplierStatement(
            UUID.randomUUID(),
            reportDate,
            content,
            "1.0",
            LocalDateTime.now()
        );

        // when
        ReconciliationStatement reconciliationStatement =
            reconciliationMapper.convertToReconciliationStatement(
                envelopeSupplierStatement,
                "crime"
            );

        // then
        assertThat(reconciliationStatement.envelopes.size()).isEqualTo(1);
        ReportedZipFile crimeFile = reconciliationStatement.envelopes.get(0);
        assertThat(crimeFile.container).isEqualTo("crime");

    }

    @Test
    void should_map_content_to_ReconciliationStatement() throws IOException {
        // given
        setupStorageConfig();
        String content = Resources.toString(
            getResource("reconciliation/valid-supplier-statement.json"),
            UTF_8
        );

        LocalDate reportDate = LocalDate.now();
        EnvelopeSupplierStatement envelopeSupplierStatement = new EnvelopeSupplierStatement(
            UUID.randomUUID(),
            reportDate,
            content,
            "1.0",
            LocalDateTime.now()
        );

        // when
        ReconciliationStatement reconciliationStatement =
            reconciliationMapper.convertToReconciliationStatement(
                envelopeSupplierStatement,
                "bulkscan"
            );

        // then
        assertThat(reconciliationStatement.date).isEqualTo(reportDate);
        assertThat(reconciliationStatement.envelopes.size()).isEqualTo(3);
        ReportedZipFile probateFile = reconciliationStatement.envelopes.get(0);
        ReportedZipFile sscsFile = reconciliationStatement.envelopes.get(1);
        ReportedZipFile cmcFile = reconciliationStatement.envelopes.get(2);
        assertReportedZipFile(probateFile,
            "1010404021234_14-08-2020-08-31.zip",
            "probate",
            null,
            List.of("1015404021234", "1015404021235"),
            List.of("123123", "123124")
        );

        assertReportedZipFile(sscsFile,
            "9810404021234_14-08-2020-03-08-31.zip",
            "sscs",
            "121212_14-08-2020-03-08-21.zip",
            List.of("9988774021234", "6655443301235"),
            List.of("999999", "999234")
        );

        assertReportedZipFile(cmcFile,
            "11111222333_14-05-2020-09-08-31.zip",
            "cmc",
            "9999999_4343_43343_4343.zip",
            List.of("4444774020000", "5555443300001"),
            List.of("3456789", "1234567")
        );

    }

    private StorageConfigItem createStorageConfigItem(String targetContainer) {
        StorageConfigItem storageConfigItem = new StorageConfigItem();
        storageConfigItem.setTargetContainer(targetContainer);
        return storageConfigItem;
    }

    private void assertReportedZipFile(
        ReportedZipFile reportedZipFile,
        String zipFileName,
        String container,
        String rescanFor,
        List<String> scannableItemDcns,
        List<String> paymentDcns
    ) {
        assertThat(reportedZipFile.zipFileName).isEqualTo(zipFileName);
        assertThat(reportedZipFile.container).isEqualTo(container);
        assertThat(reportedZipFile.rescanFor).isEqualTo(rescanFor);
        assertThat(reportedZipFile.scannableItemDcns).isEqualTo(scannableItemDcns);
        assertThat(reportedZipFile.paymentDcns).isEqualTo(paymentDcns);

    }

    private void setupStorageConfig() {
        given(storageConfig.get("sscs")).willReturn(createStorageConfigItem("bulkscan"));
        given(storageConfig.get("probate")).willReturn(createStorageConfigItem("bulkscan"));
        given(storageConfig.get("crime")).willReturn(createStorageConfigItem("crime"));
        given(storageConfig.get("pcq")).willReturn(createStorageConfigItem("pcq"));
        given(storageConfig.get("cmc")).willReturn(createStorageConfigItem("bulkscan"));
    }

}