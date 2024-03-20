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

/**
 * The `ReportCsvWriter` class in Java writes a list of `EnvelopeSummaryItem`
 * objects to a CSV file and returns the file.
 */
@Component
public class ReportCsvWriter {

    private static final String[] ENVELOPES_SUMMARY_CSV_HEADERS = {
        "Container", "Zip File Name", "Date Received", "Time Received", "Date Processed", "Time Processed", "Status"
    };

    /**
     * This Java function writes a list of EnvelopeSummaryItem objects to a CSV file and returns the file.
     *
     * @param data The `data` parameter in the `writeEnvelopesSummaryToCsv` method is a list of
     *             `EnvelopeSummaryItem` objects. Each `EnvelopeSummaryItem` object contains information
     *             about an envelope, such as the container, file name, date received, time received,
     *             date processed, time processed and status.
     * @return The method `writeEnvelopesSummaryToCsv` returns a `File` object which represents the CSV
     *      file that was created and written with the envelope summary data.
     */
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
