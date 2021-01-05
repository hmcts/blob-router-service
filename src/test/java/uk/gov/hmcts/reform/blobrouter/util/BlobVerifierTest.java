package uk.gov.hmcts.reform.blobrouter.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidConfigException;
import uk.gov.hmcts.reform.blobrouter.services.BlobVerifier;

import java.security.spec.InvalidKeySpecException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.blobrouter.testutils.DirectoryZipper.zipAndSignDir;
import static uk.gov.hmcts.reform.blobrouter.testutils.DirectoryZipper.zipDir;

@ExtendWith(MockitoExtension.class)
class  BlobVerifierTest {

    private BlobVerifier verifier;

    @BeforeEach
    void setUp() {
        this.verifier = new BlobVerifier("signature/test_public_key.der");
    }

    @Test
    void should_return_ok_when_signature_verification_is_success() throws Exception {
        // given
        byte[] zipBytes = zipAndSignDir("signature/sample_valid_content", "signature/test_private_key.der");

        // then
        assertThat(verifier.verifyZip("test.zip", zipBytes).isOk).isTrue();
    }

    @Test
    void should_return_error_when_signature_verification_fails() throws Exception {
        // given
        byte[] zipBytes = zipAndSignDir("signature/sample_valid_content", "signature/some_other_private_key.der");

        // then
        var result = verifier.verifyZip("test.zip", zipBytes);
        assertThat(result.isOk).isFalse();
        assertThat(result.error).isEqualTo(ErrorCode.ERR_SIG_VERIFY_FAILED);
        assertThat(result.errorDescription).isEqualTo("Invalid signature");
    }


    @Test
    void should_return_error_when_zip_content_is_invalid() throws Exception {
        // given
        byte[] zipBytes = zipDir("signature/sample_valid_content"); // no signature

        // then
        var result = verifier.verifyZip("test.zip", zipBytes);
        assertThat(result.isOk).isFalse();
        assertThat(result.error).isEqualTo(ErrorCode.ERR_ZIP_PROCESSING_FAILED);
        assertThat(result.errorDescription).isEqualTo("Invalid zip archive");
    }

    @Test
    void should_return_error_when_zip_file_is_corrupted() throws Exception {
        // given
        byte[] zipBytes = "not zip file".getBytes();

        // then
        var result = verifier.verifyZip("test.zip", zipBytes);
        assertThat(result.isOk).isFalse();
        assertThat(result.error).isEqualTo(ErrorCode.ERR_ZIP_PROCESSING_FAILED);
        assertThat(result.errorDescription).isEqualTo("Invalid zip archive");
    }

    @Test
    void should_throw_exception_if_invalid_file_name_is_provided() {
        assertThatThrownBy(() -> new BlobVerifier("signature/i_dont_exist.der"))
            .isInstanceOf(InvalidConfigException.class)
            .hasMessageContaining("Error loading public key")
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_throw_exception_if_invalid_public_key_is_provided() {
        assertThatThrownBy(() -> new BlobVerifier("signature/invalid_public_key_format.der"))
            .isInstanceOf(InvalidConfigException.class)
            .hasMessageContaining("Error loading public key")
            .hasCauseInstanceOf(InvalidKeySpecException.class);
    }
}
