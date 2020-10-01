package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.SupplierStatementRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.NewEnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidSupplierStatementException;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.Envelope;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.SupplierStatement;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    private ReconciliationService service;

    @Mock
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SupplierStatementRepository repository;

    @Mock
    private ReconciliationReportRepository reconciliationReportRepository;

    @Mock
    private ServiceConfiguration serviceConfig;

    @BeforeEach
    void setUp() {
        service = new ReconciliationService(repository, reconciliationReportRepository, objectMapper, serviceConfig);
    }

    @Test
    void should_save_when_the_supplier_statement_is_valid() throws Exception {
        // given
        given(serviceConfig.getSourceContainers()).willReturn(List.of("bulkscan"));
        var supplierStatement = new SupplierStatement(singletonList(
            new Envelope("a.zip", null, "bulkscan", "BULKSCAN", singletonList("1234"), singletonList("123"))
        ));

        String content = "{\"envelopes\":{}}";
        given(objectMapper.writeValueAsString(any())).willReturn(content);

        var uuidFromDb = UUID.randomUUID();
        given(repository.save(any())).willReturn(uuidFromDb);

        // when
        var uuid = service.saveSupplierStatement(LocalDate.now(), supplierStatement);

        // then
        assertThat(uuid).isEqualTo(uuidFromDb);

        var newStatementCaptor = ArgumentCaptor.forClass(NewEnvelopeSupplierStatement.class);
        verify(repository).save(newStatementCaptor.capture());

        var statement = newStatementCaptor.getValue();
        assertThat(statement.date).isEqualTo(LocalDate.now());
        assertThat(statement.content).isEqualTo(content);
        assertThat(statement.contentTypeVersion).isEqualTo("1.0");
    }

    @Test
    void should_throw_exception_when_supplier_statement_is_invalid() throws Exception {
        // given
        given(repository.save(any())).willThrow(SQLException.class);

        // when
        var exc = catchThrowable(
            () -> service.saveSupplierStatement(LocalDate.now(), new SupplierStatement(emptyList()))
        );

        // then
        assertThat(exc)
            .isInstanceOf(InvalidSupplierStatementException.class)
            .hasMessageContaining("Failed to process Supplier statement");
    }

    @Test
    void should_throw_exception_when_supplier_statement_has_invalid_containers() throws Exception {
        // given
        given(serviceConfig.getSourceContainers()).willReturn(List.of("bulkscan"));
        var supplierStatement = new SupplierStatement(
            List.of(
                new Envelope("a.zip", null, "c1", "C1", singletonList("12"), singletonList("1")),
                new Envelope("q.x", null, "BULKSCAN", "B", singletonList("4"), singletonList("3"))
            )
        );

        // when
        var exc = catchThrowable(
            () -> service.saveSupplierStatement(LocalDate.now(),supplierStatement)
        );

        // then
        assertThat(exc)
            .isInstanceOf(InvalidSupplierStatementException.class)
            .hasMessageContaining("Invalid statement. Unrecognized Containers :[c1]");
    }

    @Test
    void should_throw_exception_when_supplier_statement_json_processing_fails() throws Exception {
        // given
        given(objectMapper.writeValueAsString(any())).willThrow(JsonProcessingException.class);

        // when
        var exc = catchThrowable(
            () -> service.saveSupplierStatement(LocalDate.now(), new SupplierStatement(emptyList()))
        );

        // then
        assertThat(exc)
            .isInstanceOf(InvalidSupplierStatementException.class)
            .hasMessageContaining("Failed to process Supplier statement");
    }

    @Test
    void should_return_supplier_statement_as_it_is_in_repository() {
        // given
        LocalDate date = LocalDate.now();
        var expectedResponse = Optional.of(mock(EnvelopeSupplierStatement.class));

        given(repository.findLatest(date)).willReturn(expectedResponse);

        // when
        var response = service.getSupplierStatement(date);

        // then
        assertThat(response).isSameAs(expectedResponse);
    }

    @Test
    void should_throw_exception_when_repository_throws() {
        // given
        LocalDate date = LocalDate.now();

        given(repository.findLatest(date))
            .willThrow(new RuntimeException("Repository exception"));

        // when
        // then
        assertThrows(
            RuntimeException.class,
            () -> service.getSupplierStatement(date)
        );

    }

    @Test
    @SuppressWarnings("unchecked")
    void should_return_reconciliation_report_as_it_is_in_repository() {
        // given
        LocalDate date = LocalDate.now();
        var expectedResponse = mock(List.class);

        given(reconciliationReportRepository.findByDate(date)).willReturn(expectedResponse);

        // when
        var response = service.getReconciliationReports(date);

        // then
        assertThat(response).isSameAs(expectedResponse);
    }

}
