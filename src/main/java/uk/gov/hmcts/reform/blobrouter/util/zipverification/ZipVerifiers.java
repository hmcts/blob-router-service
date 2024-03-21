package uk.gov.hmcts.reform.blobrouter.util.zipverification;

import uk.gov.hmcts.reform.blobrouter.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;
import uk.gov.hmcts.reform.blobrouter.exceptions.SignatureValidationException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The `ZipVerifiers` class in Java provides a method to verify the contents of a zip file using a digital signature and
 * throws exceptions for invalid signatures or zip archives.
 */
public class ZipVerifiers {

    public static final String ENVELOPE = "envelope.zip";
    public static final String SIGNATURE = "signature";
    public static final int BUFFER_SIZE = 1024;

    public static final String INVALID_SIGNATURE_MESSAGE = "Zip signature failed verification";

    private ZipVerifiers() {
    }

    /**
     * The `verifyZip` function reads a zip file, verifies its contents using a digital signature, and throws exceptions
     * for invalid signatures or zip archives.
     *
     * @param zis The `zis` parameter in the `verifyZip` method is a `ZipInputStream` object.
     *            This object is used to read ZIP file entries sequentially. It allows you to read the
     *            contents of a ZIP file entry and navigate through the entries in the ZIP file.
     * @param publicKey The `publicKey` parameter in the `verifyZip` method is of type `PublicKey` and is
     *                  used to verify the digital signature of the data in the ZipInputStream. The public
     *                  key is used to verify that the signature matches the data and was signed by the
     *                  corresponding private key.
     */
    public static void verifyZip(ZipInputStream zis, PublicKey publicKey) throws IOException {
        try {

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            byte[] signatureByteArray = null;

            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {

                if (zipEntry.getName().equalsIgnoreCase(ENVELOPE)) {

                    byte[] envelopeData = new byte[BUFFER_SIZE];
                    while (zis.available() != 0) {
                        int numBytesRead = zis.readNBytes(envelopeData, 0, BUFFER_SIZE);
                        signature.update(envelopeData, 0, numBytesRead);
                    }
                } else if (zipEntry.getName().equalsIgnoreCase(SIGNATURE)) {
                    signatureByteArray = zis.readAllBytes();
                } else {
                    throw new InvalidZipArchiveException(
                        "Zip entries do not match expected file names. Found file named " + zipEntry.getName()
                    );
                }
            }

            if (Objects.isNull(signatureByteArray)) {
                throw new InvalidZipArchiveException(
                    "Invalid zip archive"
                );
            }
            if (!signature.verify(signatureByteArray)) {
                throw new DocSignatureFailureException(INVALID_SIGNATURE_MESSAGE);
            }
        } catch (SignatureException e) {
            throw new DocSignatureFailureException(INVALID_SIGNATURE_MESSAGE, e);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SignatureValidationException(e);
        }
    }
}
