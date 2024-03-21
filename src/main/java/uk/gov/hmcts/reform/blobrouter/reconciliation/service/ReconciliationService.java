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

    /**
     * This function saves a supplier statement for a specific date after validating and processing the input.
     *
     * @param date The `date` parameter in the `saveSupplierStatement` method represents the date for which the supplier
     *             statement is being saved. It is of type `LocalDate`, which is a date without a time
     *             zone in the ISO-8601 calendar system, such as 2022-01-31.
     * @param inputSupplierStatement The `inputSupplierStatement` parameter is an object of type
     *                               `SupplierStatement` that contains information about a supplier statement.
     *                               This information is being converted to a JSON string using an
     *                               `ObjectMapper` before being saved to the database.
     * @return The method `saveSupplierStatement` is returning a `UUID` value.
     */
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

    /**
     * The function `validateContainers` checks if the containers in the input supplier
     * statement are recognized based on a list of source containers and throws an exception
     * if any unrecognized containers are found.
     *
     * @param inputSupplierStatement The `validateContainers` method you provided is used to validate
     *                               the containers in the `inputSupplierStatement`. It checks if
     *                               the containers in the statement are recognized based on the list of source
     *                               containers from the service configuration.
     */
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

    /**
     * The function `getSupplierStatement` retrieves a supplier statement by their UUID
     * from the statement repository as an Optional.
     *
     * @param supplierId The `supplierId` parameter is a unique identifier for a supplier. It is of
     *                   type `UUID`, which stands for Universally Unique Identifier. This identifier
     *                   is used to retrieve the supplier statement from the repository.
     * @return An Optional object containing an EnvelopeSupplierStatement for the given supplierId
     *      is being returned.
     */
    public Optional<EnvelopeSupplierStatement> getSupplierStatement(UUID supplierId) {
        return statementRepo.findById(supplierId);
    }

    /**
     * This Java function retrieves reconciliation reports for a specific date from a repository.
     *
     * @param date The `date` parameter is of type `LocalDate` and represents the date for which
     *             reconciliation reports are being retrieved.
     * @return A List of ReconciliationReport objects that match the given LocalDate date.
     */
    public List<ReconciliationReport> getReconciliationReports(LocalDate date) {
        return reportRepo.findByDate(date);
    }
}
