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

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.List;
import java.util.zip.ZipInputStream;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.VerificationResult.OK_VERIFICATION_RESULT;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.VerificationResult.getError;

@Component
public class BlobVerifier {

    private static final Logger logger = getLogger(BlobVerifier.class);

    public static final VerificationResult INVALID_SIGNATURE_VERIFICATION_RESULT =
            getError(ErrorCode.ERR_SIG_VERIFY_FAILED, "Invalid signature");
    private static final VerificationResult INVALID_ZIP_ARCHIVE_VERIFICATION_RESULT =
            getError(ErrorCode.ERR_ZIP_PROCESSING_FAILED, "Invalid zip archive");

    private final PublicKey excelaPublicKey;
    private final PublicKey ironMountainPublicKey;

    public BlobVerifier(
        @Value("${public-key-der-file}") String excelaPublicKeyDerFilename,
        @Value("${public-key-der-file}") String ironMountainPublicKeyDerFilename
    ) {
        try {
            this.excelaPublicKey = PublicKeyDecoder.decode(toByteArray(getResource(excelaPublicKeyDerFilename)));
        } catch (Exception e) {
            throw new InvalidConfigException("Error loading Excela public key. File name: "
                                                 + excelaPublicKeyDerFilename, e);
        }

        try {
            this.ironMountainPublicKey = PublicKeyDecoder
                .decode(toByteArray(getResource(ironMountainPublicKeyDerFilename)));
        } catch (Exception e) {
            throw new InvalidConfigException("Error loading Iron Mountain public key. File name: "
                                                 + ironMountainPublicKeyDerFilename, e);
        }
    }

    public VerificationResult verifyZip(String blobName, InputStream zipSource) {
        List<PublicKey> publicKeyList = List.of(excelaPublicKey, ironMountainPublicKey);
        for (PublicKey publicKey : publicKeyList) {
            try (var zis = new ZipInputStream(zipSource)) {
                ZipVerifiers.verifyZip(zis, publicKey);
                return OK_VERIFICATION_RESULT;
            } catch (DocSignatureFailureException ex) {
                logger.info("Invalid signature using public key {}. Blob name: {}", publicKey, blobName, ex);
            } catch (InvalidZipArchiveException ex) {
                logger.info("Invalid zip archive. Blob name: {}", blobName, ex);
                return INVALID_ZIP_ARCHIVE_VERIFICATION_RESULT;
            } catch (IOException ex) {
                logger.info("Error occurred when verifying file. Blob name: {}", blobName, ex);
                return INVALID_ZIP_ARCHIVE_VERIFICATION_RESULT;
            }
        }
        return INVALID_SIGNATURE_VERIFICATION_RESULT;
    }

    public static class VerificationResult {
        public final boolean isOk;
        public final ErrorCode error;
        public final String errorDescription;

        public static final VerificationResult OK_VERIFICATION_RESULT = new VerificationResult(
            true,null, null);

        private VerificationResult(boolean isOk, ErrorCode error, String errorDescription) {
            this.isOk = isOk;
            this.error = error;
            this.errorDescription = errorDescription;
        }

        static VerificationResult getError(ErrorCode error, String reason) {
            return new VerificationResult(false, error, reason);
        }
    }
}
