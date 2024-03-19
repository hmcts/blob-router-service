package uk.gov.hmcts.reform.blobrouter.util;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 * The `PublicKeyDecoder` class in Java provides a method to decode a byte array into a public RSA key using the RSA
 * algorithm.
 */
public final class PublicKeyDecoder {

    private PublicKeyDecoder() {
        // util class
    }

    /**
     * The function decodes a byte array into a public RSA key in Java.
     *
     * @param publicKeyBytes The `publicKeyBytes` parameter is a byte array that represents the encoded
     *                       form of a public key. This method decodes the byte array into a `PublicKey`
     *                       object using the RSA algorithm.
     * @return A `PublicKey` object is being returned after decoding the provided byte array `publicKeyBytes`.
     */
    public static PublicKey decode(byte[] publicKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return KeyFactory
            .getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    }
}
