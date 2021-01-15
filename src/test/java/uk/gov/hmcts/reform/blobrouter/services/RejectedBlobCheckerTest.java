package uk.gov.hmcts.reform.blobrouter.services;

import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;

import static java.time.OffsetDateTime.now;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RejectedBlobCheckerTest {

    @Test
    void should_throw_exception_if_ttl_is_null() {
        assertThat(catchThrowable(() -> new RejectedBlobChecker(null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_throw_exception_if_ttl_is_negative() {
        assertThat(catchThrowable(() -> new RejectedBlobChecker(Duration.ofMinutes(-5))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_return_true_only_for_files_that_are_old_enough() {
        var checker = new RejectedBlobChecker(Duration.ofHours(72));

        asList(
            Pair.of(90, true),
            Pair.of(73, true),
            Pair.of(70, false),
            Pair.of(1, false)
        ).forEach(pair -> {
            // given
            var blob = blobFrom(now().minusHours(pair.getLeft()));

            // when
            var result = checker.shouldBeDeleted(blob);

            // then
            assertThat(result).isEqualTo(pair.getRight());
        });
    }

    private BlobItem blobFrom(OffsetDateTime dateTime) {
        var blobItem = mock(BlobItem.class);
        var properties = mock(BlobItemProperties.class);

        given(blobItem.getProperties()).willReturn(properties);
        given(properties.getCreationTime()).willReturn(dateTime);

        return blobItem;
    }
}
