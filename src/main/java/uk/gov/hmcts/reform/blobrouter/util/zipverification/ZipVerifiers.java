package uk.gov.hmcts.reform.blobrouter.util.zipverification;

import uk.gov.hmcts.reform.blobrouter.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.blobrouter.exceptions.SignatureValidationException;

import java.io.IOException;
import java.security.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;

public class ZipVerifiers {

    public static final String ENVELOPE = "envelope.zip";
    public static final String SIGNATURE = "signature";

    public static final String INVALID_SIGNATURE_MESSAGE = "Zip signature failed verification";

    private ZipVerifiers() {
    }

    public static void verifyZip(ZipInputStream zipInputStream, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            byte[] signatureByteArray = new byte[1024];
            try (ZipInputStream zis = zipInputStream) {
                while (zis.available() != 0) {
                    ZipEntry zipEntry = zis.getNextEntry();
                    if (zipEntry.getName().equalsIgnoreCase(ENVELOPE)) {

                        int buff = 1024;
                        MessageDigest hashSum = MessageDigest.getInstance("SHA-256");
                        byte[] buffer = new byte[buff];
                        byte[] partialHash = null;

                        long read = 0;

                        // calculate the hash of the hole file for the test
                        long offset = zipEntry.getSize();
                        int unitsize;
                        while (read < offset) {
                            unitsize = (int) (((offset - read) >= buff) ? buff : (offset - read));
                            zis.read(buffer, (int)read, unitsize);

                            signature.update(buffer, (int)read, unitsize);

                            read += unitsize;
                        }
                    }
                    if (zipEntry.getName().equalsIgnoreCase(SIGNATURE)) {
                        signatureByteArray = toByteArray(zis);
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
        }
    }
}
