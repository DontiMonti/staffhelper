package com.dmsh.staffhelper.config;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class ConfigSecretCodec {
    private ConfigSecretCodec() {}

    private static final String PREFIX = "enc:v1";
    private static final int PBKDF2_ITERS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_BYTES = 12;
    private static final byte[] SALT = new byte[] {
            0x33, 0x11, 0x5A, 0x6F, 0x2C, 0x71, 0x44, 0x1A,
            0x19, 0x4E, 0x58, 0x63, 0x27, 0x39, 0x0F, 0x6A
    };

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX + ":");
    }

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return "";
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            new SecureRandom().nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            String n = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
            String c = Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
            return PREFIX + ":" + n + ":" + c;
        } catch (Exception e) {
            return "";
        }
    }

    public static String decrypt(String encoded) {
        if (encoded == null || encoded.isBlank()) return "";
        if (!isEncrypted(encoded)) return encoded;
        try {
            String[] parts = encoded.split(":", 4);
            if (parts.length != 4) return "";
            byte[] nonce = Base64.getUrlDecoder().decode(parts[2]);
            byte[] cipherText = Base64.getUrlDecoder().decode(parts[3]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] out = cipher.doFinal(cipherText);
            return new String(out, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static SecretKeySpec deriveKey() throws Exception {
        String fp = buildFingerprint();
        PBEKeySpec spec = new PBEKeySpec(fp.toCharArray(), SALT, PBKDF2_ITERS, KEY_BITS);
        SecretKeyFactory kf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = kf.generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }

    private static String buildFingerprint() {
        String user = safe(System.getProperty("user.name"));
        String home = safe(System.getProperty("user.home"));
        String os = safe(System.getProperty("os.name"));
        String arch = safe(System.getProperty("os.arch"));
        String marker = modMarker();
        return home + "|" + reverse(user) + "|" + os + "|" + arch + "|" + marker;
    }

    private static String modMarker() {
        char[] c = new char[] { 's', 't', 'a', 'f', 'f', 'h', 'e', 'l', 'p', 'e', 'r', '.', 'v', '2' };
        return new String(c);
    }

    private static String reverse(String in) {
        return new StringBuilder(in).reverse().toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
