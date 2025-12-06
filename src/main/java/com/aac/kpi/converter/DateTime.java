package com.aac.kpi.converter;

import org.apache.poi.ss.usermodel.DateUtil;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTime {

    public static String convertToDate(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // If the value is already an ISO date string, return it as-is.
        try {
            return ZonedDateTime.parse(trimmed).toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception ignored) {
            // Not a ZonedDateTime string; continue.
        }

        // Try parsing an ISO local date string.
        try {
            return java.time.LocalDate.parse(trimmed).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception ignored) {
            // Not an ISO local date; continue.
        }

        // Fall back to Excel numeric date.
        try {
            ZonedDateTime date = ZonedDateTime.of(DateUtil.getLocalDateTime(Double.parseDouble(trimmed)), ZoneId.of("Asia/Singapore"));
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }
}
