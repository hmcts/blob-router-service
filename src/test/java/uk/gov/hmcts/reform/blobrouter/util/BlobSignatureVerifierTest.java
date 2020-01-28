package uk.gov.hmcts.reform.blobrouter.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.blobrouter.services.BlobSignatureVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.blobrouter.util.DirectoryZipper.zipAndSignDir;

@ExtendWith(MockitoExtension.class)
class BlobSignatureVerifierTest {

    private BlobSignatureVerifier signatureVerifier;

    @BeforeEach
    void setUp() {
        this.signatureVerifier = new BlobSignatureVerifier("sha256withrsa", "signature/public_key.der");
    }

    @Test
    void should_return_true_when_signature_verification_is_success() throws Exception {
        // given
        byte[] zipBytes = zipAndSignDir("signature/sample_valid_content", "signature/test_private_key.der");

        // then
        assertThat(signatureVerifier.verifyZipSignature("test.zip", "testContainer", zipBytes)).isTrue();
    }

    @Test
    void should_return_false_when_signature_verification_fails() throws Exception {
        // given
        byte[] zipBytes = zipAndSignDir("signature/sample_valid_content", "signature/some_other_private_key.der");

        // then
        assertThat(signatureVerifier.verifyZipSignature("test.zip", "testContainer", zipBytes)).isFalse();
    }

}
