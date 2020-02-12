package uk.gov.hmcts.reform.blobrouter.util;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class PublicKeyDecoder {

    private PublicKeyDecoder() {
        // util class
    }

    public static PublicKey decode(String publicKeyBase64) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return KeyFactory
            .getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
    }
}
