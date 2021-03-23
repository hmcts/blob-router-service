package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.data.reports.EnvelopeSummary;
import uk.gov.hmcts.reform.blobrouter.data.reports.ReportRepository;
import uk.gov.hmcts.reform.blobrouter.model.out.reports.EnvelopeCountSummaryReportItem;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.blobrouter.util.DateTimeUtils.instant;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
public class EnvelopeSummaryRepositoryTest {
    @Autowired private EnvelopeRepository envelopeRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private DbHelper dbHelper;
    @Autowired private ServiceConfiguration serviceConfiguration;
    private static final String CONTAINER_1 = "bulkscanauto";
    private static final String CONTAINER_2 = "cmc";
    private static final String CONTAINER_3 = "sscs";
    private static final String CONTAINER_4 = "crime";
    private static final String EXCLUDED_CONTAINER = "bulkscan";
    private static final String CONTAINER_5 = "pcq";
    private static final String CONTAINER_6 = "finrem";
    private static final String CONTAINER_7 = "probate";
    private static final String FILE_1_1 = "file_name_1_1";
    private static final String FILE_1_2 = "file_name_1_2";
    private static final String FILE_2_1 = "file_name_2_1";
    private static final String FILE_2_2 = "file_name_2_2";
    private static final String FILE_3_1 = "file_name_3_1";
    private static final String FILE_3_2 = "file_name_3_2";
    private static final String FILE_3_3 = "file_name_3_3";
    private static final String FILE_4_1 = "file_name_4_1";
    private static final String FILE_4_2 = "file_name_4_2";
    private static final String FILE_4_3 = "file_name_4_3";
    private static final String FILE_BULKSCAN_1 = "file_name_bulkscan_1";
    private static final LocalDate DATE_REPORTED_FOR = LocalDate.now();

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
            new NewEnvelope(CONTAINER_1, FILE_1_1, createdAt1, dispatchedAt, Status.DISPATCHED)
        );
        // 2 envelopes are on the request date
        envelopeRepository.insert(new NewEnvelope(CONTAINER_1, FILE_1_2, createdAt2, dispatchedAt, Status.DISPATCHED));
        envelopeRepository.insert(new NewEnvelope(CONTAINER_2, FILE_2_1, createdAt3, null, Status.REJECTED));

        // after the request date
        envelopeRepository.insert(new NewEnvelope(CONTAINER_2, FILE_2_2, createdAt4, dispatchedAt, Status.REJECTED));

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
                    CONTAINER_1,
                    FILE_1_2,
                    createdAt2,
                    dispatchedAt,
                    Status.DISPATCHED,
                    false
                ),
                new EnvelopeSummary(
                    CONTAINER_2,
                    FILE_2_1,
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
        envelopeRepository.insert(new NewEnvelope(CONTAINER_2, FILE_2_1, createdAt2, null, Status.REJECTED));
        envelopeRepository.insert(new NewEnvelope(CONTAINER_1, FILE_1_2, createdAt1, dispatchedAt, Status.DISPATCHED));

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
                    CONTAINER_1,
                    FILE_1_2,
                    createdAt1,
                    dispatchedAt,
                    Status.DISPATCHED,
                    false
                ),
                new EnvelopeSummary(
                    CONTAINER_2,
                    FILE_2_1,
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

        envelopeRepository.insert(new NewEnvelope(CONTAINER_1, FILE_1_1, createdAt1, dispatchedAt, Status.DISPATCHED));

        envelopeRepository.insert(
            new NewEnvelope(EXCLUDED_CONTAINER, FILE_BULKSCAN_1, createdAt2, dispatchedAt, Status.DISPATCHED)
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
                    CONTAINER_1,
                    FILE_1_1,
                    createdAt1,
                    dispatchedAt,
                    Status.DISPATCHED,
                    false
                )
            );
    }

    @Test
     void should_return_envelopes_received_and_status_summary_by_requested_Multiple_dates_created() {
        LocalDate dateReportedFor = LocalDate.of(2021,3,19);
        Instant fileCreatedAt1 = instant("2021-03-17 12:33:27");
        Instant fileCreatedAt2 = instant("2021-03-17 12:49:27");
        Instant fileCreatedAt3 = instant("2021-03-18 12:49:27");
        Instant createdAt1 = instant("2021-03-19 12:13:47");
        Instant createdAt2 = instant("2021-03-19 12:42:28");
        Instant createdAt3 = instant("2021-03-24 10:09:11");
        Instant dispatchedAt3 = instant("2021-03-25 12:32:26");
        dbHelper.insertWithCreatedAt(new NewEnvelope(
                                                   CONTAINER_1, FILE_1_2, fileCreatedAt1,
                                                    null, Status.REJECTED),createdAt1);
        dbHelper.insertWithCreatedAt(new NewEnvelope(
                                                   CONTAINER_2, FILE_2_2, fileCreatedAt2,
                                                    null, Status.CREATED), createdAt2);
        dbHelper.insertWithCreatedAt(new NewEnvelope(
                                                   CONTAINER_1, FILE_2_1, fileCreatedAt3,
                                                    dispatchedAt3, Status.CREATED), createdAt3);
        List<EnvelopeCountSummaryReportItem> result = reportRepository.getReportFor(dateReportedFor);
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .extracting(env -> env.received)
            .containsExactlyInAnyOrder(1,1,0,0,0,0,0,0);
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .extracting(env -> env.rejected)
            .containsExactlyInAnyOrder(1,0,0,0,0,0,0,0);
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .extracting(env -> env.date)
            .containsOnly(dateReportedFor);
        assertThat(result)
             .usingFieldByFieldElementComparator()
             .extracting(env -> env.container)
             .containsExactlyInAnyOrder(CONTAINER_1, CONTAINER_2, CONTAINER_3,
                                        CONTAINER_4,CONTAINER_5,CONTAINER_6,CONTAINER_7,EXCLUDED_CONTAINER);

    }

    @Test
    void should_return_envelopes_received_and_status_summary_Only_by_requested_date_created() {
        Instant createdAtDay1 = instant("2020-05-17 11:32:26");
        Instant createdAtDay2 = instant("2020-05-17 12:33:27");
        Instant dispatchedAt4 = instant("2020-05-17 12:56:26");
        //for envelopes received at different date
        Instant createdAtDay3 = instant("2020-06-24 10:09:11");
        Instant dispatchedAt5 = instant("2020-06-25 12:32:26");
        envelopeRepository.insert(new NewEnvelope(
            CONTAINER_2, FILE_3_1, createdAtDay1, dispatchedAt4, Status.DISPATCHED)
        );
        envelopeRepository.insert(new NewEnvelope(
            CONTAINER_3, FILE_4_1, createdAtDay1, null, Status.REJECTED)
        );
        envelopeRepository.insert(new NewEnvelope(
            CONTAINER_3, FILE_3_3, createdAtDay3, null, Status.REJECTED)
        );
        envelopeRepository.insert(new NewEnvelope(
            CONTAINER_4, FILE_3_2, createdAtDay2, null, Status.REJECTED)
        );
        envelopeRepository.insert(new NewEnvelope(
            CONTAINER_4, FILE_4_2, createdAtDay2, dispatchedAt4, Status.DISPATCHED)
        );

        envelopeRepository.insert(new NewEnvelope(
            CONTAINER_4, FILE_4_3, dispatchedAt5, null, Status.REJECTED)
        );

        List<EnvelopeCountSummaryReportItem> result = reportRepository.getReportFor(DATE_REPORTED_FOR);
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .extracting(env -> env.received)
            .containsExactlyInAnyOrder(1, 2,3,0,0,0,0,0);
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .extracting(env -> env.rejected)
            .containsExactlyInAnyOrder(0,2,2,0,0,0,0,0);
        assertThat(result)
            .extracting(env -> env.container)
            .containsExactlyInAnyOrder(CONTAINER_1,CONTAINER_2, CONTAINER_3,
                                       CONTAINER_4,CONTAINER_6,CONTAINER_7,EXCLUDED_CONTAINER);
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .extracting(env -> env.date)
            .containsOnly(DATE_REPORTED_FOR);
    }

    //containers have no rejected envelopes for the date
    @Test
    void should_return_zero_rejected_envelopes_if_no_rejected_envelope_is_there() {
        Instant createdAtDay1 = instant("2021-03-17 11:32:26");

        envelopeRepository.insert(new NewEnvelope(
            CONTAINER_1, FILE_1_1, createdAtDay1, null, Status.CREATED)
        );
        envelopeRepository.insert(new NewEnvelope(
            CONTAINER_1, FILE_2_1, createdAtDay1, null, Status.DISPATCHED)
        );
        List<EnvelopeCountSummaryReportItem> result = reportRepository.getReportFor(DATE_REPORTED_FOR);
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .extracting(env -> env.received).containsExactlyInAnyOrder(0,2,0,0,0,0,0,0);
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .extracting(env -> env.rejected).containsExactlyInAnyOrder(0,0,0,0,0,0,0,0);
        assertThat(result)
            .extracting(env -> env.container).containsExactlyInAnyOrder(CONTAINER_1,CONTAINER_2,
                                                              CONTAINER_3,CONTAINER_4,
                                                              CONTAINER_5, CONTAINER_6, CONTAINER_7,EXCLUDED_CONTAINER);

    }

}
