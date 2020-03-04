package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.config.ServiceConfiguration;
import uk.gov.hmcts.reform.blobrouter.data.model.EventType;
import uk.gov.hmcts.reform.blobrouter.services.EnvelopeService;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.DuplicateFinder;

import java.util.List;

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
    void should_move_blob_and_store_an_event() {
        // given
        List<BlobItem> blobs = asList(blob("b1"), blob("b2"));
        given(serviceConfiguration.getEnabledSourceContainers()).willReturn(singletonList("C"));
        given(duplicateFinder.findIn("C")).willReturn(blobs);

        // when
        handler.handle();

        // then
        blobs
            .forEach(blob -> {
                verify(blobMover).moveToRejectedContainer(blob.getName(), "C");
                verify(envelopeService).saveEvent("C", blob.getName(), EventType.DUPLICATE_REJECTED);
            });
    }

    @Test
    void should_continue_after_failure() {
        // given
        given(serviceConfiguration.getEnabledSourceContainers()).willReturn(singletonList("C"));
        given(duplicateFinder.findIn("C")).willReturn(asList(blob("b1"), blob("b2")));

        doThrow(RuntimeException.class)
            .when(blobMover)
            .moveToRejectedContainer("b1", "C"); // fail on first file

        // when
        handler.handle();

        // then
        verify(blobMover).moveToRejectedContainer("b2", "C");
        verify(envelopeService).saveEvent("C", "b2", EventType.DUPLICATE_REJECTED);
    }

    private BlobItem blob(String name) {
        var blob = new BlobItem();
        blob.setName(name);
        return blob;
    }
}
