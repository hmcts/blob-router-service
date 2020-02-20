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
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.DELETED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.DELETED_FROM_REJECTED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.DISPATCHED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.DUPLICATE_REJECTED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.FILE_PROCESSING_STARTED;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.REJECTED;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
public class EnvelopeSummaryRepositoryTest {
    @Autowired private EnvelopeRepository envelopeRepository;
    @Autowired private EventRecordRepository eventRecordRepository;
    @Autowired private EnvelopeSummaryRepository repo;
    @Autowired private DbHelper dbHelper;

    private static final String CONTAINER_1 = "container1";
    private static final String CONTAINER_2 = "container2";
    private static final String FILE_NAME_1_1 = "file_name_1_1";
    private static final String FILE_NAME_1_2 = "file_name_1_2";
    private static final String FILE_NAME_1_3 = "file_name_1_3";
    private static final String FILE_NAME_2_1 = "file_name_2_1";
    private static final String FILE_NAME_2_2 = "file_name_2_2";
    private static final String FILE_NAME_2_3 = "file_name_2_3";
    private static final String FILE_NAME_2_4 = "file_name_2_4";

    @BeforeEach
    void setUp() {
        dbHelper.deleteAll();
    }

    @Test
    void should_find_within_date_range() {
        // given
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Instant instantCreated1 = LocalDateTime.parse("2019-12-19 10:31:25", formatter).toInstant(UTC);
        Instant instantCreated2 = LocalDateTime.parse("2019-12-20 11:32:26", formatter).toInstant(UTC);
        Instant instantCreated3 = LocalDateTime.parse("2019-12-20 12:33:27", formatter).toInstant(UTC);
        Instant instantCreated4 = LocalDateTime.parse("2019-12-20 12:34:28", formatter).toInstant(UTC);
        Instant instantCreated5 = LocalDateTime.parse("2019-12-20 12:35:29", formatter).toInstant(UTC);
        Instant instantCreated6 = LocalDateTime.parse("2019-12-20 12:36:30", formatter).toInstant(UTC);
        Instant instantCreated7 = LocalDateTime.parse("2019-12-21 13:37:31", formatter).toInstant(UTC);
        Instant instantDispatched = LocalDateTime.parse("2019-12-22 13:37:31", formatter).toInstant(UTC);

        // before the request date
        envelopeRepository.insert(new NewEnvelope(CONTAINER_1, FILE_NAME_1_1, instantCreated1, instantDispatched, Status.DISPATCHED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_1, FILE_NAME_1_1, FILE_PROCESSING_STARTED));

        // 4 envelopes are on the request date
        envelopeRepository.insert(new NewEnvelope(CONTAINER_1, FILE_NAME_1_2, instantCreated2, instantDispatched, Status.DISPATCHED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_1, FILE_NAME_1_2, FILE_PROCESSING_STARTED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_1, FILE_NAME_1_2, DISPATCHED));

        envelopeRepository.insert(new NewEnvelope(CONTAINER_1, FILE_NAME_1_3, instantCreated3, instantDispatched, Status.DISPATCHED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_1, FILE_NAME_1_3, FILE_PROCESSING_STARTED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_1, FILE_NAME_1_3, DISPATCHED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_1, FILE_NAME_1_3, DELETED));

        envelopeRepository.insert(new NewEnvelope(CONTAINER_2, FILE_NAME_2_1, instantCreated4, instantDispatched, Status.REJECTED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_2, FILE_NAME_2_1, FILE_PROCESSING_STARTED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_2, FILE_NAME_2_1, DUPLICATE_REJECTED, "Duplicate"));

        envelopeRepository.insert(new NewEnvelope(CONTAINER_2, FILE_NAME_2_2, instantCreated5, instantDispatched, Status.REJECTED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_2, FILE_NAME_2_2, FILE_PROCESSING_STARTED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_2, FILE_NAME_2_2, REJECTED, "Invalid signature"));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_2, FILE_NAME_2_2, DELETED_FROM_REJECTED));

        // an envelope without events
        envelopeRepository.insert(new NewEnvelope(CONTAINER_2, FILE_NAME_2_3, instantCreated6, instantDispatched, Status.DISPATCHED));

        // after the request date
        envelopeRepository.insert(new NewEnvelope(CONTAINER_2, FILE_NAME_2_4, instantCreated7, instantDispatched, Status.REJECTED));
        eventRecordRepository.insert(new NewEventRecord(CONTAINER_2, FILE_NAME_2_4, FILE_PROCESSING_STARTED));

        // when
        List<EnvelopeSummary> result = repo.find(
            LocalDate.parse("2019-12-20").atStartOfDay().toInstant(UTC),
            LocalDate.parse("2019-12-21").atStartOfDay().toInstant(UTC)
        );

        // then
        assertThat(result.stream()
                       .collect(toList()))
            .extracting("container", "fileName", "fileCreatedAt", "dispatchedAt", "status", "lastEvent", "eventNotes")
            .containsExactlyInAnyOrder(
                tuple(CONTAINER_1, FILE_NAME_1_2, instantCreated2, instantDispatched, Status.DISPATCHED, DISPATCHED, null),
                tuple(CONTAINER_1, FILE_NAME_1_3, instantCreated3, instantDispatched, Status.DISPATCHED, DELETED, null),
                tuple(CONTAINER_2, FILE_NAME_2_1, instantCreated4, instantDispatched, Status.REJECTED, DUPLICATE_REJECTED, "Duplicate"),
                tuple(CONTAINER_2, FILE_NAME_2_2, instantCreated5, instantDispatched, Status.REJECTED, DELETED_FROM_REJECTED, null),
                tuple(CONTAINER_2, FILE_NAME_2_3, instantCreated6, instantDispatched, Status.DISPATCHED, null, null)
            );
    }
}
