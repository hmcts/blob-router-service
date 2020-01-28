package uk.gov.hmcts.reform.blobrouter.services;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.blobrouter.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.blobrouter.tasks.processors.BlobProcessor;
import uk.gov.hmcts.reform.blobrouter.util.ZipVerifiers;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipInputStream;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class BlobSignatureVerifier {

    private static final Logger logger = getLogger(BlobProcessor.class);

    private final String signatureAlg;
    private final String publicKeyDerFilename;

    public BlobSignatureVerifier(
        @Value("${storage-signature-algorithm}") String signatureAlg,
        @Value("${public-key-der-file}") String publicKeyDerFilename
    ) {
        this.signatureAlg = signatureAlg;
        this.publicKeyDerFilename = publicKeyDerFilename;
    }

    public boolean verifyZipSignature(
        String blobName,
        String container,
        byte[] rawBlob
    ) {
        try {
            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(rawBlob));
            ZipVerifiers.ZipStreamWithSignature zipWithSignature =
                ZipVerifiers.ZipStreamWithSignature.fromKeyfile(zis, publicKeyDerFilename, container);

            ZipVerifiers.getPreprocessor(signatureAlg).apply(zipWithSignature);
            return true;
        } catch (DocSignatureFailureException ex) {
            logger.warn("Rejected blob name: {} from container: {} - invalid signature", blobName, container, ex);
            return false;
        }
    }
}
