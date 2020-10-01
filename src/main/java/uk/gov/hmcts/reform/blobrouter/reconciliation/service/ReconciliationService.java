package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.ReconciliationReportRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.reports.model.ReconciliationReport;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.SupplierStatementRepository;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.EnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.data.reconciliation.statements.model.NewEnvelopeSupplierStatement;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidSupplierStatementException;
import uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.SupplierStatement;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReconciliationService {

    private final SupplierStatementRepository statementRepo;
    private final ReconciliationReportRepository reportRepo;
    private final ObjectMapper objectMapper;
    private final ServiceConfiguration serviceConfig;

    public ReconciliationService(
        SupplierStatementRepository statementRepo,
        ReconciliationReportRepository reportRepo,
        ObjectMapper objectMapper,
        ServiceConfiguration serviceConfig
    ) {
        this.statementRepo = statementRepo;
        this.reportRepo = reportRepo;
        this.objectMapper = objectMapper;
        this.serviceConfig = serviceConfig;
    }

    @Transactional
    public UUID saveSupplierStatement(LocalDate date, SupplierStatement inputSupplierStatement) {
        try {
            validateContainers(inputSupplierStatement);
            String supplierStatement = objectMapper.writeValueAsString(inputSupplierStatement);
            var statement = new NewEnvelopeSupplierStatement(
                date,
                supplierStatement,
                "1.0" //TODO: should save different versions
            );
            return statementRepo.save(statement);
        } catch (JsonProcessingException | SQLException e) {
            throw new InvalidSupplierStatementException("Failed to process Supplier statement", e);
        }
    }

    private void validateContainers(SupplierStatement inputSupplierStatement) {
        if (inputSupplierStatement.envelopes != null) {
            List<String> unrecognizedContainers = new ArrayList<String>();
            inputSupplierStatement
                .envelopes
                .stream()
                .map(e -> e.container)
                .distinct()
                .forEach(c -> {
                    if (!serviceConfig.getSourceContainers().contains(c.toLowerCase())) {
                        unrecognizedContainers.add(c);
                    }
                });

            if (!unrecognizedContainers.isEmpty()) {
                throw new InvalidSupplierStatementException(
                    "Invalid statement. Unrecognized Containers :"
                        + unrecognizedContainers.toString());
            }
        }
    }

    public Optional<EnvelopeSupplierStatement> getSupplierStatement(LocalDate date) {
        return statementRepo.findLatest(date);
    }

    public List<ReconciliationReport> getReconciliationReports(LocalDate date) {
        return reportRepo.findByDate(date);
    }
}
