package uk.gov.hmcts.reform.blobrouter.util;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;
import uk.gov.hmcts.reform.blobrouter.exceptions.SignatureValidationException;
import uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Set;
import java.util.zip.ZipInputStream;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.blobrouter.testutils.DirectoryZipper.zipAndSignDir;
import static uk.gov.hmcts.reform.blobrouter.testutils.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.blobrouter.testutils.SigningHelper.signWithSha256Rsa;

@ExtendWith(MockitoExtension.class)
class ZipVerifiersTest {

    private static final String INVALID_SIGNATURE_MESSAGE = "Zip signature failed verification";
    private static final String INVALID_ZIP_ENTRIES_MESSAGE = "Zip entries do not match expected file names";

    private static String publicKeyBase64;
    private static String invalidPublicKeyBase64;

    @BeforeAll
    static void setUp() throws IOException {
        publicKeyBase64 =
            Base64.getEncoder().encodeToString(
                toByteArray(getResource("signature/test_public_key.der"))
            );

        invalidPublicKeyBase64 =
            Base64.getEncoder().encodeToString(
                toByteArray(getResource("signature/invalid_test_public_key.der"))
            );
    }

    @Test
    void should_verify_signed_file_successfully() throws Exception {
        byte[] test1PdfBytes = toByteArray(getResource("test.pdf"));
        byte[] test1SigPdfBytes = toByteArray(getResource("signature/test.pdf.sig"));

        assertThatCode(() ->
            ZipVerifiers.verifySignature(publicKeyBase64, test1PdfBytes, test1SigPdfBytes)
        ).doesNotThrowAnyException();
    }

    @Test
    void should_throw_exception_for_invalid_public_key() throws Exception {
        byte[] test1PdfBytes = toByteArray(getResource("test.pdf"));
        byte[] test1SigPdfBytes = toByteArray(getResource("signature/test.pdf.sig"));

        String invalidPublicKey = Base64.getEncoder().encodeToString(
            toByteArray(getResource("signature/invalid_public_key_format.der"))
        );

        assertThatThrownBy(() ->
            ZipVerifiers.verifySignature(invalidPublicKey, test1PdfBytes, test1SigPdfBytes)
        ).isInstanceOf(SignatureValidationException.class);
    }

    @Test
    void should_not_verify_other_file_successfully() throws Exception {
        byte[] test2PdfBytes = toByteArray(getResource("test1.pdf"));
        byte[] test1SigPdfBytes = toByteArray(getResource("signature/test.pdf.sig"));
        assertThatThrownBy(() ->
            ZipVerifiers.verifySignature(publicKeyBase64, test2PdfBytes, test1SigPdfBytes)
        )
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessage("Zip signature failed verification");
    }

    @Test
    void should_verify_2_valid_filenames_successfully() {
        Set<String> files = Set.of(
            ZipVerifiers.ENVELOPE,
            ZipVerifiers.SIGNATURE
        );

        assertThatCode(() -> ZipVerifiers.verifyFileNames(files)).doesNotThrowAnyException();
    }

    @Test
    void should_not_verify_more_than_2_files_successfully() {
        Set<String> files = Set.of(
            ZipVerifiers.ENVELOPE,
            ZipVerifiers.SIGNATURE,
            "signature2"
        );

        assertThatThrownBy(() -> ZipVerifiers.verifyFileNames(files))
            .isInstanceOf(InvalidZipArchiveException.class)
            .hasMessageContaining(INVALID_ZIP_ENTRIES_MESSAGE);
    }

    @Test
    void should_not_verify_invalid_filenames_successfully() {
        Set<String> files = Set.of(
            ZipVerifiers.ENVELOPE,
            "signature.sig"
        );

        assertThatThrownBy(() -> ZipVerifiers.verifyFileNames(files))
            .isInstanceOf(InvalidZipArchiveException.class)
            .hasMessageContaining(INVALID_ZIP_ENTRIES_MESSAGE);
    }

    @Test
    void should_verify_valid_zip_successfully() throws Exception {
        byte[] zipBytes = zipAndSignDir("signature/sample_valid_content", "signature/test_private_key.der");
        var zis = ZipVerifiers.verifyZip(new ZipInputStream(new ByteArrayInputStream(zipBytes)), publicKeyBase64);
        assertThat(zis).isNotNull();
    }

