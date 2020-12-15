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
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CFT;
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
    void should_continue_if_there_is_no_supplier_statement() throws JsonProcessingException {
        // given
        LocalDate date = LocalDate.now();
        given(supplierStatementRepository.findLatest(date)).willReturn(Optional.empty());

        // when
        summaryReportService.process(date);

        // then
        verify(supplierStatementRepository).findLatest(date);
        verifyNoMoreInteractions(supplierStatementRepository);
        verifyNoInteractions(envelopeService);
        verifyNoInteractions(reconciliationReportRepository);
    }

    @Test
    void should_stop_process_if_reports_already_ready() throws JsonProcessingException {
        // given
        LocalDate date = LocalDate.now();
        var envelopeSupplierStatement = new EnvelopeSupplierStatement(
            UUID.randomUUID(),
            date,
            "{wrong_data}",
            "1.0",
            LocalDateTime.now()
        );
        var option = mock(Optional.class);
        given(supplierStatementRepository.findLatest(date)).willReturn(option);
        given(option.isPresent()).willReturn(true);
        given(option.get()).willReturn(envelopeSupplierStatement);

        var existingReportList = List.of(
            getReconciliationReport(envelopeSupplierStatement.id, CFT.name()),
            getReconciliationReport(envelopeSupplierStatement.id, CRIME.name()),
            getReconciliationReport(envelopeSupplierStatement.id, PCQ.name())
        );

        given(reconciliationReportRepository
            .findByStatementId(envelopeSupplierStatement.id))
            .willReturn(existingReportList);

        // when
        summaryReportService.process(date);

        // then
        verify(option).isPresent();
        verify(option).get();
        verifyNoMoreInteractions(option);
        verifyNoInteractions(envelopeService);
        verify(reconciliationReportRepository).findByStatementId(envelopeSupplierStatement.id);
        verifyNoMoreInteractions(reconciliationReportRepository);
    }

    @Test
    void should_stop_process_if_parsing_supplier_statements_json_fails() throws JsonProcessingException {
        // given
        LocalDate date = LocalDate.now();
        var envelopeSupplierStatement = new EnvelopeSupplierStatement(
            UUID.randomUUID(),
            date,
            "{wrong_data}",
            "1.0",
            LocalDateTime.now()
        );
        var option = mock(Optional.class);
        given(supplierStatementRepository.findLatest(date)).willReturn(option);
        given(option.isPresent()).willReturn(true);
        given(option.get()).willReturn(envelopeSupplierStatement);

        // when
        summaryReportService.process(date);

        // then
        verify(option).isPresent();
        verify(option).get();
        verifyNoMoreInteractions(option);
        verifyNoInteractions(envelopeService);
        verify(reconciliationReportRepository).findByStatementId(envelopeSupplierStatement.id);
        verifyNoMoreInteractions(reconciliationReportRepository);
    }

    @Test
    // target storage 'bulkscan' will get exception continue with other target storage
    void should_continue_processing_with_next_target_storage_if_one_fails()
        throws Exception {
        // given
        given(storageConfig.get(anyString())).willReturn(createStorageConfigItem(CFT));
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

        var summaryReportItemList = Arrays.asList(
            createSummaryReportItem("1010404021234_14-08-2020-08-31.zip", "probate")
        );
        given(summaryReportCreator.createSummaryReport(eq(summaryReportItemList), notNull()))
            .willThrow(new RuntimeException("Can not create summary for Bulkscan"));

        given(summaryReportCreator.createSummaryReport(null, null))
            .willReturn(
                new SummaryReport(
                    120,
                    120,
                    List.of(new SummaryReportItem("12312.31312.312.zip", "sscs")),
                    List.of(new SummaryReportItem("9810404021234_14-08-2020-03-08-31.zip", "cmc"))
                )
            );


        given(reconciliationReportRepository.save(any()))
            .willReturn(UUID.randomUUID());
        //when
        summaryReportService.process(date);

        // then
        // should try for all target storage accounts
        verify(summaryReportCreator, times(TargetStorageAccount.values().length)).createSummaryReport(any(), any());
        // bulkscan gets exception so can not reach to save
        verify(reconciliationReportRepository, times(2)).save(any());

    }

    @Test
    void should_save_reports_if_there_is_supplier_report() throws IOException, SQLException, JSONException {
        // given
        setupStorageConfig();
        LocalDate date = LocalDate.now();

        // while finding target storage name should be case insensitive
        String content = Resources.toString(
            getResource("reconciliation/valid-supplier-statement-container-case-insensitive.json"),
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
            createEnvelope("12312.31312.312.zip", "sscs"),
            createEnvelope("10929292923_14-05-2020-09-08-31.zip", "crime"),
            createEnvelope("7171711717_8-05-2020-09-08-31.zip", "pcq"),
            createEnvelope("1231122-05-2020-10-08-31.zip", "pcq")
        );
        given(envelopeService.getEnvelopes(date)).willReturn(envelopeList);


        given(summaryReportCreator.createSummaryReport(any(), any())).willCallRealMethod();

        //when
        summaryReportService.process(date);

        //then
        verify(reconciliationReportRepository, times(3))
            .save(newReconciliationReportCaptor.capture());
        List<NewReconciliationReport> allCapturedValues = newReconciliationReportCaptor.getAllValues();
        TargetStorageAccount[] targetStorageAccounts = TargetStorageAccount.values();

        for (int i = 0; i < targetStorageAccounts.length; i++) {
            var newReconciliationReport = allCapturedValues.get(i);

            assertThat(newReconciliationReport.supplierStatementId).isEqualTo(supplierId);
            assertThat(newReconciliationReport.contentTypeVersion).isEqualTo("1.0");
            assertThat(newReconciliationReport.account).isEqualTo(targetStorageAccounts[i].name());

            String summaryContent = Resources.toString(
                getResource("reconciliation/summary-report/"+ targetStorageAccounts[i].name()
                    + "-summary-report-with-both-discrepancy.json"),
                UTF_8
            );

            assertThat(newReconciliationReport.detailedContent).isNull();
            JSONAssert.assertEquals(newReconciliationReport.summaryContent, summaryContent, true);
        }
    }

    @Test
    void should_save_only_missing_reports_skip_existing_reports() throws IOException, SQLException, JSONException {
        // given
        setupStorageConfig();
        LocalDate date = LocalDate.now();

        // while finding target storage name should be case insensitive
        String content = Resources.toString(
            getResource("reconciliation/valid-supplier-statement-container-case-insensitive.json"),
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

        ReconciliationReport reconciliationReport = getReconciliationReport(supplierId, PCQ.name());
        ReconciliationReport reconciliationReportForSameAccount2 = getReconciliationReport(supplierId, PCQ.name());
        ReconciliationReport reconciliationReportForSameAccount3 = getReconciliationReport(supplierId, PCQ.name());

        var existingReportList = List.of(
            reconciliationReport,
            reconciliationReportForSameAccount2,
            reconciliationReportForSameAccount3
        );

        given(reconciliationReportRepository
            .findByStatementId(envelopeSupplierStatement.id))
            .willReturn(existingReportList);

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
        verify(reconciliationReportRepository, times(2))
            .save(newReconciliationReportCaptor.capture());
        List<NewReconciliationReport> allCapturedValues = newReconciliationReportCaptor.getAllValues();
        TargetStorageAccount[] targetStorageAccounts = TargetStorageAccount.values();

        // skip report creation if there is already report
        for (int i = 0; i < 2; i++) {
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

    private ReconciliationReport getReconciliationReport(UUID supplierId, String account) {
        return new ReconciliationReport(
            UUID.randomUUID(),
            supplierId,
            account,
            "{}",
            "{}",
            "1.0",
            null, LocalDateTime.now()
        );
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
        given(storageConfig.get("sscs")).willReturn(createStorageConfigItem(CFT));
        given(storageConfig.get("probate")).willReturn(createStorageConfigItem(CFT));
        given(storageConfig.get("cmc")).willReturn(createStorageConfigItem(CFT));
        given(storageConfig.get("crime")).willReturn(createStorageConfigItem(CRIME));
        given(storageConfig.get("pcq")).willReturn(createStorageConfigItem(PCQ));
    }

    private StorageConfigItem createStorageConfigItem(TargetStorageAccount targetStorageAccount) {
        StorageConfigItem storageConfigItem = new StorageConfigItem();
        storageConfigItem.setTargetStorageAccount(targetStorageAccount);
        return storageConfigItem;
    }

    private static SummaryReportItem createSummaryReportItem(String fileName, String container) {
        return new SummaryReportItem(container, fileName);
    }
}
