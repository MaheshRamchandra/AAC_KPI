package com.aac.kpi.converter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Helper methods to safely parse optional values coming from Excel.
 * All methods return null instead of throwing when input is blank/invalid.
 */
public final class ValueUtil {
    private ValueUtil() {
    }

    public static String stripDecimal(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int dotIndex = trimmed.indexOf('.');
        return dotIndex >= 0 ? trimmed.substring(0, dotIndex) : trimmed;
    }

    public static Integer toInteger(String value) {
        String stripped = stripDecimal(value);
        if (stripped == null) {
            return null;
        }
        try {
            return Integer.parseInt(stripped);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Float toFloat(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String normalizeNumberString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim()).toPlainString();
        } catch (NumberFormatException e) {
            return raw.trim();
        }
    }

    public static List<String> splitRefs(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(raw.split("##"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static String cleanPostalCode(String raw) {
        String stripped = stripDecimal(raw);
        if (stripped == null || stripped.trim().isEmpty()) {
            return null;
        }
        return stripped.trim();
    }
}
