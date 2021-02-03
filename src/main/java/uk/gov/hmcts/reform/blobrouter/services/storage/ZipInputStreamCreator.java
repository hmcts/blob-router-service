package uk.gov.hmcts.reform.blobrouter.services.storage;

import com.azure.storage.blob.specialized.BlockBlobClient;
import org.springframework.stereotype.Component;

import java.util.zip.ZipInputStream;

@Component
public class ZipInputStreamCreator {

    public ZipInputStream getZipInputStream(BlockBlobClient sourceBlob) {
        return new ZipInputStream(sourceBlob.openInputStream());
    }

}
