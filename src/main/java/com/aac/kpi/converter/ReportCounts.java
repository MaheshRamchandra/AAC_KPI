package com.aac.kpi.converter;

/**
 * Holds the detected number of rows for each report table in the common sheet.
 */
public record ReportCounts(int aac,
                           int resident,
                           int volunteerAttendance,
                           int eventReports,
                           int organization,
                           int location) {
}
