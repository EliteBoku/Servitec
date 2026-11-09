package com.servitec.servitec_backend.shared.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class TokenHashUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenHashUtil() {
    }

    /**
     * Genera un token aleatorio URL-safe de 32 bytes (256 bits de entropía).
     */
    public static String generarTokenSeguro() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Calcula el hash SHA-256 en formato hexadecimal (64 caracteres) para persistir en TOKEN_CUENTA o SESION_USUARIO.
     */
    public static String hashSha256(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en la JVM", e);
        }
    }
    public static String generateSecureToken() {

        byte[] tokenBytes = new byte[32];

        new java.security.SecureRandom().nextBytes(tokenBytes);

        return java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    public static String sha256Hex(String value) {

        try {

            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

            return java.util.HexFormat.of().formatHex(hash);

        } catch (java.security.NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "No está disponible el algoritmo SHA-256.",
                    exception
            );
        }
    }
}