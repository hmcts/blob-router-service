package uk.gov.hmcts.reform.blobrouter.tasks.processors;

import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.specialized.BlobLeaseAsyncClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobDispatcher;
import uk.gov.hmcts.reform.blobrouter.services.storage.BlobProcessorSubscriber;

public class BlobProcessor {

    private final BlobServiceAsyncClient storageClient;
    private final BlobDispatcher dispatcher;

    public BlobProcessor(
        BlobServiceAsyncClient storageClient,
        BlobDispatcher dispatcher
    ) {
        this.storageClient = storageClient;
        this.dispatcher = dispatcher;
    }

    public void process(BlobItem blob, String containerName) {
        String blobName = blob.getName();
        BlobAsyncClient blobClient = storageClient
            .getBlobContainerAsyncClient(containerName)
            .getBlobAsyncClient(blobName);
        BlobLeaseAsyncClient blobLeaseClient = new BlobLeaseClientBuilder()
            .blobAsyncClient(blobClient)
            .buildAsyncClient();

        Mono<String> lease = blobLeaseClient.acquireLease(60); // parameter: seconds between 15 and 60

        blobClient
            .download()
            .zipWith(lease)
            .subscribe(new BlobProcessorSubscriber(
                blobClient,
                blobLeaseClient,
                dispatcher
            ));
    }
}
