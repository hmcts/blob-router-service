package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Envelope;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.EnvelopeRepository;
import uk.gov.hmcts.reform.blobrouter.data.envelopes.Status;
import uk.gov.hmcts.reform.blobrouter.model.out.IncompleteEnvelopeInfo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.blobrouter.data.envelopes.Status.CREATED;

@ExtendWith(MockitoExtension.class)
class IncompleteEnvelopesServiceTest {
    private IncompleteEnvelopesService incompleteEnvelopesService;

    @Mock
    private EnvelopeRepository envelopeRepository;

    @BeforeEach
    void setUp() {
        incompleteEnvelopesService = new IncompleteEnvelopesService(envelopeRepository);
    }

    @Test
    void should_propagate_result_from_repository() {
        // given
        List<Envelope> envelopes = asList(
            envelope("file1.zip", "cmc", CREATED),
            envelope("file2.zip", "sscs", CREATED)
        );
        given(envelopeRepository.getIncompleteEnvelopesBefore(any(LocalDateTime.class)))
            .willReturn(envelopes);

        // when
        List<IncompleteEnvelopeInfo> result = incompleteEnvelopesService.getIncompleteEnvelopes(2);

        // then
        assertThat(result)
            .extracting(e -> tuple(e.fileName, e.container))
            .containsExactlyInAnyOrder(
                tuple("file1.zip", "cmc"),
                tuple("file2.zip", "sscs")
            )
        ;
    }

    @Test
    void should_propagate_empty_result() {
        // given
        given(envelopeRepository.getIncompleteEnvelopesBefore(any(LocalDateTime.class)))
            .willReturn(emptyList());

        // when
        List<IncompleteEnvelopeInfo> result = incompleteEnvelopesService.getIncompleteEnvelopes(2);

        // then
        assertThat(result).isEmpty();
    }

    private Envelope envelope(
        String fileName,
        String container,
        Status status
    ) {
        return new Envelope(
            UUID.randomUUID(),
            container,
            fileName,
            now(),
            now(),
            now(),
            status,
            false,
            false,
            null
        );
    }

    @Test
    void should_delete_multiple_envelopes() {
        given(envelopeRepository.deleteEnvelopesBefore(any(), anyList())).willReturn(3);

        int rowsDeleted = incompleteEnvelopesService
            .deleteIncompleteEnvelopes(3,
                                       List.of("e0f61751-0801-4e90-999f-182a9a1a7922",
                                               "de4694ee-968f-47f0-8b5b-3c26b8dc92c7",
                                               "a0e7429f-1960-43c9-8511-241559ade310"));

        assertThat(rowsDeleted).isEqualTo(3);
    }

    @Test
    void should_delete_no_envelopes() {
        int rowsDeleted = incompleteEnvelopesService.deleteIncompleteEnvelopes(4, emptyList());

        assertThat(rowsDeleted).isEqualTo(0);
    }
}
