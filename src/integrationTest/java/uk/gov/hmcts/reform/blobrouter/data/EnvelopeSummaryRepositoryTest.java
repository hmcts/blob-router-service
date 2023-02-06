package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.reports.EnvelopeSummary;
import uk.gov.hmcts.reform.blobrouter.data.reports.ReportRepository;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.blobrouter.util.DateTimeUtils.instant;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
public class EnvelopeSummaryRepositoryTest {
    @Autowired private EnvelopeRepository envelopeRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private DbHelper dbHelper;
    private static final String CONTAINER_CRIME = "crime";
    private static final String CONTAINER_SSCS = "sscs";
    private static final String CONTAINER_PCQ = "pcq";
    private static final String CONTAINER_PROBATE = "probate";
    private static final String EXCLUDED_CONTAINER = "bulkscan";
    private static final String CRIME_CREATED_1 = "crime_created_1";
    private static final String CRIME_REJECTED_2 = "crime_rejected_2";
    private static final String CRIME_DISPATCHED_3 = "crime_dispatched_3";
    private static final String SSCS_CREATED_1 = "sscs_created_1";
    private static final String SSCS_REJECTED_2 = "sscs_rejected_2";
    private static final String SSCS_DISPATCHED_3 = "sscs_dispatched_3";
    private static final String PCQ_CREATED_1 = "pcq_created_1";
    private static final String PCQ_REJECTED_2 = "pcq_rejected_2";
    private static final String PCQ_DISPATCHED_3 = "pcq_dispatched_3";
    private static final String PROBATE_CREATED_1 = "probate_created_1";
    private static final String PROBATE_REJECTED_2 = "probate_rejected_2";
    private static final String PROBATE_DISPATCHED_3 = "probate_dispatched_3";
    private static final String BULKSCAN_DISPATCHED_1 = "bulkscan_dispatched_1";
    private static final LocalDate DATE_REPORTED_FOR = LocalDate.now();
    private static final List<String> containerNames = Arrays.asList("crime", "sscs", "pcq", "probate");

    @BeforeEach
    void setUp() {
        dbHelper.deleteAll();
    }

