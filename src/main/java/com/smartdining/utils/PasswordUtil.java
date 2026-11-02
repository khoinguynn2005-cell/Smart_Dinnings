package com.smartdining.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PasswordUtil {
    private PasswordUtil() {
    }

    public static String forStorage(String password) {
        return password;
    }

    public static boolean matches(String password, String storedPassword) {
        if (password == null || storedPassword == null) {
            return false;
        }
        return MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8));
    }
}
