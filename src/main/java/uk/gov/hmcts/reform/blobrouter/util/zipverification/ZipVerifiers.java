package uk.gov.hmcts.reform.blobrouter.util.zipverification;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.blobrouter.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.blobrouter.exceptions.InvalidZipArchiveException;
import uk.gov.hmcts.reform.blobrouter.exceptions.SignatureValidationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static java.util.Arrays.asList;

public class ZipVerifiers {

    public static final String ENVELOPE = "envelope.zip";
    public static final String SIGNATURE = "signature";

    public static final String INVALID_SIGNATURE_MESSAGE = "Zip signature failed verification";

    private ZipVerifiers() {
    }

    public static ZipInputStream verifyZip(ZipStreamWithSignature zipWithSignature) {
        Map<String, byte[]> zipEntries = extractZipEntries(zipWithSignature.zipInputStream);

        verifyFileNames(zipEntries.keySet());
        verifySignature(
            zipWithSignature.publicKeyBase64,
            zipEntries.get(ENVELOPE),
            zipEntries.get(SIGNATURE)
        );

        return new ZipInputStream(new ByteArrayInputStream(zipEntries.get(ENVELOPE)));
    }

    private static Map<String, byte[]> extractZipEntries(ZipInputStream zis) {
        try {
            Map<String, byte[]> zipEntries = new HashMap<>();
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                zipEntries.put(zipEntry.getName(), toByteArray(zis));
            }

            return zipEntries;
        } catch (IOException ioe) {
            throw new InvalidZipArchiveException("Error extracting zip entries", ioe);
        }
    }

    public static void verifyFileNames(Set<String> fileNames) {
        if (!(fileNames.size() == 2 && fileNames.containsAll(asList(ENVELOPE, SIGNATURE)))) {
            throw new InvalidZipArchiveException(
                "Zip entries do not match expected file names. Actual names = " + fileNames
            );
        }
    }

    public static void verifySignature(String publicKeyBase64, byte[] data, byte[] signed) {
        PublicKey publicKey = decodePublicKey(publicKeyBase64);
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data);
            if (!signature.verify(signed)) {
                throw new DocSignatureFailureException(INVALID_SIGNATURE_MESSAGE);
            }
        } catch (SignatureException e) {
            throw new DocSignatureFailureException(INVALID_SIGNATURE_MESSAGE, e);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SignatureValidationException(e);
        }
    }

    private static PublicKey decodePublicKey(String publicKeyBase64) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SignatureValidationException(e);
        }
    }

    public static class PublicKeyFile {
        public final String derFilename;
        public final String publicKeyBase64;

        public PublicKeyFile(String derFilename, String publicKeyBase64) {
            this.derFilename = derFilename;
            this.publicKeyBase64 = publicKeyBase64;
        }
    }

    public static class ZipStreamWithSignature {
        public final ZipInputStream zipInputStream;
        public final String publicKeyBase64;

        private static PublicKeyFile cachedPublicKeyFile;

        public ZipStreamWithSignature(
            ZipInputStream zipInputStream,
            String publicKeyBase64
        ) {
            this.zipInputStream = zipInputStream;
            this.publicKeyBase64 = publicKeyBase64;
        }

        public static ZipStreamWithSignature fromKeyfile(
            ZipInputStream zipInputStream,
            String publicKeyDerFile
        ) {
            try {
                String publicKeyBase64 = null;
                if (cachedPublicKeyFile != null && publicKeyDerFile.equals(cachedPublicKeyFile.derFilename)) {
                    publicKeyBase64 = cachedPublicKeyFile.publicKeyBase64;
                } else if (StringUtils.isNotEmpty(publicKeyDerFile)) {
                    publicKeyBase64 = Base64.getEncoder().encodeToString(toByteArray(getResource(publicKeyDerFile)));
                    cachedPublicKeyFile = new PublicKeyFile(publicKeyDerFile, publicKeyBase64);
                }

                return new ZipStreamWithSignature(zipInputStream, publicKeyBase64);
            } catch (IOException e) {
                throw new SignatureValidationException(e);
            }
        }
    }
}
