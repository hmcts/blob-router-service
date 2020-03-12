package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.blobrouter.config.TargetStorageAccount;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers.ENVELOPE;
import static uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers.SIGNATURE;

class BlobContentExtractorTest {

    BlobContentExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new BlobContentExtractor();
    }

    @Test
    void should_return_internal_zip_for_crime() throws Exception {
        // given
        var content = getBlobContent(
            Map.of(
                ENVELOPE, "internal".getBytes(),
                SIGNATURE, "sig".getBytes()
            )
        );

        // when
        var result = extractor.getContentToUpload(content, TargetStorageAccount.CRIME);

        // then
        assertThat(result).isEqualTo("internal".getBytes());
    }

    @Test
    void should_throw_exception_if_internal_zip_for_crime_cannot_be_found() throws Exception {
        // given
        var content = getBlobContent(
            Map.of(
                "x", "foo".getBytes(),
                "b", "bar".getBytes()
            )
        );

        // when
        var exc = catchThrowable(() -> extractor.getContentToUpload(content, TargetStorageAccount.CRIME));

        // then
        assertThat(exc).isInstanceOf(InvalidZipArchiveException.class);
    }

    @Test
    void should_return_original_zip_for_bulkscan() throws Exception {
        // given
        var content = "irrelevant".getBytes();

        // when
        var result = extractor.getContentToUpload(content, TargetStorageAccount.BULKSCAN);

        // then
        assertThat(result).isEqualTo(content);
    }

    private static byte[] getBlobContent(Map<String, byte[]> zipEntries) {
        try (
            var outputStream = new ByteArrayOutputStream();
            var zipOutputStream = new ZipOutputStream(outputStream)
        ) {
            for (var entry : zipEntries.entrySet()) {
                var fileName = entry.getKey();
                var fileBytes = entry.getValue();

                zipOutputStream.putNextEntry(new ZipEntry(fileName));
                zipOutputStream.write(fileBytes);
                zipOutputStream.closeEntry();
            }

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test blob content", e);
        }
    }
}
