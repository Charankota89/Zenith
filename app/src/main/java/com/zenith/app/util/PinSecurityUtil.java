package com.zenith.app.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes PINs before they're ever written to SharedPreferences. The PIN
 * itself is never stored — only its SHA-256 hash — so it can't be read
 * back out in plaintext by anyone with access to the app's private data
 * (a rooted device, an ADB backup, etc).
 */
public final class PinSecurityUtil {

    private PinSecurityUtil() {}

    public static String hash(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(pin.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every Android runtime;
            // this branch is unreachable in practice.
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    public static boolean matches(String rawPin, String storedHash) {
        if (storedHash == null || rawPin == null) return false;
        return hash(rawPin).equals(storedHash);
    }
}
