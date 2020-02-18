package uk.gov.hmcts.reform.blobrouter.services;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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

    public boolean verifyZip(String blobName, byte[] rawBlob) {
        try (var zis = new ZipInputStream(new ByteArrayInputStream(rawBlob))) {

            ZipVerifiers.verifyZip(zis, publicKey);
            return true;
        } catch (DocSignatureFailureException ex) {
            logger.info("Invalid signature. Blob name: {}", blobName, ex);
            return false;
        } catch (InvalidZipArchiveException ex) {
            logger.info("Invalid zip archive. Blob name: {}", blobName, ex);
            return false;
        } catch (IOException ex) {
            logger.info("Error occurred when verifying file. Blob name: {}", blobName, ex);
            return false;
        }
    }
}
