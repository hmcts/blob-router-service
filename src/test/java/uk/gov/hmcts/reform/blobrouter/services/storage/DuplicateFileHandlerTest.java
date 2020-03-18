package uk.gov.hmcts.reform.blobrouter.services.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.DuplicateFinder;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.DuplicateFinder.Duplicate;

import java.util.List;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DuplicateFileHandlerTest {

    @Mock EnvelopeService envelopeService;
    @Mock BlobMover blobMover;
    @Mock DuplicateFinder duplicateFinder;
    @Mock ServiceConfiguration serviceConfiguration;

    DuplicateFileHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DuplicateFileHandler(envelopeService, blobMover, duplicateFinder, serviceConfiguration);
    }

    @Test
    void should_check_all_containers() {
        // given
        var containers = asList("C1", "C2", "C3");
        given(serviceConfiguration.getEnabledSourceContainers()).willReturn(containers);

        // when
        handler.handle();

        // then
        containers
            .forEach(c -> verify(duplicateFinder).findIn(c));
    }

    @Test
    void should_move_blob_to_rejected_container_and_create_a_new_envelope() {
        // given
        List<Duplicate> duplicates = asList(
            new Duplicate("b1", "C", now()),
            new Duplicate("b2", "C", now())
        );
        given(serviceConfiguration.getEnabledSourceContainers()).willReturn(singletonList("C"));
        given(duplicateFinder.findIn("C")).willReturn(duplicates);

        // when
        handler.handle();

        // then
        duplicates
            .forEach(d -> {
                verify(envelopeService).createNewEnvelope(d.container, d.fileName, d.blobCreatedAt);
                verify(blobMover).moveToRejectedContainer(d.fileName, "C");
            });
    }

    @Test
    void should_continue_after_failure() {
        // given
        given(serviceConfiguration.getEnabledSourceContainers()).willReturn(singletonList("C"));
        var duplicate1 = new Duplicate("b1", "C", now());
        var duplicate2 = new Duplicate("b2", "C", now());
        given(duplicateFinder.findIn("C"))
            .willReturn(asList(
                duplicate1,
                duplicate2
            ));

        doThrow(RuntimeException.class)
            .when(blobMover)
            .moveToRejectedContainer(duplicate1.fileName, "C"); // fail on first file

        // when
        handler.handle();

        // then
        verify(blobMover).moveToRejectedContainer(duplicate2.fileName, "C");
    }
}
