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

/**
 * The `BlobContentExtractor` class in Java provides a method to extract specific content from a ZIP file based on the
 * target storage account, throwing an exception if the required entry is not found.
 */
@Component
public class BlobContentExtractor {

    /**
     * The function `getContentToUpload` processes a byte array of blob content based on the target storage account,
     * extracting a specific entry from a ZIP file if the target account is CRIME or PCQ.
     *
     * @param blobContent The `blobContent` parameter is a byte array representing the content of a
     *                    file that needs to be uploaded to a target storage account.
     * @param targetAccount `targetAccount` is an enum representing the target storage account where the
     *                      content will be uploaded. The possible values for `targetAccount` are
     *                      `CRIME` and `PCQ`.
     * @return If the `targetAccount` is either `CRIME` or `PCQ`, the method will return the content of
     *      the ZIP entry with the name `ENVELOPE` from the provided `blobContent`. If the required
     *      entry is not found in the ZIP file, an `InvalidZipArchiveException` will be thrown.
     *      If the `targetAccount` is not `CRIME` or `PC` it returns the content provided.
     */
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
