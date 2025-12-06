package com.aac.kpi.converter;

/**
 * Shared constants describing sheet positions and type names used when parsing
 * the workbook and building reports.
 */
public final class ReportConstants {
    private ReportConstants() {
    }

    public static final int START_ROW = 1;
    public static final int FILLER_ROWS = 2;

    public static final int EVENT_SESSIONS_SHEET_NO = 1;
    public static final int EVENT_SESSIONS_NRIC_SHEET_NO = 2;
    public static final int PATIENT_SHEET_NO = 3;
    public static final int PRACTITIONER_SHEET_NO = 4;
    public static final int ENCOUNTER_SHEET_NO = 5;
    public static final int QUESTIONNAIRE_SHEET_NO = 6;

    public static final String AAC_TYPE = "AAC";
    public static final String RESIDENT_TYPE = "RESIDENT";
    public static final String VOLUNTEER_ATTENDANCE_TYPE = "VOLUNTEER ATTENDANCE";
    public static final String EVENT_TYPE = "EVENT";
    public static final String ORGANIZATION_TYPE = "ORGANIZATION";
    public static final String LOCATION_TYPE = "LOCATION";

    public static final String EVENT_SESSIONS_TYPE = "EVENT SESSIONS";
    public static final String EVENT_SESSIONS_NRIC_TYPE = "EVENT SESSIONS NRIC";
    public static final String PATIENT_TYPE = "PATIENT";
    public static final String PRACTITIONER_TYPE = "PRACTITIONER";
    public static final String ENCOUNTER_TYPE = "ENCOUNTER";
    public static final String QUESTIONNAIRE_TYPE = "QUESTIONNAIRE";
}
