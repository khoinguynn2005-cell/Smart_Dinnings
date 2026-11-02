package com.smartdining.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.smartdining.entity.Employee;

/**
 * Ma xac thuc email cho tai khoan nhan vien moi (trang thai PENDING).
 * Ma chi luu trong bo nho: khoi dong lai server thi ma cu mat, nhan vien bam "Gui lai ma" de nhan ma moi.
 */
public final class VerificationCodeUtil {
    public static final Duration CODE_LIFETIME = Duration.ofMinutes(15);
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    public static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, CodeEntry> CODES = new ConcurrentHashMap<>();

    public enum Result { OK, WRONG, EXPIRED, TOO_MANY_ATTEMPTS }

    private VerificationCodeUtil() {
    }

    /** Tao ma moi (thay ma cu neu co) va gui email. Tra ve false neu nhan vien khong co email. */
    public static boolean issueAndSend(Employee employee) {
        if (employee == null || employee.getId() == null || employee.getEmail() == null
                || employee.getEmail().isBlank()) {
            return false;
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Instant now = Instant.now();
        CODES.put(employee.getId(), new CodeEntry(code, now, now.plus(CODE_LIFETIME), 0));
        XMail.sendVerificationCodeAsync(employee, code, CODE_LIFETIME.toMinutes());
        return true;
    }

    public static boolean hasActiveCode(String employeeId) {
        CodeEntry entry = employeeId == null ? null : CODES.get(employeeId);
        return entry != null && entry.expiresAt().isAfter(Instant.now()) && entry.attempts() < MAX_ATTEMPTS;
    }

    /** So giay con phai doi truoc khi duoc gui lai ma (0 = gui duoc ngay). */
    public static long secondsUntilResend(String employeeId) {
        CodeEntry entry = employeeId == null ? null : CODES.get(employeeId);
        if (entry == null) {
            return 0;
        }
        long seconds = Duration.between(Instant.now(), entry.issuedAt().plus(RESEND_COOLDOWN)).toSeconds();
        return Math.max(0, seconds);
    }

    public static Result verify(String employeeId, String code) {
        if (employeeId == null) {
            return Result.EXPIRED;
        }
        CodeEntry entry = CODES.get(employeeId);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            CODES.remove(employeeId);
            return Result.EXPIRED;
        }
        if (entry.attempts() >= MAX_ATTEMPTS) {
            return Result.TOO_MANY_ATTEMPTS;
        }
        String input = code == null ? "" : code.trim();
        if (MessageDigest.isEqual(entry.code().getBytes(StandardCharsets.UTF_8),
                input.getBytes(StandardCharsets.UTF_8))) {
            CODES.remove(employeeId);
            return Result.OK;
        }
        CodeEntry failed = new CodeEntry(entry.code(), entry.issuedAt(), entry.expiresAt(), entry.attempts() + 1);
        CODES.put(employeeId, failed);
        return failed.attempts() >= MAX_ATTEMPTS ? Result.TOO_MANY_ATTEMPTS : Result.WRONG;
    }

    /** So giay con lai truoc khi ma het han (0 = khong co ma / da het han). */
    public static long secondsUntilExpiry(String employeeId) {
        CodeEntry entry = employeeId == null ? null : CODES.get(employeeId);
        if (entry == null) {
            return 0;
        }
        return Math.max(0, Duration.between(Instant.now(), entry.expiresAt()).toSeconds());
    }

    public static int remainingAttempts(String employeeId) {
        CodeEntry entry = employeeId == null ? null : CODES.get(employeeId);
        return entry == null ? 0 : Math.max(0, MAX_ATTEMPTS - entry.attempts());
    }

    private record CodeEntry(String code, Instant issuedAt, Instant expiresAt, int attempts) {
    }
}
