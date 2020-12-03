package uk.gov.hmcts.reform.blobrouter.util.zipverification;

import uk.gov.hmcts.reform.blobrouter.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;
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
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {

                    if (zipEntry.getName().equalsIgnoreCase(ENVELOPE)) {

                        int bufferSize = 1024;
                        while (zis.available() != 0) {
                            byte[] envelopeData = new byte[bufferSize];
                            int numBytesRead = zis.readNBytes(envelopeData, 0, bufferSize);
                            signature.update(envelopeData, 0, numBytesRead);
//                            signature.update(zis.readAllBytes()); // works
                            if (numBytesRead < bufferSize) {
                                // we know we're at the end
                                break;
                            }
                        }
                    } else if (zipEntry.getName().equalsIgnoreCase(SIGNATURE)) {
                        signatureByteArray = toByteArray(zis);
                    } else {
                        throw new InvalidZipArchiveException("Zip entries do not match expected file names. Found file named " + zipEntry.getName());
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
