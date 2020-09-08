package uk.gov.hmcts.reform.blobrouter.reconciliation.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.config.StorageConfigItem;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.NewReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.SupplierStatementRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReportItem;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.BULKSCAN;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CRIME;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.PCQ;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class SummaryReportServiceTest {

    private SummaryReportService summaryReportService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private SupplierStatementRepository supplierStatementRepository;
    @Mock private ReconciliationReportRepository reconciliationReportRepository;
    @Mock private EnvelopeService envelopeService;
    @Mock private ServiceConfiguration serviceConfiguration;
    @Mock private Map<String, StorageConfigItem> storageConfig;
    @Mock private SummaryReportCreator summaryReportCreator;

    @Captor
    private ArgumentCaptor<NewReconciliationReport> newReconciliationReportCaptor;

    @BeforeEach
    void setUp() {
        given(serviceConfiguration.getStorageConfig()).willReturn(storageConfig);
        summaryReportService = new SummaryReportService(
            supplierStatementRepository,
            reconciliationReportRepository,
            objectMapper,
            envelopeService,
            serviceConfiguration,
            summaryReportCreator
        );
    }

    @Test
    void should_continue_if_there_is_no_supplier_report() throws JsonProcessingException {
        // given
        LocalDate date = LocalDate.now();
        given(supplierStatementRepository.findLatest(date)).willReturn(Optional.empty());

        // when
        summaryReportService.process(date);

        // then
        verifyNoInteractions(envelopeService);
        verifyNoInteractions(reconciliationReportRepository);
    }

    @Test
    void should_continue_process_with_next_target_storage_if_one_fails()
        throws Exception {
        // given
        given(storageConfig.get(anyString())).willReturn(createStorageConfigItem(BULKSCAN));
        LocalDate date = LocalDate.now();
        String content = Resources.toString(
            getResource("reconciliation/valid-supplier-statement.json"),
            UTF_8
        );

        UUID supplierId = UUID.randomUUID();
        var envelopeSupplierStatement = new EnvelopeSupplierStatement(
            supplierId,
            date,
            content,
            "1.0",
            LocalDateTime.now()
        );

        given(supplierStatementRepository.findLatest(date))
            .willReturn(Optional.of(envelopeSupplierStatement));
        var envelopeList = Arrays.asList(
            createEnvelope("1010404021234_14-08-2020-08-31.zip", "probate")
        );
        given(envelopeService.getEnvelopes(date)).willReturn(envelopeList);
        given(summaryReportCreator.createSummaryReport(any(), any()))
            .willReturn(
                new SummaryReport(
                    120,
                    120,
                    List.of(new SummaryReportItem("12312.31312.312.zip", "sscs")),
                    List.of(new SummaryReportItem("9810404021234_14-08-2020-03-08-31.zip", "cmc"))
                )
            );


        given(reconciliationReportRepository.save(any()))
            .willThrow(new RuntimeException("Can not save"));
        //when
        summaryReportService.process(date);

        // then
        // should try for all target storage accounts
        verify(summaryReportCreator, times(3)).createSummaryReport(any(), any());
        verify(reconciliationReportRepository, times(3)).save(any());

    }

    @Test
    void should_save_reports_if_there_is_supplier_report() throws IOException, SQLException, JSONException {
        // given
        setupStorageConfig();
        LocalDate date = LocalDate.now();
        String content = Resources.toString(
            getResource("reconciliation/valid-supplier-statement.json"),
            UTF_8
        );

        UUID supplierId = UUID.randomUUID();
        var envelopeSupplierStatement = new EnvelopeSupplierStatement(
            supplierId,
            date,
            content,
            "1.0",
            LocalDateTime.now()
        );

        given(supplierStatementRepository.findLatest(date)).willReturn(Optional.of(envelopeSupplierStatement));

        List envelopeList = Arrays.asList(
            createEnvelope("1010404021234_14-08-2020-08-31.zip", "probate"),
            createEnvelope("9810404021234_14-08-2020-03-08-31.zip", "sscs"),
            createEnvelope("3108198112345_14-05-2020-10-11-21.zip", "crime"),
            createEnvelope("7171711717_8-05-2020-09-08-31.zip", "pcq")
        );
        given(envelopeService.getEnvelopes(date)).willReturn(envelopeList);
        given(summaryReportCreator.createSummaryReport(any(), any()))
            .willReturn(
                new SummaryReport(
                    120,
                    120,
                    List.of(new SummaryReportItem("12312.31312.312.zip", "sscs")),
                    List.of(new SummaryReportItem("9810404021234_14-08-2020-03-08-31.zip", "cmc"))
                )
            );

        //when
        summaryReportService.process(date);

        //then
        verify(reconciliationReportRepository, times(3))
            .save(newReconciliationReportCaptor.capture());
        List<NewReconciliationReport> allCapturedValues = newReconciliationReportCaptor.getAllValues();
        TargetStorageAccount[] targetStorageAccounts = TargetStorageAccount.values();


        for (int i = 0; i < 3; i++) {
            var newReconciliationReport = allCapturedValues.get(i);

            assertThat(newReconciliationReport.supplierStatementId).isEqualTo(supplierId);
            assertThat(newReconciliationReport.contentTypeVersion).isEqualTo("1.0");
            assertThat(newReconciliationReport.account).isEqualTo(targetStorageAccounts[i].name());

            String summaryContent = Resources.toString(
                getResource("reconciliation/summary-report/summary-report-with-both-discrepancy.json"),
                UTF_8
            );

            assertThat(newReconciliationReport.detailedContent).isNull();
            JSONAssert.assertEquals(newReconciliationReport.summaryContent, summaryContent, true);
        }
    }

    private static Envelope createEnvelope(String fileName, String container) {
        return new Envelope(
            UUID.randomUUID(),
            container,
            fileName,
            Instant.now(),
            Instant.now(),
            Instant.now(),
            Status.CREATED,
            false,
            false
        );
    }

    private void setupStorageConfig() {
        given(storageConfig.get("sscs")).willReturn(createStorageConfigItem(BULKSCAN));
        given(storageConfig.get("probate")).willReturn(createStorageConfigItem(BULKSCAN));
        given(storageConfig.get("cmc")).willReturn(createStorageConfigItem(BULKSCAN));
        given(storageConfig.get("crime")).willReturn(createStorageConfigItem(CRIME));
        given(storageConfig.get("pcq")).willReturn(createStorageConfigItem(PCQ));
    }

    private StorageConfigItem createStorageConfigItem(TargetStorageAccount targetStorageAccount) {
        StorageConfigItem storageConfigItem = new StorageConfigItem();
        storageConfigItem.setTargetStorageAccount(targetStorageAccount);
        return storageConfigItem;
    }
}
