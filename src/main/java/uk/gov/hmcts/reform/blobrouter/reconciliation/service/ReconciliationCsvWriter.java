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

@Component
public class ReconciliationCsvWriter {

    private static final String RECEIVED_NOT_REPORTED_PROBLEM = "Received but not Reported";
    private static final String REPORTED_NOT_RECEIVED_PROBLEM = "Reported but not Received";
    private static final String[] SUMMARY_RECONCILIATION_CSV_HEADERS = {"Problem", "Zip File Name", "Container"};
    private static final String[] DETAILED_RECONCILIATION_CSV_HEADERS =
        {"Zip File Name", "Container", "Type", "Stated", "Actual"};

    public File writeSummaryReconciliationToCsv(SummaryReport data) throws IOException {

        File csvFile = File.createTempFile("Reconciliation-summary-", ".csv");

        CSVFormat csvFileHeader = CSVFormat.DEFAULT.withHeader(SUMMARY_RECONCILIATION_CSV_HEADERS);
        FileWriter fileWriter = new FileWriter(csvFile);

        try (CSVPrinter printer = new CSVPrinter(fileWriter, csvFileHeader)) {
            printSummaryRecords(printer, RECEIVED_NOT_REPORTED_PROBLEM, data.receivedButNotReported);
            printSummaryRecords(printer, REPORTED_NOT_RECEIVED_PROBLEM, data.reportedButNotReceived);
        }
        return csvFile;
    }

    private void printSummaryRecords(
        CSVPrinter printer,
        String problem,
        List<SummaryReportItem> summaryReportItems
    ) throws IOException {
        for (var summary : summaryReportItems) {
            printer.printRecord(problem, summary.zipFileName, summary.container);
        }
    }

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
