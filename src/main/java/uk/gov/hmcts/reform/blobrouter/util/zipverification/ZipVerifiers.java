package uk.gov.hmcts.reform.blobrouter.util.zipverification;

import uk.gov.hmcts.reform.blobrouter.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.blobrouter.exceptions.SignatureValidationException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
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
                byte[] buffer = new byte[1024];
                int len;
                while (zis.available() != 0) {
                    ZipEntry zipEntry = zis.getNextEntry();
                    if (zipEntry.getName().equalsIgnoreCase(ENVELOPE)) {
                        len = zis.read(buffer);
                        signature.update(buffer, 0, len);
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
