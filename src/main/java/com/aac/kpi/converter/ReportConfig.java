package com.aac.kpi.converter;

import java.util.Objects;

/**
 * Immutable container for user-supplied input/output locations and report counts.
 */
public record ReportConfig(String inputPath,
                           String outputFolder,
                           int aacReports,
                           int residentReports,
                           int volunteerAttendanceReports,
                           int eventReports,
                           int organizationReports,
                           int locationReports) {

    public ReportConfig {
        inputPath = Objects.requireNonNull(inputPath, "Input Excel path is required").trim();
        outputFolder = Objects.requireNonNull(outputFolder, "Output folder is required").trim();

        if (inputPath.isEmpty()) {
            throw new IllegalArgumentException("Input Excel path cannot be blank");
        }
        if (outputFolder.isEmpty()) {
            throw new IllegalArgumentException("Output folder cannot be blank");
        }

        validateNonNegative(aacReports, "AAC report count");
        validateNonNegative(residentReports, "Resident report count");
        validateNonNegative(volunteerAttendanceReports, "Volunteer attendance report count");
        validateNonNegative(eventReports, "Event report count");
        validateNonNegative(organizationReports, "Organization report count");
        validateNonNegative(locationReports, "Location report count");
    }

    public static ReportConfig fromArgs(String[] args) {
        if (args == null || args.length < 8) {
            throw new IllegalArgumentException("""
                    Expected arguments:
                    <inputExcelPath> <outputFolder> <numAacReports> <numResidentReports> \
<numVolunteerAttendanceReports> <numEventReports> <numOrganizationReports> <numLocationReports>""");
        }

        return new ReportConfig(
                args[0],
                args[1],
                parseCount(args[2], "AAC reports"),
                parseCount(args[3], "Resident reports"),
                parseCount(args[4], "Volunteer attendance reports"),
                parseCount(args[5], "Event reports"),
                parseCount(args[6], "Organization reports"),
                parseCount(args[7], "Location reports")
        );
    }

    private static int parseCount(String raw, String label) {
        try {
            int parsed = Integer.parseInt(raw);
            validateNonNegative(parsed, label);
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a non-negative integer", ex);
        }
    }

    private static void validateNonNegative(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " cannot be negative");
        }
    }
}
