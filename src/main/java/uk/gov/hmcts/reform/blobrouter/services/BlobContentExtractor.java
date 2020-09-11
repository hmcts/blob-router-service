package uk.gov.hmcts.reform.blobrouter.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.CRIME;
import static uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount.PCQ;
import static uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers.ENVELOPE;

@Component
public class BlobContentExtractor {

    public byte[] getContentToUpload(byte[] blobContent, TargetStorageAccount targetAccount) throws IOException {
        if (targetAccount == CRIME || targetAccount == PCQ) {
            try (var zipStream = new ZipInputStream(new ByteArrayInputStream(blobContent))) {
                ZipEntry entry;

                while ((entry = zipStream.getNextEntry()) != null) {
                    if (Objects.equals(entry.getName(), ENVELOPE)) {
                        return toByteArray(zipStream);
                    }
                }

                throw new InvalidZipArchiveException(
                    String.format("ZIP file doesn't contain the required %s entry", ENVELOPE)
                );
            }
        } else {
            return blobContent;
        }
    }
}
