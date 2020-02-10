package uk.gov.hmcts.reform.blobrouter.services;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;
import uk.gov.hmcts.reform.blobrouter.util.zipverification.ZipVerifiers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class BlobSignatureVerifier {

    private static final Logger logger = getLogger(BlobSignatureVerifier.class);

    private final String publicKeyDerFilename;

    public BlobSignatureVerifier(
        @Value("${public-key-der-file}") String publicKeyDerFilename
    ) {
        this.publicKeyDerFilename = publicKeyDerFilename;
    }

    public boolean verifyZipSignature(String blobName, byte[] rawBlob) {
        try (var zis = new ZipInputStream(new ByteArrayInputStream(rawBlob))) {
            var zipWithSignature = ZipVerifiers.ZipStreamWithSignature.fromKeyfile(zis, publicKeyDerFilename);

            ZipVerifiers.verifyZip(zipWithSignature);
            return true;
        } catch (DocSignatureFailureException ex) {
            logger.info("Invalid signature. Blob name: {}", blobName, ex);
            return false;
        } catch (InvalidZipArchiveException ex) {
            logger.info("Invalid zip archive. Blob name: {}", blobName, ex);
            return false;
        } catch (IOException ex) {
            logger.info("Error occurred when verifying signature. Blob name: {}", blobName, ex);
            return false;
        }
    }
}
