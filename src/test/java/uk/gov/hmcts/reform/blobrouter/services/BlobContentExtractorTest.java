package uk.gov.hmcts.reform.blobrouter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

    @ParameterizedTest
    @EnumSource(
        value = TargetStorageAccount.class,
        names = {"CRIME", "PCQ"}
    )
    void should_return_internal_zip_for_crime_and_pcq_containers(
        TargetStorageAccount targetStorageAccount
    ) throws Exception {
        // given
        var content = getBlobContent(
            Map.of(
                ENVELOPE, "internal".getBytes(),
                SIGNATURE, "sig".getBytes()
            )
        );

        // when
        var result = extractor.getContentToUpload(content, targetStorageAccount);

        // then
        assertThat(result).isEqualTo("internal".getBytes());
    }

    @ParameterizedTest
    @EnumSource(
        value = TargetStorageAccount.class,
        names = {"CRIME", "PCQ"}
    )
    void should_throw_exception_if_internal_zip_cannot_be_found_for_crime_and_pcq_containers(
        TargetStorageAccount targetStorageAccount
    ) throws Exception {
        // given
        var content = getBlobContent(
            Map.of(
                "x", "foo".getBytes(),
                "b", "bar".getBytes()
            )
        );

        // when
        var exc = catchThrowable(() -> extractor.getContentToUpload(content, targetStorageAccount));

        // then
        assertThat(exc).isInstanceOf(InvalidZipArchiveException.class);
    }

    @Test
    void should_return_original_zip_for_bulkscan() throws Exception {
        // given
        var content = "irrelevant".getBytes();

        // when
        var result = extractor.getContentToUpload(content, TargetStorageAccount.CFT);

        // then
        assertThat(result).isEqualTo(content);
    }

    private static byte[] getBlobContent(Map<String, byte[]> zipEntries) throws IOException {
        try (
            var outputStream = new ByteArrayOutputStream();
            var zipOutputStream = new ZipOutputStream(outputStream)
        ) {
            for (var entry : zipEntries.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutputStream.write(entry.getValue());
                zipOutputStream.closeEntry();
            }

            return outputStream.toByteArray();
        }
    }
}