    @Test
    @SuppressWarnings("checkstyle:variabledeclarationusagedistance")
    void should_find_within_date_range() {
        // given
        Instant createdAt1 = instant("2019-12-19 10:31:25");
        Instant createdAt2 = instant("2019-12-20 11:32:26");
        Instant createdAt3 = instant("2019-12-20 12:34:28");
        Instant createdAt4 = instant("2019-12-21 13:38:32");
        Instant dispatchedAt = instant("2019-12-22 13:39:33");

        // before the request date
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_CRIME,
                CRIME_CREATED_1,
                createdAt1,
                dispatchedAt,
                Status.DISPATCHED,
                null
            )
        );
        // 2 envelopes are on the request date
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_CRIME,
                CRIME_DISPATCHED_3,
                createdAt2,
                dispatchedAt,
                Status.DISPATCHED,
                null
            )
        );
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_SSCS,
                SSCS_REJECTED_2,
                createdAt3,
                null,
                Status.REJECTED,
                null
            )
        );

        // after the request date
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_SSCS,
                SSCS_REJECTED_2,
                createdAt4,
                dispatchedAt,
                Status.REJECTED,
                null
            )
        );

        // when
        List<EnvelopeSummary> result = reportRepository.getEnvelopeSummary(
            instant("2019-12-20 00:00:00"),
            instant("2019-12-21 00:00:00")
        );

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new EnvelopeSummary(
                    CONTAINER_CRIME,
                    CRIME_DISPATCHED_3,
                    createdAt2,
                    dispatchedAt,
                    Status.DISPATCHED,
                    false
                ),
                new EnvelopeSummary(
                    CONTAINER_SSCS,
                    SSCS_REJECTED_2,
                    createdAt3,
                    null,
                    Status.REJECTED,
                    false
                )
            );
    }

    @Test
    @SuppressWarnings("checkstyle:variabledeclarationusagedistance")
    void should_find_in_correct_order() {
        // given
        Instant createdAt1 = instant("2019-12-20 11:32:26");
        Instant createdAt2 = instant("2019-12-20 12:34:28");
        Instant dispatchedAt = instant("2019-12-22 13:39:33");

        // 2 envelopes are on the request date, wrong order
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_SSCS,
                SSCS_REJECTED_2,
                createdAt2,
                null,
                Status.REJECTED,
                null
            )
        );
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_CRIME,
                CRIME_DISPATCHED_3,
                createdAt1,
                dispatchedAt,
                Status.DISPATCHED,
                null
            )
        );

        // when
        List<EnvelopeSummary> result = reportRepository.getEnvelopeSummary(
            instant("2019-12-20 00:00:00"),
            instant("2019-12-21 00:00:00")
        );

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactly(
                new EnvelopeSummary(
                    CONTAINER_CRIME,
                    CRIME_DISPATCHED_3,
                    createdAt1,
                    dispatchedAt,
                    Status.DISPATCHED,
                    false
                ),
                new EnvelopeSummary(
                    CONTAINER_SSCS,
                    SSCS_REJECTED_2,
                    createdAt2,
                    null,
                    Status.REJECTED,
                    false
                )
            );
    }

    @Test
    @SuppressWarnings("checkstyle:variabledeclarationusagedistance")
    void should_skip_bulkscan_container() {
        // given
        Instant createdAt1 = instant("2019-12-20 11:32:26");
        Instant createdAt2 = instant("2019-12-20 12:33:27");
        Instant dispatchedAt = instant("2019-12-22 13:39:33");

        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_CRIME,
                CRIME_DISPATCHED_3,
                createdAt1,
                dispatchedAt,
                Status.DISPATCHED,
                null
            )
        );

        envelopeRepository.insert(
            new NewEnvelope(
                EXCLUDED_CONTAINER,
                BULKSCAN_DISPATCHED_1,
                createdAt2,
                dispatchedAt,
                Status.DISPATCHED,
                null
            )
        );

        // when
        List<EnvelopeSummary> result = reportRepository.getEnvelopeSummary(
            instant("2019-12-20 00:00:00"),
            instant("2019-12-21 00:00:00")
        );

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new EnvelopeSummary(
                    CONTAINER_CRIME,
                    CRIME_DISPATCHED_3,
                    createdAt1,
                    dispatchedAt,
                    Status.DISPATCHED,
                    false
                )
            );
    }

    @Test
     void should_return_envelopes_summary_received_and_rejected_multiple_dates_created() {
        LocalDate dateReportedFor = LocalDate.of(2021,3,19);
        Instant fileCreatedAt1 = instant("2021-03-17 12:33:27");
        Instant fileCreatedAt2 = instant("2021-03-17 12:49:27");
        Instant fileCreatedAt3 = instant("2021-03-18 12:49:27");
        Instant createdAt1 = instant("2021-03-19 12:13:47");
        Instant createdAt2 = instant("2021-03-19 12:42:28");
        Instant createdAt3 = instant("2021-03-24 10:09:11");
        Instant dispatchedAt3 = instant("2021-03-25 12:32:26");
        dbHelper.insertWithCreatedAt(
            new NewEnvelope(
                CONTAINER_CRIME,
                CRIME_REJECTED_2,
                fileCreatedAt1,
                null,
                Status.REJECTED,
                null
            ),
            createdAt1
        );
        dbHelper.insertWithCreatedAt(
            new NewEnvelope(
                CONTAINER_SSCS,
                SSCS_CREATED_1,
                fileCreatedAt2,
                null,
                Status.CREATED,
                null
            ),
            createdAt2
        );
        dbHelper.insertWithCreatedAt(
            new NewEnvelope(
                CONTAINER_PCQ,
                PCQ_CREATED_1,
                fileCreatedAt3,
                dispatchedAt3,
                Status.CREATED,
                null
            ),
            createdAt3
        );
        List<EnvelopeCountSummaryReportItem> result = reportRepository.getReportFor(dateReportedFor, containerNames);
        assertThat(result).usingRecursiveFieldByFieldElementComparator()
            .extracting(env -> env.received)
            .containsExactlyInAnyOrder(1,1,0,0);
        assertThat(result).usingRecursiveFieldByFieldElementComparator()
            .extracting(env -> env.rejected)
            .containsExactlyInAnyOrder(1,0,0,0);
        assertThat(result).usingRecursiveFieldByFieldElementComparator()
            .extracting(env -> env.date.format(ISO_DATE))
            .containsOnly(dateReportedFor.format(ISO_DATE));
        assertThat(result).usingRecursiveFieldByFieldElementComparator()
             .extracting(env -> env.container)
             .containsExactlyInAnyOrder(CONTAINER_CRIME, CONTAINER_SSCS, CONTAINER_PROBATE, CONTAINER_PCQ);

    }

    @Test
    void should_return_envelopes_summary_received_and_rejected_by_date() {
        Instant createdAtDay1 = instant("2020-05-17 11:32:26");
        Instant createdAtDay2 = instant("2020-05-17 12:33:27");
        Instant dispatchedAt4 = instant("2020-05-17 12:56:26");
        //for envelopes received at different date
        Instant createdAtDay3 = instant("2020-06-24 10:09:11");
        Instant dispatchedAt5 = instant("2020-06-25 12:32:26");
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_SSCS,
                SSCS_DISPATCHED_3,
                createdAtDay1,
                dispatchedAt4,
                Status.DISPATCHED,
                null
            )
        );
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_PCQ,
                PCQ_REJECTED_2,
                createdAtDay1,
                null,
                Status.REJECTED,
                null
            )
        );
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_PCQ,
                PCQ_REJECTED_2,
                createdAtDay3,
                null,
                Status.REJECTED,
                null
            )
        );
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_PROBATE,
                PROBATE_CREATED_1,
                createdAtDay2,
                null,
                Status.CREATED,
                null
            )
        );
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_PROBATE,
                PROBATE_DISPATCHED_3,
                createdAtDay2,
                dispatchedAt4,
                Status.DISPATCHED,
                null
            )
        );
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_PROBATE,
                PROBATE_REJECTED_2,
                dispatchedAt5,
                null,
                Status.REJECTED,
                null
            )
        );

        List<EnvelopeCountSummaryReportItem> result = reportRepository.getReportFor(DATE_REPORTED_FOR, containerNames);
        assertThat(result).usingRecursiveFieldByFieldElementComparator()
            .extracting(env -> env.received)
            .containsExactlyInAnyOrder(1, 2,3,0);
        assertThat(result).usingRecursiveFieldByFieldElementComparator()
            .extracting(env -> env.rejected)
            .containsExactlyInAnyOrder(0,1,2,0);
        assertThat(result)
            .extracting(env -> env.container)
            .containsExactlyInAnyOrder(CONTAINER_CRIME, CONTAINER_PCQ, CONTAINER_SSCS, CONTAINER_PROBATE);
        assertThat(result).usingRecursiveFieldByFieldElementComparator()
            .extracting(env -> env.date.format(ISO_DATE))
            .containsOnly(DATE_REPORTED_FOR.format(ISO_DATE));
    }

    //containers have no rejected envelopes for the date
    @Test
    void should_return_zero_rejected_envelopes_if_no_rejected_envelope_is_there() {
        Instant createdAtDay1 = instant("2021-03-17 11:32:26");
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_CRIME,
                CRIME_CREATED_1,
                createdAtDay1,
                null,
                Status.CREATED,
                null
            )
        );
        envelopeRepository.insert(
            new NewEnvelope(
                CONTAINER_PCQ,
                PCQ_DISPATCHED_3,
                createdAtDay1,
                null,
                Status.DISPATCHED,
                null
            )
        );
        List<EnvelopeCountSummaryReportItem> result = reportRepository.getReportFor(DATE_REPORTED_FOR, containerNames);
        assertThat(result).usingRecursiveFieldByFieldElementComparator()
            .extracting(env -> env.received).containsExactlyInAnyOrder(1, 1, 0, 0);
        assertThat(result).usingRecursiveFieldByFieldElementComparator()
            .extracting(env -> env.rejected).containsExactlyInAnyOrder(0, 0, 0, 0);
        assertThat(result)
            .extracting(env -> env.container)
            .containsExactlyInAnyOrder(
                CONTAINER_CRIME,
                CONTAINER_SSCS,
                CONTAINER_PCQ,
                CONTAINER_PROBATE
            );
    }

}
