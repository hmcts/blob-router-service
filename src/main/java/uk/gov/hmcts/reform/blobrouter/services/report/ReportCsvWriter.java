package uk.gov.hmcts.reform.blobrouter.services.report;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeSummaryItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

@Component
public class ReportCsvWriter {

    private static final String[] ENVELOPES_SUMMARY_CSV_HEADERS = {
        "Container", "Zip File Name", "Date Received", "Time Received", "Date Processed", "Time Processed", "Status"
    };

    public File writeEnvelopesSummaryToCsv(
        List<EnvelopeSummaryItem> data
    ) throws IOException {
        File csvFile = File.createTempFile("Zipfiles-summary-", ".csv");

        CSVFormat csvFileHeader = CSVFormat.DEFAULT.withHeader(ENVELOPES_SUMMARY_CSV_HEADERS);

        try (
            FileWriter fileWriter = new FileWriter(csvFile);
            CSVPrinter printer = new CSVPrinter(fileWriter, csvFileHeader)
        ) {
            for (EnvelopeSummaryItem summary : Optional.ofNullable(data).orElse(emptyList())) {
                printer.printRecord(
                    summary.container,
                    summary.fileName,
                    summary.dateReceived,
                    summary.timeReceived,
                    summary.dateProcessed,
                    summary.timeProcessed,
                    summary.status
                );
            }
        }
        return csvFile;
    }
}
