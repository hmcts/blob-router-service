package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.clients.bulkscanprocessor.BulkScanProcessorClient;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CFT;

@ExtendWith(MockitoExtension.class)
class CftDetailedReportServiceTest {

    @Mock private ReconciliationMapper reconciliationMapper;
    @Mock private ReconciliationService reconciliationService;
    @Mock private BulkScanProcessorClient bulkScanProcessorClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private ReconciliationReportRepository reconciliationReportRepository;

    private CftDetailedReportService service;

    @BeforeEach
    void setUp() {
        service = new CftDetailedReportService(
            reconciliationMapper,
            reconciliationReportRepository,
            reconciliationService,
            bulkScanProcessorClient,
            objectMapper
        );
    }

    @Test
    void should_not_process_further_if_there_is_no_summary_report() {
        //given
        LocalDate date = LocalDate.now();
        given(reconciliationReportRepository.getLatestReconciliationReport(date, CFT.name()))
            .willReturn(Optional.empty());

        //when
        service.process(date);

        // then
        verify(reconciliationReportRepository).getLatestReconciliationReport(date, CFT.name());
        verifyNoMoreInteractions(reconciliationReportRepository);
        verifyNoInteractions(reconciliationService);
        verifyNoInteractions(reconciliationMapper);
        verifyNoInteractions(bulkScanProcessorClient);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void should_not_process_further_if_detailed_report_already_present() {
        //given
        LocalDate date = LocalDate.now();
        given(reconciliationReportRepository.getLatestReconciliationReport(date, CFT.name()))
            .willReturn(Optional.of(createReconciliationReport("{}", "{\"report\" :1}")));

        //when
        service.process(date);

        // then
        verify(reconciliationReportRepository).getLatestReconciliationReport(date, CFT.name());
        verifyNoMoreInteractions(reconciliationReportRepository);
        verifyNoInteractions(reconciliationService);
        verifyNoInteractions(reconciliationMapper);
        verifyNoInteractions(bulkScanProcessorClient);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void should_not_process_further_if_no_supplier_statement() {
        //given
        LocalDate date = LocalDate.now();
        given(reconciliationReportRepository.getLatestReconciliationReport(date, CFT.name()))
            .willReturn(Optional.of(createReconciliationReport("{\"a\" :1}", null)));
        given(reconciliationService.getSupplierStatement(date)).willReturn(Optional.empty());

        //when
        service.process(date);

        // then
        verify(reconciliationService).getSupplierStatement(date);
        verifyNoMoreInteractions(reconciliationService);
        verifyNoInteractions(reconciliationMapper);
        verifyNoInteractions(bulkScanProcessorClient);
        verifyNoInteractions(objectMapper);
        verify(reconciliationReportRepository).getLatestReconciliationReport(date, CFT.name());
        verifyNoMoreInteractions(reconciliationReportRepository);
    }

    @Test
    void should_process_further_if_no_detailed_report() throws IOException, SQLException {
        //given
        LocalDate date = LocalDate.now();
        var reconciliationReport = createReconciliationReport("{\"a\" :1}", null);
        given(reconciliationReportRepository.getLatestReconciliationReport(date, CFT.name()))
            .willReturn(Optional.of(reconciliationReport));

        String content = Resources.toString(
            getResource("reconciliation/valid-supplier-statement.json"),
            UTF_8
        );

        var envelopeSupplierStatement =
            new EnvelopeSupplierStatement(
                reconciliationReport.supplierStatementId,
                date,
                content,
                "1.0",
                LocalDateTime.now()
            );

        given(reconciliationService.getSupplierStatement(date))
            .willReturn(Optional.of(envelopeSupplierStatement));

        var reconciliationStatement = mock(ReconciliationStatement.class);
        given(reconciliationMapper.convertToReconciliationStatement(any(), any()))
            .willReturn(reconciliationStatement);

        var reconciliationReportResponse = mock(ReconciliationReportResponse.class);
        given(bulkScanProcessorClient.postReconciliationReport(reconciliationStatement))
            .willReturn(reconciliationReportResponse);

        String contentJson = "content json";
        given(objectMapper.writeValueAsString(reconciliationReportResponse))
            .willReturn(contentJson);

        //when
        service.process(date);

        // then
        verify(reconciliationService).getSupplierStatement(date);
        verifyNoMoreInteractions(reconciliationService);
        verify(reconciliationMapper).convertToReconciliationStatement(any(), eq(CFT));
        verifyNoMoreInteractions(reconciliationMapper);
        verify(bulkScanProcessorClient).postReconciliationReport(reconciliationStatement);
        verifyNoMoreInteractions(bulkScanProcessorClient);
        verify(objectMapper).writeValueAsString(reconciliationReportResponse);
        verifyNoMoreInteractions(objectMapper);
        verify(reconciliationReportRepository).getLatestReconciliationReport(date, CFT.name());
        verify(reconciliationReportRepository)
            .updateDetailedContent(reconciliationReport.id, contentJson);
        verifyNoMoreInteractions(reconciliationReportRepository);
    }

    private static ReconciliationReport createReconciliationReport(
        String summaryContent,
        String detailedContent
    ) {
        return new ReconciliationReport(
            UUID.randomUUID(),
            UUID.randomUUID(),
            CFT.name(),
            summaryContent,
            detailedContent,
            "1.0",
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}


