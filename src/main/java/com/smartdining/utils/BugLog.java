package com.smartdining.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ghi ra console Tomcat khi xay ra cac loi da biet (BUG-1..BUG-7 trong docs/testing/serious-bugs.md).
 * Chi de quan sat: khong thay doi hanh vi cua ung dung, cac loi van con nguyen.
 */
public final class BugLog {
    private static final Logger LOGGER = Logger.getLogger("SmartDining.BUG");

    private BugLog() {
    }

    /** Phat hien mot loi da biet, vd. warn(1, "Ban 5 co 3 hoa don dang mo"). */
    public static void warn(int bug, String message) {
        LOGGER.warning("[BUG-" + bug + "] " + message);
    }

    /** Loi bi nuot trong khoi catch (truoc day khong in gi). */
    public static void swallowed(String where, Exception ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        LOGGER.log(Level.SEVERE, "[LOI BI AN] " + where + ": " + root.getClass().getSimpleName() + " - " + root.getMessage());
    }
}
