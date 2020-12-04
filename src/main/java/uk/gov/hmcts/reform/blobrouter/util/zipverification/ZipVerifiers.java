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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipVerifiers {

    public static final String ENVELOPE = "envelope.zip";
    public static final String SIGNATURE = "signature";
    public static final int BUFFER_SIZE = 1024;

    public static final String INVALID_SIGNATURE_MESSAGE = "Zip signature failed verification";

    private ZipVerifiers() {
    }

    public static void verifyZip(ZipInputStream zipInputStream, PublicKey publicKey) throws IOException {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            byte[] signatureByteArray = null;
            try (ZipInputStream zis = zipInputStream) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {

                    if (zipEntry.getName().equalsIgnoreCase(ENVELOPE)) {

                        while (zis.available() != 0) {
                            byte[] envelopeData = new byte[BUFFER_SIZE];
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
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!signature.verify(signatureByteArray)) {
                throw new DocSignatureFailureException(INVALID_SIGNATURE_MESSAGE);
            }
        } catch (SignatureException e) {
            throw new DocSignatureFailureException(INVALID_SIGNATURE_MESSAGE, e);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SignatureValidationException(e);
        } catch (NullPointerException e) {
            throw new IOException(e);
        }
    }
}
