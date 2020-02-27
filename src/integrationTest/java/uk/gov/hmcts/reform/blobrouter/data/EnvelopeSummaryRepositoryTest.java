package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.model.EnvelopeSummary;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
public class EnvelopeSummaryRepositoryTest {
    @Autowired private EnvelopeRepository envelopeRepository;
    @Autowired private ReportRepository reportRepository;
    @Autowired private DbHelper dbHelper;

    private static final String CONTAINER_1 = "container1";
    private static final String CONTAINER_2 = "container2";
    private static final String BULKSCAN_CONTAINER = "bulkscan";
    private static final String FILE_1_1 = "file_name_1_1";
    private static final String FILE_1_2 = "file_name_1_2";
    private static final String FILE_2_1 = "file_name_2_1";
    private static final String FILE_2_2 = "file_name_2_2";
    private static final String FILE_BULKSCAN_1 = "file_name_bulkscan_1";

    @BeforeEach
    void setUp() {
        dbHelper.deleteAll();
    }

    @Test
    @SuppressWarnings("checkstyle:variabledeclarationusagedistance")
    void should_find_within_date_range() {
        // given
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Instant createdAt1 = LocalDateTime.parse("2019-12-19 10:31:25", formatter).toInstant(UTC);
        Instant createdAt2 = LocalDateTime.parse("2019-12-20 11:32:26", formatter).toInstant(UTC);
        Instant createdAt3 = LocalDateTime.parse("2019-12-20 12:34:28", formatter).toInstant(UTC);
        Instant createdAt4 = LocalDateTime.parse("2019-12-21 13:38:32", formatter).toInstant(UTC);
        Instant dispatchedAt = LocalDateTime.parse("2019-12-22 13:39:33", formatter).toInstant(UTC);

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
            LocalDate.parse("2019-12-20").atStartOfDay().toInstant(UTC),
            LocalDate.parse("2019-12-21").atStartOfDay().toInstant(UTC)
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Instant createdAt1 = LocalDateTime.parse("2019-12-20 11:32:26", formatter).toInstant(UTC);
        Instant createdAt2 = LocalDateTime.parse("2019-12-20 12:34:28", formatter).toInstant(UTC);
        Instant dispatchedAt = LocalDateTime.parse("2019-12-22 13:39:33", formatter).toInstant(UTC);

        // 2 envelopes are on the request date, wrong order
        envelopeRepository.insert(new NewEnvelope(CONTAINER_2, FILE_2_1, createdAt2, null, Status.REJECTED));
        envelopeRepository.insert(new NewEnvelope(CONTAINER_1, FILE_1_2, createdAt1, dispatchedAt, Status.DISPATCHED));

        // when
        List<EnvelopeSummary> result = reportRepository.getEnvelopeSummary(
            LocalDate.parse("2019-12-20").atStartOfDay().toInstant(UTC),
            LocalDate.parse("2019-12-21").atStartOfDay().toInstant(UTC)
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Instant createdAt1 = LocalDateTime.parse("2019-12-20 11:32:26", formatter).toInstant(UTC);
        Instant createdAt2 = LocalDateTime.parse("2019-12-20 12:33:27", formatter).toInstant(UTC);
        Instant dispatchedAt = LocalDateTime.parse("2019-12-22 13:39:33", formatter).toInstant(UTC);

        envelopeRepository.insert(new NewEnvelope(CONTAINER_1, FILE_1_1, createdAt1, dispatchedAt, Status.DISPATCHED));

        envelopeRepository.insert(
            new NewEnvelope(BULKSCAN_CONTAINER, FILE_BULKSCAN_1, createdAt2, dispatchedAt, Status.DISPATCHED)
        );

        // when
        List<EnvelopeSummary> result = reportRepository.getEnvelopeSummary(
            LocalDate.parse("2019-12-20").atStartOfDay().toInstant(UTC),
            LocalDate.parse("2019-12-21").atStartOfDay().toInstant(UTC)
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
}
