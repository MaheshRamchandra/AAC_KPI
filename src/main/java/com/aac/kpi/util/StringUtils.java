package com.aac.kpi.util;

public final class StringUtils {
    private StringUtils() {}

    public static String sanitizeAlphaNum(String s) {
        if (s == null) return "";
        return s.replaceAll("[^A-Za-z0-9]", "");
    }
}
