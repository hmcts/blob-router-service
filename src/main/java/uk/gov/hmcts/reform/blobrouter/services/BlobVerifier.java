package uk.gov.hmcts.reform.blobrouter.services;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.data.events.ErrorCode;
import uk.gov.hmcts.reform.blobrouter.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidConfigException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;
import uk.gov.hmcts.reform.blobrouter.util.PublicKeyDecoder;
import uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.zip.ZipInputStream;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.VerificationResult.error;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.VerificationResult.ok;

@Component
public class BlobVerifier {

    private static final Logger logger = getLogger(BlobVerifier.class);

    private final PublicKey publicKey;

    public BlobVerifier(
        @Value("${public-key-der-file}") String publicKeyDerFilename
    ) {
        try {
            this.publicKey = PublicKeyDecoder.decode(toByteArray(getResource(publicKeyDerFilename)));
        } catch (Exception e) {
            throw new InvalidConfigException("Error loading public key. File name: " + publicKeyDerFilename, e);
        }
    }

    public VerificationResult verifyZip(String blobName, byte[] rawBlob) {
        try (var zis = new ZipInputStream(new ByteArrayInputStream(rawBlob))) {

            ZipVerifiers.verifyZip(zis, publicKey);
            return ok();
        } catch (DocSignatureFailureException ex) {
            logger.info("Invalid signature. Blob name: {}", blobName, ex);
            return error(ErrorCode.ERR_SIG_VERIFY_FAILED, "Invalid signature");
        } catch (InvalidZipArchiveException ex) {
            logger.info("Invalid zip archive. Blob name: {}", blobName, ex);
            return error(ErrorCode.ERR_METAFILE_INVALID, "Invalid zip archive");
        } catch (IOException ex) {
            logger.info("Error occurred when verifying file. Blob name: {}", blobName, ex);
            return error(null, null);
        }
    }

    public static class VerificationResult {
        public final boolean isOk;
        public final ErrorCode error;
        public final String errorDescription;

        private VerificationResult(boolean isOk, ErrorCode error, String errorDescription) {
            this.isOk = isOk;
            this.error = error;
            this.errorDescription = errorDescription;
        }

        public static VerificationResult ok() {
            return new VerificationResult(true, null, null);
        }

        public static VerificationResult error(ErrorCode error, String reason) {
            return new VerificationResult(false, error, reason);
        }
    }
}
