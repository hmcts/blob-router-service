package uk.gov.hmcts.reform.blobrouter.util;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public final class PublicKeyDecoder {

    private PublicKeyDecoder() {
        // util class
    }

    public static PublicKey decode(byte[] publicKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return KeyFactory
            .getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    }
}
