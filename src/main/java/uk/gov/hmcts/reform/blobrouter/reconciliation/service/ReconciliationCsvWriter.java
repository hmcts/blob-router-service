package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.DiscrepancyItem;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.ReconciliationReportResponse;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReportItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * The `ReconciliationCsvWriter` class in Java provides methods to write summary and detailed reconciliation
 * reports to CSV files based on input data structures.
 */
@Component
public class ReconciliationCsvWriter {

    private static final String RECEIVED_NOT_REPORTED_PROBLEM = "Received but not Reported";
    private static final String REPORTED_NOT_RECEIVED_PROBLEM = "Reported but not Received";
    private static final String[] SUMMARY_RECONCILIATION_CSV_HEADERS = {"Problem", "Zip File Name", "Container"};
    private static final String[] DETAILED_RECONCILIATION_CSV_HEADERS =
        {"Zip File Name", "Container", "Type", "Stated", "Actual"};

    /**
     * The function writes a summary reconciliation report to a CSV file and returns the file.
     *
     * @param data The `writeSummaryReconciliationToCsv` method takes a `SummaryReport` object as input, which contains
     *             information needed to generate a summary reconciliation CSV file. The method creates a
     *             temporary CSV file, writes the summary data to the file using a CSV printer, and then returns
     *             the generated CSV file.
     * @return The method `writeSummaryReconciliationToCsv` returns a `File`
     *      object, which represents the CSV file that was created and written with the
     *      summary reconciliation data.
     */
    public File writeSummaryReconciliationToCsv(SummaryReport data) throws IOException {

        File csvFile = File.createTempFile("Reconciliation-summary-", ".csv");

        CSVFormat csvFileHeader = CSVFormat.DEFAULT.withHeader(SUMMARY_RECONCILIATION_CSV_HEADERS);

        try (
            FileWriter fileWriter = new FileWriter(csvFile);
            CSVPrinter printer = new CSVPrinter(fileWriter, csvFileHeader)
        ) {
            printSummaryRecords(printer, RECEIVED_NOT_REPORTED_PROBLEM, data.receivedButNotReported);
            printSummaryRecords(printer, REPORTED_NOT_RECEIVED_PROBLEM, data.reportedButNotReceived);
        }
        return csvFile;
    }

    /**
     * The `printSummaryRecords` method prints summary records for a given problem
     * and list of `SummaryReportItem` objects using a `CSVPrinter`.
     *
     * @param printer            The `printer` parameter is of type `CSVPrinter`, which is used to write CSV
     *                           records to an output destination, such as a file or a stream.
     *                           In the `printSummaryRecords` method, it is used to print summary records
     *                           for a given problem along with information from the `summaryReportItems`.
     * @param problem            The `problem` parameter is a `String` representing the problem associated
     *                           with the summary records being printed.
     * @param summaryReportItems The `summaryReportItems` parameter is a list of `SummaryReportItem` objects. Each
     *                           `SummaryReportItem` object likely contains information related to a summary
     *                           report, such as the `zipFileName` and `container` properties that are being
     *                           accessed in the `printSummaryRecords` method.
     */
    private void printSummaryRecords(
        CSVPrinter printer,
        String problem,
        List<SummaryReportItem> summaryReportItems
    ) throws IOException {
        for (var summary : summaryReportItems) {
            printer.printRecord(problem, summary.zipFileName, summary.container);
        }
    }

    /**
     * The `writeDetailedReconciliationToCsv` function writes a detailed reconciliation report to a temporary CSV file
     * based on the provided `ReconciliationReportResponse` data.
     *
     * @param data The `writeDetailedReconciliationToCsv` method takes a `ReconciliationReportResponse` object named
     *             `data` as a parameter. This object contains a list of `DiscrepancyItem` objects, each
     *             representing a discrepancy in the reconciliation report.
     * @return The method `writeDetailedReconciliationToCsv` is returning a `File` object, which represents the CSV file
     * that was created and written with the reconciliation data provided in the `ReconciliationReportResponse` object.
     */
    public File writeDetailedReconciliationToCsv(ReconciliationReportResponse data)
        throws IOException {
        File csvFile = File.createTempFile("Reconciliation-detailed-", ".csv");
        CSVFormat csvFileHeader = CSVFormat.DEFAULT.withHeader(DETAILED_RECONCILIATION_CSV_HEADERS);

        try (
            FileWriter fileWriter = new FileWriter(csvFile);
            CSVPrinter printer = new CSVPrinter(fileWriter, csvFileHeader)
        ) {
            for (DiscrepancyItem item : data.items) {
                printer.printRecord(
                    item.zipFileName,
                    item.container,
                    item.type,
                    item.stated,
                    item.actual
                );
            }
        }
        return csvFile;
    }
}
