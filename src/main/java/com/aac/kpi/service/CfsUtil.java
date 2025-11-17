package com.aac.kpi.service;

public final class CfsUtil {
    private CfsUtil() {}

    public static String formatCfs(int cfs) {
        if (cfs <= 0) return "";
        if (cfs <= 3) return "1-3";
        if (cfs >= 7) return "7-8";
        return String.valueOf(cfs);
    }
}
