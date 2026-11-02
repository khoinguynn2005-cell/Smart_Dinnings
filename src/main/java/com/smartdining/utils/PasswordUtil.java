package com.smartdining.utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Luu mat khau nhan vien dang PBKDF2-HMAC-SHA256: pbkdf2_sha256$<so_vong>$<salt>$<hash>.
 * Mat khau cu luu dang chu thuong (truoc khi ma hoa) van duoc doi chieu duoc; LoginServlet
 * se ma hoa lai sau lan dang nhap thanh cong dau tien (xem {@link #needsRehash(String)}).
 */
public final class PasswordUtil {
    private static final String PREFIX = "pbkdf2_sha256";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 310_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String forStorage(String password) {
        if (password == null) {
            return null;
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        return PREFIX + "$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean matches(String password, String storedPassword) {
        if (password == null || storedPassword == null) {
            return false;
        }
        if (!isHashed(storedPassword)) {
            return MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8),
                    storedPassword.getBytes(StandardCharsets.UTF_8));
        }
        String[] parts = storedPassword.split("\\$");
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /** Mat khau con dang chu thuong: can ghi lai dang ma hoa sau khi dang nhap thanh cong. */
    public static boolean needsRehash(String storedPassword) {
        return storedPassword != null && !isHashed(storedPassword);
    }

    private static boolean isHashed(String storedPassword) {
        return storedPassword.startsWith(PREFIX + "$");
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
            try {
                return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Khong the ma hoa mat khau", ex);
        }
    }
}