    @Test
    void should_not_verify_invalid_zip_successfully() throws Exception {
        byte[] zipBytes = zipAndSignDir("signature/sample_valid_content", "signature/some_other_private_key.der");
        assertThrows(
            DocSignatureFailureException.class,
            () -> ZipVerifiers.verifyZip(new ZipInputStream(new ByteArrayInputStream(zipBytes)), publicKeyBase64)
        );
    }

    @Test
    void should_verify_valid_test_zip_successfully() throws Exception {
        byte[] zipBytes = zipDir("signature/sample_valid_content");
        byte[] signature = signWithSha256Rsa(
            zipBytes,
            toByteArray(getResource("signature/test_private_key.der"))
        );

        assertThatCode(() ->
            ZipVerifiers.verifySignature(publicKeyBase64, zipBytes, signature)
        ).doesNotThrowAnyException();
    }

    @Test
    void should_not_verify_invalid_signature_successfully() throws Exception {
        byte[] zipBytes = zipDir("signature/sample_valid_content");
        byte[] otherSignature = toByteArray(getResource("signature/signature"));

        assertThatThrownBy(() ->
            ZipVerifiers.verifySignature(invalidPublicKeyBase64, zipBytes, otherSignature)
        )
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessage(INVALID_SIGNATURE_MESSAGE);
    }

    @Test
    void should_not_verify_valid_zip_with_wrong_public_key_successfully() throws Exception {
        byte[] zipBytes = zipDir("signature/sample_valid_content");
        byte[] signature = signWithSha256Rsa(zipBytes, toByteArray(getResource("signature/test_private_key.der")));

        assertThatThrownBy(() ->
            ZipVerifiers.verifySignature(invalidPublicKeyBase64, zipBytes, signature)
        )
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessage(INVALID_SIGNATURE_MESSAGE);
    }

    @Test
    void should_handle_sample_prod_signature() throws Exception {
        byte[] prodZip = toByteArray(getResource("signature/prod_test_envelope.zip")); // inner zip
        byte[] prodSignature = toByteArray(getResource("signature/prod_test_signature"));
        String prodPublicKey =
            Base64.getEncoder().encodeToString(toByteArray(getResource("signature/prod_public_key.der")));

        assertThatCode(() ->
            ZipVerifiers.verifySignature(prodPublicKey, prodZip, prodSignature)
        ).doesNotThrowAnyException();
    }

    @Test
    void should_verify_signature_using_nonprod_public_key_for_file_signed_using_nonprod_private_key()
        throws Exception {
        byte[] nonprodZip = toByteArray(getResource("signature/nonprod_envelope.zip")); // inner zip
        byte[] nonprodSignature = toByteArray(getResource("signature/nonprod_envelope_signature"));
        String nonprodPublicKey =
            Base64.getEncoder().encodeToString(toByteArray(getResource("nonprod_public_key.der")));

        assertThatCode(() ->
            ZipVerifiers.verifySignature(nonprodPublicKey, nonprodZip, nonprodSignature)
        ).doesNotThrowAnyException();
    }

    @Test
    void should_not_verify_signature_using_wrong_pub_key_for_file_signed_using_nonprod_private_key()
        throws Exception {
        byte[] nonprodZip = toByteArray(getResource("signature/nonprod_envelope.zip")); // inner zip
        byte[] nonprodSignature = toByteArray(getResource("signature/nonprod_envelope_signature"));

        assertThatThrownBy(() ->
            ZipVerifiers.verifySignature(publicKeyBase64, nonprodZip, nonprodSignature)
        )
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessage(INVALID_SIGNATURE_MESSAGE);
    }

    @Test
    void should_not_verify_signature_of_the_wrong_length() throws Exception {
        byte[] zipBytes = zipDir("signature/sample_valid_content");
        byte[] tooLongSignature = RandomUtils.nextBytes(256);

        assertThatThrownBy(() ->
            ZipVerifiers.verifySignature(publicKeyBase64, zipBytes, tooLongSignature)
        )
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessage(INVALID_SIGNATURE_MESSAGE)
            .hasCauseInstanceOf(SignatureException.class);
    }
}
