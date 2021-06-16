package uk.gov.hmcts.reform.blobrouter.data.reports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.blobrouter.config.TestClockProvider;
import uk.gov.hmcts.reform.blobrouter.data.DbHelper;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.NewEnvelope;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.DISPATCHED;

@ActiveProfiles({"integration-test", "db-test"})
@SpringBootTest
@Import(TestClockProvider.class)
class StatisticsRepositoryTest {

    @Autowired private StatisticsRepository statisticsRepo;
    @Autowired private EnvelopeRepository envelopeRepo;
    @Autowired private DbHelper dbHelper;

    @AfterEach
    void tearDown() {
        dbHelper.deleteAll();
    }

    @Test
    void getEnvelopesDayStatistics() {
        // given
        insertEnvelope("c1", "hello1.zip", LocalDateTime.of(2021, 1, 31, 12, 10));
        insertEnvelope("c1", "hello2.zip", LocalDateTime.of(2021, 2, 10, 12, 10));
        insertEnvelope("c2", "hello3.zip", LocalDateTime.of(2021, 2, 11, 12, 10));
        insertEnvelope("c2", "hello4.zip", LocalDateTime.of(2021, 2, 11, 12, 11));
        insertEnvelope("c2", "hello5.zip", LocalDateTime.of(2021, 3, 1, 12, 10));

        // when
        List<DayCount> res = statisticsRepo.getEnvelopesDayStatistics(
                LocalDate.of(2021, 2, 1),
                LocalDate.of(2021, 3, 1));

        // then
        assertThat(res)
                .extracting(e -> tuple(e.date, e.count))
                .containsExactly(
                        tuple(LocalDate.of(2021, 2, 10), 1),
                        tuple(LocalDate.of(2021, 2, 11), 2)
            );
    }

    private void insertEnvelope(String container, String fileName, LocalDateTime createdAt) {
        var newEnvelope2 = new NewEnvelope(
                container,
                fileName,
                createdAt.toInstant(UTC),
                now(),
                DISPATCHED
        );
        envelopeRepo.insert(newEnvelope2);
    }
}