package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.ZipInputStream;

import static org.slf4j.LoggerFactory.getLogger;

public class BlobProcessorSubscriber implements Subscriber<Tuple2<ByteBuffer, String>> {

    private static final Logger logger = getLogger(BlobProcessorSubscriber.class);

    private final BlobAsyncClient blobClient;
    private final BlobLeaseAsyncClient blobLeaseClient;
    private final BlobDispatcher dispatcher;

    public BlobProcessorSubscriber(
        BlobAsyncClient blobClient,
        BlobLeaseAsyncClient blobLeaseClient,
        BlobDispatcher dispatcher
    ) {
        this.blobClient = blobClient;
        this.blobLeaseClient = blobLeaseClient;
        this.dispatcher = dispatcher;
    }

    @Override
    public void onSubscribe(Subscription subscription) {

    }

    // tuple of downloaded blob and lease id
    @Override
    public void onNext(Tuple2<ByteBuffer, String> item) {
        String blobName = blobClient.getBlobName();
        String containerName = blobClient.getContainerName();

        logger.info("Start processing {} blob from {} container", blobName, containerName);

        try (ZipInputStream inputStream = new ZipInputStream(new ByteArrayInputStream(item.getT1().array()))) {
            Mono.just(inputStream.readAllBytes()) // signature check placeholder
                .flatMap(rawBlob -> dispatcher.dispatch(blobName, rawBlob, containerName))
                .flatMap(blockBlobItem -> Mono.empty()) // delete blob placeholder
                .subscribe();
        } catch (IOException exception) {
            logger.error("Unable to read zip file {} from {} container", blobName, containerName, exception);
        }
    }

    // handle unexpected error. will break the processing flow
    @Override
    public void onError(Throwable throwable) {
        logger.error(
            "Error occurred while processing {} blob from {} container",
            blobClient.getBlobName(),
            blobClient.getContainerName(),
            throwable
        );
    }

    @Override
    public void onComplete() {

    }
}
