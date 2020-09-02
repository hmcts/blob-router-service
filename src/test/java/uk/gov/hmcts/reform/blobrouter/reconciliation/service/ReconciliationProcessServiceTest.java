package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.NewReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationMapper;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationReportResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationStatement;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.BULKSCAN;

@ExtendWith(MockitoExtension.class)
class ReconciliationProcessServiceTest {

    @Mock private ReconciliationMapper reconciliationMapper;
    @Mock private ReconciliationService reconciliationService;
    @Mock private BulkScanProcessorClient bulkScanProcessorClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private ReconciliationReportRepository reconciliationReportRepository;

    private ReconciliationProcessService service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationProcessService(
            reconciliationMapper,
            reconciliationService,
            bulkScanProcessorClient,
            objectMapper,
            reconciliationReportRepository
        );
    }

    @Test
    void should_only_interact_reconciliationService_when_no_supplier_statement() {
        //given
        LocalDate date = LocalDate.now();
        given(reconciliationService.getSupplierStatement(date)).willReturn(Optional.empty());

        //when
        service.process(date);

        // then
        verify(reconciliationService).getSupplierStatement(date);
        verifyNoMoreInteractions(reconciliationService);
        verifyNoInteractions(reconciliationMapper);
        verifyNoInteractions(bulkScanProcessorClient);
        verifyNoInteractions(objectMapper);
        verifyNoInteractions(reconciliationReportRepository);
    }

    @Test
    void should_throw_exception_when_getting_supplier_statement() {
        //given
        LocalDate date = LocalDate.now();
        given(reconciliationService.getSupplierStatement(date))
            .willThrow(new RuntimeException("Random Error: Sql exception"));

        //when
        assertThrows(
            RuntimeException.class,
            () -> service.process(date)
        );

        // then
        verify(reconciliationService).getSupplierStatement(date);
        verifyNoMoreInteractions(reconciliationService);
    }

    @Test
    void should_continue_process_when_exception_occurs_while_processing_storage_account() throws IOException {
        //given
        LocalDate date = LocalDate.now();
        EnvelopeSupplierStatement supplierStatement = mock(EnvelopeSupplierStatement.class);
        given(reconciliationService.getSupplierStatement(date)).willReturn(Optional.of(supplierStatement));

        var reconciliationStatement = mock(ReconciliationStatement.class);
        given(reconciliationMapper.convertToReconciliationStatement(any(), any()))
            .willReturn(reconciliationStatement);

        given(bulkScanProcessorClient.postReconciliationReport(reconciliationStatement))
            .willThrow(new RuntimeException("Post to processor failed"));

        //when
        service.process(date);

        //then
        verify(reconciliationService).getSupplierStatement(date);
        verifyNoMoreInteractions(reconciliationService);
        for (var targetStorageAccount : TargetStorageAccount.values()) {
            verify(reconciliationMapper)
                .convertToReconciliationStatement(any(), eq(targetStorageAccount));
        }
        verifyNoMoreInteractions(reconciliationMapper);
    }

    @Test
    void should_process_when_there_is_supplier_statement() throws IOException, SQLException {
        //given
        LocalDate date = LocalDate.now();

        String content = Resources.toString(
            getResource("reconciliation/valid-supplier-statement.json"),
            UTF_8
        );
        var supplierStatementId =  UUID.randomUUID();
        var envelopeSupplierStatement =
            new EnvelopeSupplierStatement(
                supplierStatementId,
                date,
                content,
                "1.0",
                LocalDateTime.now()
            );

        given(reconciliationService.getSupplierStatement(date)).willReturn(Optional.of(envelopeSupplierStatement));

        var reconciliationStatement = mock(ReconciliationStatement.class);
        given(reconciliationMapper.convertToReconciliationStatement(any(), any()))
            .willReturn(reconciliationStatement);

        var reconciliationReportResponse = mock(ReconciliationReportResponse.class);
        given(bulkScanProcessorClient.postReconciliationReport(reconciliationStatement))
            .willReturn(reconciliationReportResponse);

        String contentJson = "content json";
        given(objectMapper.writeValueAsString(reconciliationReportResponse))
            .willReturn(contentJson);

        given(reconciliationReportRepository.save(any()))
            .willReturn(UUID.randomUUID());


        //when
        service.process(date);

        // then
        verify(reconciliationService).getSupplierStatement(date);
        verifyNoMoreInteractions(reconciliationService);
        verify(reconciliationMapper, times(3))
            .convertToReconciliationStatement(any(), any(TargetStorageAccount.class));

        verify(objectMapper).writeValueAsString(reconciliationReportResponse);
        ArgumentCaptor<NewReconciliationReport> reportCaptor = ArgumentCaptor.forClass(NewReconciliationReport.class);
        verify(reconciliationReportRepository).save(reportCaptor.capture());
        var newReconciliationReport = reportCaptor.getValue();
        assertThat(newReconciliationReport.account).isEqualTo(BULKSCAN.name());
        assertThat(newReconciliationReport.content).isEqualTo(contentJson);
        assertThat(newReconciliationReport.contentTypeVersion).isEqualTo("1.0");
        assertThat(newReconciliationReport.supplierStatementId).isEqualTo(supplierStatementId);

        verifyNoMoreInteractions(reconciliationService);
        verifyNoMoreInteractions(reconciliationMapper);
        verifyNoMoreInteractions(bulkScanProcessorClient);
        verifyNoMoreInteractions(objectMapper);
        verifyNoMoreInteractions(reconciliationReportRepository);
    }
}