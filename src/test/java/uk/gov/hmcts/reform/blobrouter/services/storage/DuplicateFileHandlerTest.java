package uk.gov.hmcts.reform.blobrouter.services.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.DuplicateFinder;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.DuplicateFinder.Duplicate;

import java.util.UUID;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DuplicateFileHandlerTest {

    @Mock
    EnvelopeService envelopeService;
    @Mock
    BlobMover blobMover;
    @Mock
    DuplicateFinder duplicateFinder;
    @Mock
    ServiceConfiguration serviceConfiguration;

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
        var duplicate1 = new Duplicate("b1", "C", now());
        var duplicate2 = new Duplicate("b2", "C", now());
        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();

        given(serviceConfiguration.getEnabledSourceContainers()).willReturn(singletonList("C"));
        given(duplicateFinder.findIn("C")).willReturn(asList(duplicate1, duplicate2));
        given(envelopeService.createNewEnvelope(any(), any(), any())).willReturn(id1, id2);

        // when
        handler.handle();

        // then
        verify(envelopeService).createNewEnvelope(duplicate1.container, duplicate1.fileName, duplicate1.blobCreatedAt);
        verify(envelopeService)
            .markAsRejected(id1, ErrorCode.ERR_ZIP_PROCESSING_FAILED, DuplicateFileHandler.EVENT_MESSAGE);
        verify(blobMover).moveToRejectedContainer(duplicate1.fileName, duplicate1.container);

        verify(envelopeService).createNewEnvelope(duplicate2.container, duplicate2.fileName, duplicate2.blobCreatedAt);
        verify(envelopeService)
            .markAsRejected(id2, ErrorCode.ERR_ZIP_PROCESSING_FAILED, DuplicateFileHandler.EVENT_MESSAGE);
        verify(blobMover).moveToRejectedContainer(duplicate2.fileName, duplicate2.container);
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
