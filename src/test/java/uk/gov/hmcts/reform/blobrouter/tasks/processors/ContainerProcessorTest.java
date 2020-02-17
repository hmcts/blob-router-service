package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.PagedResponseBase;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.reform.blobrouter.data.EventRecordRepository;
import uk.gov.hmcts.reform.blobrouter.data.model.NewEventRecord;
import uk.gov.hmcts.reform.blobrouter.services.BlobReadinessChecker;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.blobrouter.data.model.Event.FILE_PROCESSING_STARTED;

@ExtendWith(MockitoExtension.class)
class ContainerProcessorTest {

    private static final String CONTAINER_NAME = "container";
    private static final String BLOB_NAME = "blob";

    @Mock BlobServiceClient storageClient;
    @Mock BlobContainerClient containerClient;
    @Mock BlobProcessor blobProcessor;
    @Mock BlobReadinessChecker readinessChecker;
    @Mock EventRecordRepository eventRecordRepository;

    private ContainerProcessor containerProcessor;

    @BeforeEach
    void setUp() {
        containerProcessor = new ContainerProcessor(
            storageClient,
            blobProcessor,
            readinessChecker,
            eventRecordRepository
        );
    }

    @Test
    void should_record_event_of_blob_being_processed() {
        given(storageClient.getBlobContainerClient(CONTAINER_NAME)).willReturn(containerClient);
        given(containerClient.listBlobs())
            .willReturn(new PagedIterable<>(new PagedFlux<>(() -> Mono.just(
                new PagedResponseBase<>(
                    null,
                    200,
                    null,
                    Collections.singletonList(
                        new BlobItem().setName(BLOB_NAME).setProperties(
                            new BlobItemProperties().setLastModified(OffsetDateTime.now())
                        )
                    ),
                    null,
                    null
                )
            ))));
        given(readinessChecker.isReady(any())).willReturn(true);

        containerProcessor.process(CONTAINER_NAME);

        var newEventRecordCaptor = ArgumentCaptor.forClass(NewEventRecord.class);
        verify(eventRecordRepository).insert(newEventRecordCaptor.capture());
        assertThat(newEventRecordCaptor.getValue()).satisfies(record -> {
            assertThat(record.fileName).isEqualTo(BLOB_NAME);
            assertThat(record.container).isEqualTo(CONTAINER_NAME);
            assertThat(record.event).isEqualTo(FILE_PROCESSING_STARTED);
            assertThat(record.notes).isNull();
        });
    }
}
