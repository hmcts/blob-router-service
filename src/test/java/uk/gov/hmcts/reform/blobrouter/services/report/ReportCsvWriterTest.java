package uk.gov.hmcts.reform.blobrouter.services.report;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;
import uk.gov.hmcts.reform.blobrouter.model.out.EnvelopeSummaryItem;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

public class ReportCsvWriterTest {

    private static final Tuple HEADERS = tuple(
        "Container",
        "Zip File Name",
        "Date Received",
        "Time Received",
        "Date Processed",
        "Time Processed",
        "Status"
    );

    private ReportCsvWriter reportCsvWriter;

    @BeforeEach
    void setUp() {
        reportCsvWriter = new ReportCsvWriter();
    }

    @Test
    void should_return_csv_file_with_headers_and_csv_records() throws IOException {
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        //given
        List<EnvelopeSummaryItem> csvData = Arrays.asList(
            new EnvelopeSummaryItem(
                "bulkscan",
                "test1.zip",
                date,
                time,
                date,
                time,
                Status.DISPATCHED.name(),
                true
            ),
            new EnvelopeSummaryItem(
                "bulkscan",
                "test2.zip",
                date,
                time,
                date,
                time,
                Status.DISPATCHED.name(),
                true
            )
        );

        //when
        File summaryToCsv = reportCsvWriter.writeEnvelopesSummaryToCsv(csvData);

        //then
        List<CSVRecord> csvRecordList = readCsv(summaryToCsv);

        assertThat(csvRecordList)
            .isNotEmpty()
            .hasSize(3)
            .extracting(this::getTupleFromCsvRecord)
            .containsExactly(
                HEADERS,
                tuple(
                    "bulkscan",
                    "test1.zip",
                    date.toString(),
                    time.toString(),
                    date.toString(),
                    time.toString(),
                    Status.DISPATCHED.name()
                ),
                tuple(
                    "bulkscan",
                    "test2.zip",
                    date.toString(),
                    time.toString(),
                    date.toString(),
                    time.toString(),
                    Status.DISPATCHED.name()
                )
            );
    }

    @Test
    void should_return_csv_file_with_only_headers_when_the_data_is_null() throws IOException {
        //when
        File summaryToCsv = reportCsvWriter.writeEnvelopesSummaryToCsv(null);

        //then
        List<CSVRecord> csvRecordList = readCsv(summaryToCsv);

        assertThat(csvRecordList)
            .isNotEmpty()
            .hasSize(1)
            .extracting(this::getTupleFromCsvRecord)
            .containsExactly(
                HEADERS
            );
    }

    @Test
    void should_return_csv_file_with_only_headers_when_the_data_is_empty() throws IOException {
        //when
        File summaryToCsv = reportCsvWriter.writeEnvelopesSummaryToCsv(emptyList());

        //then
        List<CSVRecord> csvRecordList = readCsv(summaryToCsv);

        assertThat(csvRecordList)
            .isNotEmpty()
            .hasSize(1)
            .extracting(this::getTupleFromCsvRecord)
            .containsExactly(
                HEADERS
            );
    }

    private List<CSVRecord> readCsv(File summaryToCsv) throws IOException {
        return CSVFormat.DEFAULT.parse(new FileReader(summaryToCsv)).getRecords();
    }

    private Tuple getTupleFromCsvRecord(CSVRecord data) {
        return tuple(
            data.get(0), data.get(1), data.get(2), data.get(3), data.get(4), data.get(5), data.get(6));
    }
}
