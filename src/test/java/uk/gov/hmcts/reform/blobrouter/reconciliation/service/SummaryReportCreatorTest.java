package uk.gov.hmcts.reform.blobrouter.reconciliation.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReport;
import uk.gov.hmcts.reform.blobrouter.reconciliation.report.SummaryReportItem;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class SummaryReportCreatorTest {

    private SummaryReportCreator summaryReportCreator = new SummaryReportCreator();

    @ParameterizedTest
    @MethodSource("serviceWithClassification")
    void createSummaryReport(
        List<Envelope> envelopeList,
        List<uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.Envelope> reconciliationEnvelopeList,
        SummaryReport expectedSummaryReport
    ) {
        //given with params

        // when
        SummaryReport summaryReport = summaryReportCreator
            .createSummaryReport(envelopeList, reconciliationEnvelopeList);

        // then
        assertThat(summaryReport).usingRecursiveComparison().isEqualTo(expectedSummaryReport);
    }

    private static Object[][] serviceWithClassification() {
        return new Object[][]{
            new Object[]{
                // tests supplier reported more than we received
                Arrays.asList(
                    createEnvelope("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createEnvelope("9810404021234_14-08-2020-03-08-31.zip", "sscs")
                ),
                Arrays.asList(
                    createReconciliationEnvelope("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createReconciliationEnvelope("9810404021234_14-08-2020-03-08-31.zip", "sscs"),
                    createReconciliationEnvelope("9810404021234_14-08-2020-03-08-31.zip", "cmc")
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
                    createEnvelope("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createEnvelope("9810404021234_14-08-2020-03-08-31.zip", "sscs"),
                    createEnvelope("10929292923_14-05-2020-09-08-31.zip", "crime"),
                    createEnvelope("1231122-05-2020-10-08-31.zip", "pcq")
                ),
                Arrays.asList(
                    createReconciliationEnvelope("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createReconciliationEnvelope("9810404021234_14-08-2020-03-08-31.zip", "sscs"),
                    createReconciliationEnvelope("10929292923_14-05-2020-09-08-31.zip", "crime"),
                    createReconciliationEnvelope("1231122-05-2020-10-08-31.zip", "pcq")
                ),
                new SummaryReport(4, 4, emptyList(), emptyList())
            },
            new Object[]{
                // supplier reported less than we received
                Arrays.asList(
                    createEnvelope("7171711717_8-05-2020-09-08-31.zip", "pcq"),
                    createEnvelope("10929292923_14-05-2020-09-08-31.zip", "pcq"),
                    createEnvelope("1231122-05-2020-10-08-31.zip", "pcq")
                ),
                Arrays.asList(
                    createReconciliationEnvelope("1231122-05-2020-10-08-31.zip", "pcq")
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
                    createEnvelope("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createEnvelope("12312.31312.312.zip", "sscs")
                ),
                Arrays.asList(
                    createReconciliationEnvelope("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createReconciliationEnvelope("9810404021234_14-08-2020-03-08-31.zip", "cmc")
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
                    createEnvelope("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createEnvelope("12312.31312.312.zip", "sscs")
                ),
                Arrays.asList(
                    createReconciliationEnvelope("1010404021234_14-08-2020-08-31.zip", "probate"),
                    createReconciliationEnvelope("9810404021234_14-08-2020-03-08-31.zip", "cmc")
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
                    createEnvelope("12312.31312.312.zip", "crime")
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
                    createReconciliationEnvelope("999-08-2020-08-31.zip", "probate"),
                    createReconciliationEnvelope("1921-08-2020-03-08.zip", "cmc")
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


    private static uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.Envelope createReconciliationEnvelope(
        String fileName,
        String container
    ) {
        return new uk.gov.hmcts.reform.blobrouter.reconciliation.model.in.Envelope(
            fileName,
            null,
            container,
            container.toUpperCase(),
            getRandomDcn(),
            getRandomDcn()
        );
    }

    private static List getRandomDcn() {
        return List.of(generateRandomDigits(9), generateRandomDigits(9), generateRandomDigits(9));
    }

    public static String generateRandomDigits(int n) {
        int m = (int) Math.pow(10, n - 1);
        return Integer.toString(m + new Random().nextInt(9 * m));
    }

    private static Envelope createEnvelope(String fileName, String container) {
        return new Envelope(
            UUID.randomUUID(),
            container,
            fileName,
            Instant.now(),
            Instant.now(),
            Instant.now(),
            Status.CREATED,
            false,
            false
        );
    }

}