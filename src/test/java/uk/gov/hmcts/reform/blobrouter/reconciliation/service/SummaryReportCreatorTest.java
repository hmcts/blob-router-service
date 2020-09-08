package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReportItem;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class SummaryReportCreatorTest {

    private SummaryReportCreator summaryReportCreator = new SummaryReportCreator();

    @ParameterizedTest
    @MethodSource("serviceWithClassification")
    void createSummaryReport(
        List<SummaryReportItem> envelopeList,
        List<SummaryReportItem> reconciliationEnvelopeList,
        SummaryReport expectedSummaryReport
    ) {
        //given with params

        // when
        SummaryReport summaryReport = summaryReportCreator
            .createSummaryReport(envelopeList, reconciliationEnvelopeList);

        // then
        assertThat(summaryReport)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expectedSummaryReport);
    }

    private static Object[][] serviceWithClassification() {
        return new Object[][]{
            new Object[]{
                // tests supplier reported more than we received
                Arrays.asList(
                    createSummaryReportItem("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createSummaryReportItem("9810404021234_14-08-2020-03-08-31.zip", "sscs")
                ),
                Arrays.asList(
                    createSummaryReportItem("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createSummaryReportItem("9810404021234_14-08-2020-03-08-31.zip", "sscs"),
                    createSummaryReportItem("9810404021234_14-08-2020-03-08-31.zip", "cmc")
                ),
                new SummaryReport(
                    2,
                    3,
                    emptyList(),
                    List.of(new SummaryReportItem("9810404021234_14-08-2020-03-08-31.zip", "cmc"))
                )
            },
            new Object[]{
                // no discrepancy, reported and received matches
                Arrays.asList(
                    createSummaryReportItem("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createSummaryReportItem("9810404021234_14-08-2020-03-08-31.zip", "sscs"),
                    createSummaryReportItem("10929292923_14-05-2020-09-08-31.zip", "crime"),
                    createSummaryReportItem("1231122-05-2020-10-08-31.zip", "pcq")
                ),
                Arrays.asList(
                    createSummaryReportItem("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createSummaryReportItem("9810404021234_14-08-2020-03-08-31.zip", "sscs"),
                    createSummaryReportItem("10929292923_14-05-2020-09-08-31.zip", "crime"),
                    createSummaryReportItem("1231122-05-2020-10-08-31.zip", "pcq")
                ),
                new SummaryReport(4, 4, emptyList(), emptyList())
            },
            new Object[]{
                // supplier reported less than we received
                Arrays.asList(
                    createSummaryReportItem("7171711717_8-05-2020-09-08-31.zip", "pcq"),
                    createSummaryReportItem("10929292923_14-05-2020-09-08-31.zip", "pcq"),
                    createSummaryReportItem("1231122-05-2020-10-08-31.zip", "pcq")
                ),
                Arrays.asList(
                    createSummaryReportItem("1231122-05-2020-10-08-31.zip", "pcq")
                ),
                new SummaryReport(
                    3,
                    1,
                    List.of(
                        new SummaryReportItem("7171711717_8-05-2020-09-08-31.zip", "pcq"),
                        new SummaryReportItem("10929292923_14-05-2020-09-08-31.zip", "pcq")
                    ),
                    emptyList()
                )
            },
            new Object[]{
                // both side has missing files
                Arrays.asList(
                    createSummaryReportItem("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createSummaryReportItem("12312.31312.312.zip", "sscs")
                ),
                Arrays.asList(
                    createSummaryReportItem("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createSummaryReportItem("9810404021234_14-08-2020-03-08-31.zip", "cmc")
                ),
                new SummaryReport(
                    2,
                    2,
                    List.of(new SummaryReportItem("12312.31312.312.zip", "sscs")),
                    List.of(new SummaryReportItem("9810404021234_14-08-2020-03-08-31.zip", "cmc"))
                )
            },
            new Object[]{
                // both side has missing files
                Arrays.asList(
                    createSummaryReportItem("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createSummaryReportItem("12312.31312.312.zip", "sscs")
                ),
                Arrays.asList(
                    createSummaryReportItem("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createSummaryReportItem("9810404021234_14-08-2020-03-08-31.zip", "cmc")
                ),
                new SummaryReport(
                    2,
                    2,
                    List.of(new SummaryReportItem("12312.31312.312.zip", "sscs")),
                    List.of(new SummaryReportItem("9810404021234_14-08-2020-03-08-31.zip", "cmc")
                    )
                )
            },
            new Object[]{
                // supplier does not have any reported value
                Arrays.asList(
                    createSummaryReportItem("12312.31312.312.zip", "crime")
                ),
                null,
                new SummaryReport(
                    1,
                    0,
                    List.of(new SummaryReportItem("12312.31312.312.zip", "crime")),
                    emptyList())
            },
            new Object[]{
                // supplier report but we do not have files.
                null,
                Arrays.asList(
                    createSummaryReportItem("999-08-2020-08-31.zip", "probate"),
                    createSummaryReportItem("1921-08-2020-03-08.zip", "cmc")
                ),
                new SummaryReport(
                    0,
                    2,
                    emptyList(),
                    List.of(
                        new SummaryReportItem("999-08-2020-08-31.zip", "probate"),
                        new SummaryReportItem("1921-08-2020-03-08.zip", "cmc"))
                )
            },
            new Object[]{
                // both side does not have files.
                null,
                null,
                new SummaryReport(0, 0, emptyList(), emptyList())
            }
        };
    }

    private static SummaryReportItem createSummaryReportItem(String fileName, String container) {
        return new SummaryReportItem(fileName, container);
    }

}