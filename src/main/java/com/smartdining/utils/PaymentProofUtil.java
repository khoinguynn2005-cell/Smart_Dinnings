package com.smartdining.utils;

import com.smartdining.enums.PaymentMethod;

/** Quy tac chung cho anh bill: dung cho thanh toan tai ban va don mang ve. */
public final class PaymentProofUtil {
    /** Anh bill dang data URL base64: gioi han ~3MB de khong lam nang DB. */
    public static final int MAX_IMAGE_LENGTH = 4_000_000;

    public enum Problem { IMAGE_REQUIRED, INVALID_IMAGE }

    private PaymentProofUtil() {
    }

    /** null neu hop le; chuyen khoan bat buoc anh JPEG/PNG/WEBP, tien mat khong can anh. */
    public static Problem validate(PaymentMethod method, String image) {
        if (method != PaymentMethod.TRANSFER) {
            return null;
        }
        if (image == null || image.isBlank()) {
            return Problem.IMAGE_REQUIRED;
        }
        if (image.length() > MAX_IMAGE_LENGTH || !image.matches("^data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/=]+$")) {
            return Problem.INVALID_IMAGE;
        }
        return null;
    }
}
