package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.Test;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class BlobReadinessCheckerTest {

    @Test
    void should_only_allow_processing_files_after_required_time_passed() {
        int delayInMinutes = 10;
        var checker = new BlobReadinessChecker(delayInMinutes);

        assertThat(checker.isReady(now().minus(0, MINUTES))).isFalse();
        assertThat(checker.isReady(now().minus(5, MINUTES))).isFalse();
        assertThat(checker.isReady(now().minus(9, MINUTES))).isFalse();
        assertThat(checker.isReady(now().minus(11, MINUTES))).isTrue();
        assertThat(checker.isReady(now().minus(30, MINUTES))).isTrue();
    }

    @Test
    void should_allow_processing_all_files_if_delay_is_set_to_zero() {
        var checker = new BlobReadinessChecker(0);

        assertThat(checker.isReady(now().minus(0, MINUTES))).isTrue();
        assertThat(checker.isReady(now().minus(5, MINUTES))).isTrue();
        assertThat(checker.isReady(now().minus(10, MINUTES))).isTrue();
    }

    @Test
    void should_require_positive_number_for_delay() {
        Throwable exc = catchThrowable(() -> new BlobReadinessChecker(-1));
        assertThat(exc)
            .isInstanceOf(IllegalArgumentException.class);
    }
}
