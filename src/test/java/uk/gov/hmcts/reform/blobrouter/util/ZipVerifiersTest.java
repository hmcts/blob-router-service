package uk.gov.hmcts.reform.blobrouter.util;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;
import uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.zip.ZipInputStream;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.blobrouter.testutils.DirectoryZipper.zipAndSignDir;
import static uk.gov.hmcts.reform.blobrouter.testutils.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.blobrouter.testutils.DirectoryZipper.zipFiles;
import static uk.gov.hmcts.reform.blobrouter.testutils.SigningHelper.signWithSha256Rsa;

@ExtendWith(MockitoExtension.class)
class ZipVerifiersTest {

    private static final String INVALID_SIGNATURE_MESSAGE = "Zip signature failed verification";
    private static final String INVALID_ZIP_ENTRIES_MESSAGE = "Zip entries do not match expected file names";

    private static PublicKey publicKey;
    private static PublicKey invalidPublicKey;

    @BeforeAll
    static void setUp() throws Exception {
        publicKey = loadPublicKey("signature/test_public_key.der");
        invalidPublicKey = loadPublicKey("signature/invalid_test_public_key.der");
    }

    @Test
    void should_not_verify_envelope_without_signature() throws Exception {
        byte[] innerZip = zipDir("signature/sample_valid_content");

        byte[] zipBytes = zipFiles(new HashMap<String, byte[]>() {
            {
                put(ZipVerifiers.ENVELOPE, innerZip);
            }
        });

        assertThatThrownBy(() ->
            ZipVerifiers.verifyZip(new ZipInputStream(new ByteArrayInputStream(zipBytes)), publicKey)
        )
            .isInstanceOf(InvalidZipArchiveException.class);
    }

    @Test
    void should_not_verify_more_than_2_files_successfully() throws Exception {
        byte[] innerZip = zipDir("signature/sample_valid_content");
        byte[] signature = signWithSha256Rsa(innerZip, toByteArray(getResource("signature/test_private_key.der")));

        byte[] zipBytes = zipFiles(new HashMap<String, byte[]>() {
            {
                put(ZipVerifiers.ENVELOPE, innerZip);
                put(ZipVerifiers.SIGNATURE, signature);
                put("signature2", signature);
            }
        });

        assertThatThrownBy(() ->
            ZipVerifiers.verifyZip(new ZipInputStream(new ByteArrayInputStream(zipBytes)), publicKey)
        )
            .isInstanceOf(InvalidZipArchiveException.class)
            .hasMessageContaining(INVALID_ZIP_ENTRIES_MESSAGE);
    }

    @Test
    void should_not_verify_invalid_filenames_successfully() throws Exception {
        byte[] innerZip = zipDir("signature/sample_valid_content");
        byte[] signature = signWithSha256Rsa(innerZip, toByteArray(getResource("signature/test_private_key.der")));

        byte[] zipBytes = zipFiles(new HashMap<String, byte[]>() {
            {
                put(ZipVerifiers.ENVELOPE, innerZip);
                put("signature.sig", signature);
            }
        });

        assertThatThrownBy(() ->
            ZipVerifiers.verifyZip(new ZipInputStream(new ByteArrayInputStream(zipBytes)), publicKey))
            .isInstanceOf(InvalidZipArchiveException.class)
            .hasMessageContaining(INVALID_ZIP_ENTRIES_MESSAGE);
    }

    @Test
    void should_verify_valid_zip_successfully() throws Exception {
        byte[] zipBytes = zipAndSignDir("signature/sample_valid_content", "signature/test_private_key.der");

        assertThatCode(() ->
            ZipVerifiers.verifyZip(new ZipInputStream(new ByteArrayInputStream(zipBytes)), publicKey)
        ).doesNotThrowAnyException();
    }

