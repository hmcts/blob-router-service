package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class ReconciliationService {

    private static final Logger logger = getLogger(ReconciliationService.class);
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
            logger.info("Save supplier statement for date: {}", statement.date);
            return statementRepo.save(statement);
        } catch (JsonProcessingException | SQLException e) {
            throw new InvalidSupplierStatementException("Failed to process Supplier statement", e);
        }
    }

    private void validateContainers(SupplierStatement inputSupplierStatement) {
        if (inputSupplierStatement.envelopes != null) {
            List<String> unrecognizedContainers = inputSupplierStatement
                .envelopes
                .stream()
                .map(e -> e.container)
                .distinct()
                .filter(c -> !serviceConfig.getSourceContainers().contains(c.toLowerCase()))
                .collect(toList());


            if (!unrecognizedContainers.isEmpty()) {
                logger.error("Invalid statement. Unrecognized Containers :{}", unrecognizedContainers);
                throw new InvalidSupplierStatementException(
                    "Invalid statement. Unrecognized Containers : "
                        + unrecognizedContainers.toString());
            }
        }
    }

    public Optional<EnvelopeSupplierStatement> getSupplierStatement(UUID supplierId) {
        return statementRepo.findById(supplierId);
    }

    public List<ReconciliationReport> getReconciliationReports(LocalDate date) {
        return reportRepo.findByDate(date);
    }
}
