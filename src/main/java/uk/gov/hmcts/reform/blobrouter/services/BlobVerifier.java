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
import java.io.InputStream;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
        @Value("${public-key-der-file-two}") String ironMountainPublicKeyDerFilename
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
        byte[] zipBytes;

        // Read zip source into byte array to ensure that verification uses an unmodified input each time.
        try {
            zipBytes = zipSource.readAllBytes();
        } catch (IOException ex) {
            logger.error("Error reading zip file into byte array", ex);
            return INVALID_ZIP_ARCHIVE_VERIFICATION_RESULT;
        }

        // Verify zip file, try both public keys, if fails for any other reason it won't check the other public key.
        for (PublicKey publicKey : publicKeyList) {
            try {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(zipBytes);
                ZipInputStream zis = new ZipInputStream(byteArrayInputStream);
                ZipVerifiers.verifyZip(zis, publicKey);
                zis.close();
                return OK_VERIFICATION_RESULT;
            } catch (DocSignatureFailureException ex) {
                logger.info(createLogMessage("Invalid signature", blobName, publicKey, ex));
            } catch (InvalidZipArchiveException ex) {
                logger.info(createLogMessage("Invalid zip archive", blobName, publicKey, ex));
                return INVALID_ZIP_ARCHIVE_VERIFICATION_RESULT;
            } catch (IOException ex) {
                logger.info(createLogMessage("Error occurred when verifying file", blobName,
                                             publicKey, ex));
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

    /**
     * Creates a formatted String with the provided parameters to be logged, used when checking an uploaded file.
     * @param verificationResult A string for the reason it failed e.g. Invalid signature or Invalid Zip Archive.
     * @param blobName The name of the blob that has failed processing.
     * @param publicKey The public key that was being used when the check failed so we know which supplier it was.
     * @param ex The exception that occurred.
     * @return A formatted string with all the required details that can be info logged.
     */
    private String createLogMessage(String verificationResult, String blobName, PublicKey publicKey, Exception ex) {
        Map<PublicKey, String> publicKeyMap = new ConcurrentHashMap<>();
        publicKeyMap.put(excelaPublicKey, "Excela");
        publicKeyMap.put(ironMountainPublicKey, "Iron Mountain");

        String publicKeyName = publicKeyMap.getOrDefault(publicKey, "Unknown");

        return String.format("%s. Blob name: %s. Public key: %s. Exception: %s", verificationResult, blobName,
                             publicKeyName, ex);
    }
}
