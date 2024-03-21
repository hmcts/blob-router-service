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
import java.util.zip.ZipInputStream;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.VerificationResult.OK_VERIFICATION_RESULT;
import static uk.gov.hmcts.reform.blobrouter.services.BlobVerifier.VerificationResult.getError;

/**
 * The `BlobVerifier` class in Java is responsible for verifying zip files using a public key and returning appropriate
 * verification results based on the outcome.
 */
@Component
public class BlobVerifier {

    private static final Logger logger = getLogger(BlobVerifier.class);

    public static final VerificationResult INVALID_SIGNATURE_VERIFICATION_RESULT =
            getError(ErrorCode.ERR_SIG_VERIFY_FAILED, "Invalid signature");
    private static final VerificationResult INVALID_ZIP_ARCHIVE_VERIFICATION_RESULT =
            getError(ErrorCode.ERR_ZIP_PROCESSING_FAILED, "Invalid zip archive");

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

    /**
     * The `verifyZip` function takes a blob name and an input stream containing a zip file,
     * verifies the zip file using a public key, and returns a verification result based on the outcome.
     *
     * @param blobName The `blobName` parameter is a string that represents the name or identifier of the
     *                 blob (file) being verified. It is used for logging purposes to identify the
     *                 specific blob that is being processed during the verification process.
     * @param zipSource The `zipSource` parameter in the `verifyZip` method is an `InputStream` that
     *                  represents the source of the ZIP file that needs to be verified. This input
     *                  stream is used to read the contents of the ZIP file during the verification process.
     * @return The method `verifyZip` returns a `VerificationResult` object. Depending on the outcome of
     *      the verification process, it will return one of the following results:
     *      - `OK_VERIFICATION_RESULT` if the zip file is successfully verified.
     *      - `INVALID_SIGNATURE_VERIFICATION_RESULT` if the signature of the zip file is invalid.
     *      - `INVALID_ZIP_ARCHIVE_VERIFICATION_RESULT` if the zip archive is invalid or an error occurs.
     */
    public VerificationResult verifyZip(String blobName, InputStream zipSource) {
        try (var zis = new ZipInputStream(zipSource)) {

            ZipVerifiers.verifyZip(zis, publicKey);
            return OK_VERIFICATION_RESULT;
        } catch (DocSignatureFailureException ex) {
            logger.info("Invalid signature. Blob name: {}", blobName, ex);
            return INVALID_SIGNATURE_VERIFICATION_RESULT;
        } catch (InvalidZipArchiveException ex) {
            logger.info("Invalid zip archive. Blob name: {}", blobName, ex);
            return INVALID_ZIP_ARCHIVE_VERIFICATION_RESULT;
        } catch (IOException ex) {
            logger.info("Error occurred when verifying file. Blob name: {}", blobName, ex);
            return INVALID_ZIP_ARCHIVE_VERIFICATION_RESULT;
        }
    }

    /**
     * The `VerificationResult` class in Java represents the result of a verification process, indicating whether it was
     * successful or not along with any associated error code and description.
     */
    public static class VerificationResult {
        public final boolean isOk;
        public final ErrorCode error;
        public final String errorDescription;

        public static final VerificationResult OK_VERIFICATION_RESULT = new VerificationResult(true, null, null);

        private VerificationResult(boolean isOk, ErrorCode error, String errorDescription) {
            this.isOk = isOk;
            this.error = error;
            this.errorDescription = errorDescription;
        }

        /**
         * The function `getError` creates a new `VerificationResult` object with a specified error code and reason.
         *
         * @param error The `error` parameter is of type `ErrorCode`, which is likely an enumeration or
         *              a class representing different error codes or types. It is used to specify the
         *              type of error that occurred during verification.
         * @param reason The `reason` parameter in the `getError` method represents the explanation or
         *               description of the error that occurred. It provides additional context or
         *               details about why the error occurred.
         * @return An instance of the `VerificationResult` class with the parameters `false`, `error`, and `reason`.
         */
        static VerificationResult getError(ErrorCode error, String reason) {
            return new VerificationResult(false, error, reason);
        }
    }
}
