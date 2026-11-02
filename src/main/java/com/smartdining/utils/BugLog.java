package com.smartdining.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ghi log cac loi bi nuot trong khoi catch de con dau vet khi dieu tra su co.
 */
public final class BugLog {
    private static final Logger LOGGER = Logger.getLogger("SmartDining.BUG");

    private BugLog() {
    }

    /** Loi bi nuot trong khoi catch. */
    public static void swallowed(String where, Exception ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        LOGGER.log(Level.SEVERE, "[LOI BI AN] " + where + ": " + root.getClass().getSimpleName() + " - " + root.getMessage());
    }
}
