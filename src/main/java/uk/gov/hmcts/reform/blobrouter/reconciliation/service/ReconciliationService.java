package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.SupplierStatementRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.NewEnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidSupplierStatementException;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.SupplierStatement;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReconciliationService {

    private final SupplierStatementRepository repository;
    private final ObjectMapper objectMapper;

    public ReconciliationService(SupplierStatementRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID saveSupplierStatement(LocalDate date, SupplierStatement inputSupplierStatement) {
        try {
            String supplierStatement = objectMapper.writeValueAsString(inputSupplierStatement);
            NewEnvelopeSupplierStatement statement = new NewEnvelopeSupplierStatement(
                date,
                supplierStatement,
                "1.0" //TODO: should save different versions
            );
            return repository.save(statement);
        } catch (JsonProcessingException | SQLException e) {
            throw new InvalidSupplierStatementException("Failed to process Supplier statement", e);
        }
    }

    public Optional<EnvelopeSupplierStatement> getSupplierStatement(LocalDate date) {
        return repository.findLatest(date);
    }

}
