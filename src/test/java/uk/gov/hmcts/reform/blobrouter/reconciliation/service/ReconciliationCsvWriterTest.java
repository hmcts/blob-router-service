package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReportItem;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

class ReconciliationCsvWriterTest {

    private static final Tuple HEADERS = tuple(
        "Problem",
        "Zip File Name",
        "Container"
    );

    private static final String RECEIVED_NOT_REPORTED_PROBLEM = "Received but not Reported";
    private static final String REPORTED_NOT_RECEIVED_PROBLEM = "Reported but not Received";

    private ReconciliationCsvWriter reconciliationCsvWriter = new ReconciliationCsvWriter();

    @Test
    void should_return_csv_file_with_headers_and_csv_records() throws IOException {

        // given
        SummaryReport data = new SummaryReport(
            5,
            6,
            List.of(
                new SummaryReportItem("zip_file_1_1", "crime"),
                new SummaryReportItem("zip_file_2_2", "bulkscan")
            ),
            List.of(
                new SummaryReportItem("zip_file_A_A", "sscs"),
                new SummaryReportItem("zip_file_Z_Z", "pcq")
            )
        );
        // when
        File summaryToCsv = reconciliationCsvWriter.writeSummaryReconciliationToCsv(data);

        //then
        List<CSVRecord> csvRecordList = readCsv(summaryToCsv);

        assertThat(csvRecordList)
            .isNotEmpty()
            .hasSize(5)
            .extracting(this::getTupleFromCsvRecord)
            .containsExactly(
                HEADERS,
                tuple(
                    RECEIVED_NOT_REPORTED_PROBLEM,
                    "zip_file_1_1",
                    "crime"
                ),
                tuple(
                    RECEIVED_NOT_REPORTED_PROBLEM,
                    "zip_file_2_2",
                    "bulkscan"
                ),
                tuple(
                    REPORTED_NOT_RECEIVED_PROBLEM,
                    "zip_file_A_A",
                    "sscs"
                ),
                tuple(
                    REPORTED_NOT_RECEIVED_PROBLEM,
                    "zip_file_Z_Z",
                    "pcq"
                )
            );
    }

    @Test
    void should_return_csv_file_with_only_headers_when_no_discrepancy() throws IOException {
        //when
        SummaryReport data = new SummaryReport(
            5,
            5,
            Collections.emptyList(),
            Collections.emptyList()
        );
        File summaryToCsv = reconciliationCsvWriter.writeSummaryReconciliationToCsv(data);

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
            data.get(0), data.get(1), data.get(2));
    }
}