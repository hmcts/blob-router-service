package uk.gov.hmcts.reform.blobrouter.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.data.model.EnvelopeSummary;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEnvelope;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEventRecord;
import uk.gov.hmcts.reform.blobrouter.data.model.Status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.DUPLICATE_REJECTED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.FILE_PROCESSING_STARTED;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
public class EnvelopeSummaryRepositoryTest {
    @Autowired private EnvelopeRepository envelopeRepository;
    @Autowired private EventRecordRepository eventRecordRepository;
    @Autowired private EnvelopeSummaryRepository envelopeSummaryRepository;
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
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_1, FILE_1_1, FILE_PROCESSING_STARTED));

        // 2 envelopes are on the request date
        envelopeRepository.insert(new NewEnvelope(CONTAINER_1, FILE_1_2, createdAt2, dispatchedAt, Status.DISPATCHED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_1, FILE_1_2, FILE_PROCESSING_STARTED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_1, FILE_1_2, DISPATCHED));
        Instant lastEvent12CreatedAt = getLastEventCreatedCreatedAt(CONTAINER_1, FILE_1_2);

        envelopeRepository.insert(new NewEnvelope(CONTAINER_2, FILE_2_1, createdAt3, null, Status.REJECTED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_2, FILE_2_1, FILE_PROCESSING_STARTED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_2, FILE_2_1, DUPLICATE_REJECTED, "Duplicate"));
        Instant lastEvent21CreatedAt = getLastEventCreatedCreatedAt(CONTAINER_2, FILE_2_1);

        // after the request date
        envelopeRepository.insert(new NewEnvelope(CONTAINER_2, FILE_2_2, createdAt4, dispatchedAt, Status.REJECTED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_2, FILE_2_2, FILE_PROCESSING_STARTED));

        // when
        List<EnvelopeSummary> result = envelopeSummaryRepository.find(
            LocalDate.parse("2019-12-20").atStartOfDay().toInstant(UTC),
            LocalDate.parse("2019-12-21").atStartOfDay().toInstant(UTC)
        );

        // then
        assertThat(result.stream()
                       .collect(toList()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new EnvelopeSummary(
                    CONTAINER_1,
                    FILE_1_2,
                    createdAt2,
                    dispatchedAt,
                    Status.DISPATCHED,
                    false,
                    DISPATCHED,
                    null,
                    lastEvent12CreatedAt
                ),
                new EnvelopeSummary(
                    CONTAINER_2,
                    FILE_2_1,
                    createdAt3,
                    null,
                    Status.REJECTED,
                    false,
                    DUPLICATE_REJECTED,
                    "Duplicate",
                    lastEvent21CreatedAt
                )
            );
    }

    @Test
    @SuppressWarnings("checkstyle:variabledeclarationusagedistance")
    void should_handle_missing_events() {
        // given
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Instant createdAt = LocalDateTime.parse("2019-12-20 10:31:25", formatter).toInstant(UTC);
        Instant dispatchedAt = LocalDateTime.parse("2019-12-22 13:39:33", formatter).toInstant(UTC);

        envelopeRepository.insert(new NewEnvelope(CONTAINER_1, FILE_1_1, createdAt, dispatchedAt, Status.DISPATCHED));

        // when
        List<EnvelopeSummary> result = envelopeSummaryRepository.find(
            LocalDate.parse("2019-12-20").atStartOfDay().toInstant(UTC),
            LocalDate.parse("2019-12-21").atStartOfDay().toInstant(UTC)
        );

        // then
        assertThat(result.stream()
                       .collect(toList()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new EnvelopeSummary(
                    CONTAINER_1,
                    FILE_1_1,
                    createdAt,
                    dispatchedAt,
                    Status.DISPATCHED,
                    false,
                    null,
                    null,
                    null
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
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_1, FILE_1_1, FILE_PROCESSING_STARTED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_1, FILE_1_1, DISPATCHED));
        Instant lastEventCreatedAt = getLastEventCreatedCreatedAt(CONTAINER_1, FILE_1_1);

        envelopeRepository.insert(
            new NewEnvelope(BULKSCAN_CONTAINER, FILE_BULKSCAN_1, createdAt2, dispatchedAt, Status.DISPATCHED)
        );

        // when
        List<EnvelopeSummary> result = envelopeSummaryRepository.find(
            LocalDate.parse("2019-12-20").atStartOfDay().toInstant(UTC),
            LocalDate.parse("2019-12-21").atStartOfDay().toInstant(UTC)
        );

        // then
        assertThat(result.stream()
                       .collect(toList()))
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new EnvelopeSummary(
                    CONTAINER_1,
                    FILE_1_1,
                    createdAt1,
                    dispatchedAt,
                    Status.DISPATCHED,
                    false,
                    DISPATCHED,
                    null,
                    lastEventCreatedAt
                )
            );
    }

    private Instant getLastEventCreatedCreatedAt(String container, String fileName) {
        return eventRecordRepository
            .find(container, fileName)
            .stream()
            .max(comparingLong(er -> er.id))
            .get()
            .createdAt;
    }
}