    @Test
    void should_not_verify_invalid_zip_successfully() throws Exception {
        byte[] zipBytes = zipAndSignDir("signature/sample_valid_content", "signature/some_other_private_key.der");
        assertThrows(
            DocSignatureFailureException.class,
            () -> ZipVerifiers.verifyZip(new ZipInputStream(new ByteArrayInputStream(zipBytes)), publicKey)
        );
    }


    @Test
    void should_not_verify_valid_zip_with_wrong_public_key_successfully() throws Exception {
        byte[] zipBytes = zipAndSignDir("signature/sample_valid_content", "signature/test_private_key.der");

        assertThatThrownBy(() ->
            ZipVerifiers.verifyZip(new ZipInputStream(new ByteArrayInputStream(zipBytes)), invalidPublicKey)
        )
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessage(INVALID_SIGNATURE_MESSAGE);
    }

    @Test
    void should_handle_sample_prod_signature() throws Exception {

        byte[] zipBytes = zipFiles(new HashMap<String, byte[]>() {
            {
                put(ZipVerifiers.ENVELOPE, toByteArray(getResource("signature/prod_test_envelope.zip")));
                put(ZipVerifiers.SIGNATURE, toByteArray(getResource("signature/prod_test_signature")));
            }
        });

        PublicKey prodPublicKey = loadPublicKey("signature/prod_public_key.der");

        assertThatCode(() ->
            ZipVerifiers.verifyZip(new ZipInputStream(new ByteArrayInputStream(zipBytes)), prodPublicKey)
        ).doesNotThrowAnyException();
    }

    @Test
    void should_verify_signature_using_nonprod_public_key_for_file_signed_using_nonprod_private_key()
        throws Exception {

        byte[] zipBytes = zipFiles(new HashMap<String, byte[]>() {
            {
                put(ZipVerifiers.ENVELOPE, toByteArray(getResource("signature/nonprod_envelope.zip")));
                put(ZipVerifiers.SIGNATURE, toByteArray(getResource("signature/nonprod_envelope_signature")));
            }
        });

        PublicKey nonprodPublicKey = loadPublicKey("nonprod_public_key.der");

        assertThatCode(() ->
            ZipVerifiers.verifyZip(new ZipInputStream(new ByteArrayInputStream(zipBytes)), nonprodPublicKey)
        ).doesNotThrowAnyException();
    }

    @Test
    void should_not_verify_signature_using_wrong_pub_key_for_file_signed_using_nonprod_private_key()
        throws Exception {

        byte[] zipBytes = zipFiles(new HashMap<String, byte[]>() {
            {
                put(ZipVerifiers.ENVELOPE, toByteArray(getResource("signature/nonprod_envelope.zip")));
                put(ZipVerifiers.SIGNATURE, toByteArray(getResource("signature/nonprod_envelope_signature")));
            }
        });

        assertThatThrownBy(() ->
            ZipVerifiers.verifyZip(new ZipInputStream(new ByteArrayInputStream(zipBytes)), publicKey)
        )
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessage(INVALID_SIGNATURE_MESSAGE);
    }

    @Test
    void should_not_verify_signature_of_the_wrong_length() throws Exception {
        byte[] zipBytes = zipFiles(new HashMap<String, byte[]>() {
            {
                put(ZipVerifiers.ENVELOPE, zipDir("signature/sample_valid_content"));
                put(ZipVerifiers.SIGNATURE, RandomUtils.nextBytes(256));
            }
        });

        assertThatThrownBy(() ->
            ZipVerifiers.verifyZip(new ZipInputStream(new ByteArrayInputStream(zipBytes)), publicKey)
        )
            .isInstanceOf(DocSignatureFailureException.class)
            .hasMessage(INVALID_SIGNATURE_MESSAGE)
            .hasCauseInstanceOf(SignatureException.class);
    }

    private static PublicKey loadPublicKey(String fileName) throws Exception {
        return PublicKeyDecoder.decode(toByteArray(getResource(fileName)));
    }
}
