package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Component;

@Component
public class BlobContainerClientBuilderProvider {

    @Lookup("bulk-scan-blob-client-builder")
    public BlobContainerClientBuilder getBlobContainerClientBuilderBean() {
        return null;
    }
}
