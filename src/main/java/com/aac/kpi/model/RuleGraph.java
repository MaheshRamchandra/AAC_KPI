package com.aac.kpi.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes rule-based mappings for each sheet/column without holding real data.
 * This graph is surfaced in the Rules & ML tab to let non-coders view and edit mappings.
 */
public class RuleGraph {
    public List<SheetRule> sheets = new ArrayList<>();
    public List<ScenarioRule> scenarios = new ArrayList<>();

    public static RuleGraph defaults() {
        RuleGraph g = new RuleGraph();
        g.sheets = new ArrayList<>();

        g.sheets.add(SheetRule.of("Patient (Master)",
                ColumnRule.of("patient_id", "text", "uuid32", "random", "", "", "generate UUIDs", "PAT-xxxxx"),
                ColumnRule.of("patient_name", "text", "name", "random", "", "", "random resident name", "Jane Doe"),
                ColumnRule.of("patient_postal_code", "text", "postal-6", "random", "", "", "random 6-digit postal within AAC area", "123456"),
                ColumnRule.of("attended_event_references", "text", "list##alphanumeric", "derived",
                        "Event Session", "composition_id", "collect attended sessions where attended_indicator=true", "EVT123##EVT456")
        ));

        g.sheets.add(SheetRule.of("Event Session",
                ColumnRule.of("composition_id", "text", "uuid32", "random", "", "", "unique event composition id", "EVT-xxxxx"),
                ColumnRule.of("event_session_patient_references1", "text", "list##uuid", "derived",
                        "Patient (Master)", "patient_id", "pick N unique based on number_of_event_sessions and attended_indicator", "PAT-xxxxx##PAT-yyyyy"),
                ColumnRule.of("event_session_start_date1", "date", "yyyy-MM-dd'T'HH:mm:ssXXX", "random", "",
                        "", "random within FY window", "2025-04-15T09:00:00+08:00")
        ));

        g.sheets.add(SheetRule.of("Encounter Master",
                ColumnRule.of("encounter_patient_reference", "text", "uuid", "derived",
                        "Patient (Master)", "patient_id", "link encounter to patient", "PAT-xxxxx"),
                ColumnRule.of("encounter_practitioner_reference", "text", "uuid", "derived",
                        "Practitioner Master", "practitioner_id", "staff who created encounter", "PR-xxxxx")
        ));

        g.sheets.add(SheetRule.of("Practitioner Master",
                ColumnRule.of("practitioner_id", "text", "uuid32", "random", "", "", "unique practitioner id", "PR-xxxxx"),
                ColumnRule.of("practitioner_name", "text", "name", "random", "", "", "random volunteer name", "Alex Lee")
        ));

        g.sheets.add(SheetRule.of("Questionnaire Response",
                ColumnRule.of("questionnaire_patient_reference", "text", "uuid", "derived",
                        "Patient (Master)", "patient_id", "link questionnaire to patient", "PAT-xxxxx"),
                ColumnRule.of("q1", "number", "score(0-5)", "random", "", "", "random score in range", "4")
        ));

        g.sheets.add(SheetRule.of("Common",
                ColumnRule.of("patient_reference", "text", "uuid", "derived",
                        "Patient (Master)", "patient_id", "pass-through for exports", "PAT-xxxxx"),
                ColumnRule.of("event_reference", "text", "uuid", "derived",
                        "Event Session", "composition_id", "pass-through for exports", "EVT-xxxxx")
        ));

        g.sheets.add(SheetRule.of("Common: aac_report",
                ColumnRule.of("S.No", "text", "prefixed-seq", "generated", "", "",
                        "aac_report_<row> synthetic id per AAC center", "aac_report_1"),
                ColumnRule.of("composition_id", "text", "uuid32", "random", "", "",
                        "RandomDataUtil.uuid32() uppercase", "A1B2C3UUID"),
                ColumnRule.of("version_id", "number", "int", "fixed", "", "",
                        "always 1", "1"),
                ColumnRule.of("last_updated", "datetime", "iso+08:00", "timestamp", "", "",
                        "nowIsoOffset(+08:00)", "2025-04-15T10:00:00+08:00"),
                ColumnRule.of("meta_code", "text", "uuid20", "random", "", "",
                        "RandomDataUtil.uuid32().substring(0,20)", "META_UUID20"),
                ColumnRule.of("extension_reporting_month", "text", "yyyy-MM", "derived",
                        "Event Session", "event_session_start_date1",
                        "latest session month per AAC (fallback current month)", "2025-04"),
                ColumnRule.of("extension_total_operating_days", "number", "int", "fixed", "", "",
                        "constant 240 days", "240"),
                ColumnRule.of("extension_total_clients", "number", "int", "derived",
                        "Patient (Master)", "AAC", "count of patients grouped by AAC", "32"),
                ColumnRule.of("status", "text", "code", "fixed", "", "",
                        "final", "final"),
                ColumnRule.of("date", "datetime", "iso+08:00", "timestamp", "", "",
                        "nowIsoOffset(+08:00)", "2025-04-15T10:00:00+08:00"),
                ColumnRule.of("author_value", "text", "aac_id", "derived",
                        "Master Data", "aac_center_id", "AAC center id from MasterDataService", "AAC01"),
                ColumnRule.of("author_display", "text", "aac_name", "derived",
                        "Master Data", "aac_center_name", "AAC center label (Active Ageing Centre <digits>)",
                        "Active Ageing Centre 01"),
                ColumnRule.of("practitioner_references", "text", "list##uuid", "derived",
                        "Practitioner Master", "practitioner_id",
                        "subset of practitioner ids per AAC (1-3, ##-delimited)", "PR001##PR002")
        ));

        g.sheets.add(SheetRule.of("Common: resident_report",
                ColumnRule.of("S. No", "text", "prefixed-seq", "generated", "", "",
                        "resident_report_<row> synthetic id per CommonRow", "resident_report_1"),
                ColumnRule.of("composition_id", "text", "uuid32", "random", "", "",
                        "RandomDataUtil.uuid32() for CommonRow", "UUID32"),
                ColumnRule.of("version_id", "number", "int", "fixed", "", "",
                        "always 1", "1"),
                ColumnRule.of("last_updated", "datetime", "iso+08:00", "derived",
                        "Event Session", "event_session_end_date1",
                        "latest attended session end per patient (fallback now)", "2025-05-01T09:00:00+08:00"),
                ColumnRule.of("meta_code", "text", "uuid", "random", "", "",
                        "CommonRow.metaCode random token", "META1234"),
                ColumnRule.of("extension_reporting_month", "text", "yyyy-MM", "derived",
                        "Event Session", "event_session_start_date1",
                        "month of latest attended session (fallback CommonRow.reportingMonth)", "2025-05"),
                ColumnRule.of("status", "text", "code", "derived",
                        "Common", "status", "CommonRow status (default final)", "final"),
                ColumnRule.of("date", "datetime", "iso+08:00", "derived",
                        "Common", "last_updated", "ISO offset of last_updated", "2025-05-01T09:00:00+08:00"),
                ColumnRule.of("author_value", "text", "aac_id", "derived",
                        "Patient (Master)", "AAC", "patient AAC id", "AAC01"),
                ColumnRule.of("author_display", "text", "aac_label", "derived",
                        "Patient (Master)", "AAC", "Active Ageing Centre label from AAC id", "Active Ageing Centre 01"),
                ColumnRule.of("resident_volunteer_status", "text", "flag", "default", "", "",
                        "CommonRow flag (default TRUE)", "TRUE"),
                ColumnRule.of("cst_date", "date", "yyyy-MM-dd", "default", "", "",
                        "CommonRow cst_date or 2025-12-31", "2025-12-31"),
                ColumnRule.of("cfs", "text", "range-label", "derived",
                        "Patient (Master)", "CFS", "CFS label via CfsUtil (1-3/4-5 etc.)", "1-3"),
                ColumnRule.of("social_risk_factor_score", "text", "label", "derived",
                        "Patient (Master)", "RF", "RF mapped to 1 or >1", ">1"),
                ColumnRule.of("aap_recommendation", "text", "kpi_type", "derived",
                        "Patient (Master)", "KPI Type", "from patient.kpiType", "AAP Robust"),
                ColumnRule.of("social_support_recommendation", "text", "kpi_group", "derived",
                        "Patient (Master)", "KPI Group", "from patient.kpiGroup", "Befriender"),
                ColumnRule.of("aac_opt_out_status", "text", "flag", "default", "", "",
                        "defaults to TRUE if blank", "TRUE"),
                ColumnRule.of("aap_opt_out_status", "text", "flag", "default", "", "",
                        "defaults to TRUE if blank", "TRUE"),
                ColumnRule.of("screening_declaration_date", "date", "yyyy-MM-dd", "default", "", "",
                        "CommonRow value or 2025-12-31", "2025-12-31"),
                ColumnRule.of("befriending_opt_out_status", "text", "flag", "default", "", "",
                        "defaults to TRUE if blank", "TRUE"),
                ColumnRule.of("buddying_opt_out_status", "text", "flag", "default", "", "",
                        "defaults to TRUE if blank", "TRUE"),
                ColumnRule.of("resident_befriending_programme_period_start", "date", "yyyy-MM-dd", "derived",
                        "Event Session", "event_session_start_date1",
                        "earliest attended session or patient befriendingProgramStartDate", "2025-04-01"),
                ColumnRule.of("resident_befriending_programme_period_end", "date", "yyyy-MM-dd", "derived",
                        "Event Session", "event_session_end_date1",
                        "latest attended session or patient befriendingProgramEndDate", "2026-03-31"),
                ColumnRule.of("resident_buddying_programme_period_start", "date", "yyyy-MM-dd", "derived",
                        "Event Session", "event_session_start_date1",
                        "earliest attended session or patient buddyingProgramStartDate", "2025-04-01"),
                ColumnRule.of("resident_buddying_programme_period_end", "date", "yyyy-MM-dd", "derived",
                        "Event Session", "event_session_end_date1",
                        "latest attended session or patient buddyingProgramEndDate", "2026-03-31"),
                ColumnRule.of("irms_referral_raised_date", "date", "yyyy-MM-dd", "derived",
                        "Encounter (Master)", "encounter_start",
                        "earliest finished encounter for patient", "2025-04-01"),
                ColumnRule.of("irms_referral_accepted_date", "date", "yyyy-MM-dd", "derived",
                        "Encounter (Master)", "encounter_start",
                        "latest finished encounter for patient", "2025-04-15"),
                ColumnRule.of("asg_referral_raised_by", "text", "staff_name", "derived",
                        "Encounter (Master)", "encounter_contacted_staff_name",
                        "staff from encounter or default Mr Staff A", "Mr Staff A"),
                ColumnRule.of("asg_referral_accepted_by", "text", "staff_name", "derived",
                        "Encounter (Master)", "encounter_contacted_staff_name",
                        "staff from encounter or default Mr Staff A", "Mr Staff A"),
                ColumnRule.of("patient_reference", "text", "uuid", "derived",
                        "Patient (Master)", "patient_id",
                        "patient_id or patient_identifier_value", "PAT123"),
                ColumnRule.of("encounter_references", "text", "list##uuid", "derived",
                        "Encounter (Master)", "encounter_id",
                        "finished encounters with allowed purposes (##-delimited)", "ENC1##ENC2"),
                ColumnRule.of("questionnaire_reference", "text", "uuid", "derived",
                        "Questionnaire Response", "questionnaire_id",
                        "latest completed questionnaire_id for patient", "QNR123")
        ));

        g.sheets.add(SheetRule.of("Common: volunteer_attendance_report",
                ColumnRule.of("S.No", "text", "prefixed-seq", "generated", "", "",
                        "volunteer_attendance_report_<row>", "volunteer_attendance_report_1"),
                ColumnRule.of("composition_id", "text", "uuid32", "random", "", "",
                        "RandomDataUtil.uuid32() uppercase", "UUID32"),
                ColumnRule.of("version_id", "number", "int", "fixed", "", "",
                        "always 1", "1"),
                ColumnRule.of("last_updated", "datetime", "iso+08:00", "timestamp", "", "",
                        "nowIsoOffset(+08:00)", "2025-04-15T10:00:00+08:00"),
                ColumnRule.of("meta_code", "text", "uuid20", "random", "", "",
                        "RandomDataUtil.uuid32().substring(0,20)", "META_UUID20"),
                ColumnRule.of("extension_reporting_month", "text", "yyyy-MM", "timestamp", "", "",
                        "current month for run", "2025-04"),
                ColumnRule.of("status", "text", "code", "fixed", "", "",
                        "final", "final"),
                ColumnRule.of("date", "datetime", "iso+08:00", "timestamp", "", "",
                        "nowIsoOffset(+08:00)", "2025-04-15T10:00:00+08:00"),
                ColumnRule.of("author_value", "text", "aac_id", "derived",
                        "Master Data", "aac_center_id", "AAC id from center/patient", "AAC01"),
                ColumnRule.of("author_display", "text", "aac_label", "derived",
                        "Master Data", "aac_center_name", "Active Ageing Centre label", "Active Ageing Centre 01"),
                ColumnRule.of("practitioner_references", "text", "list##uuid", "derived",
                        "Practitioner Master", "practitioner_id",
                        "subset of practitioner ids sized by requested count", "PR001##PR002"),
                ColumnRule.of("number_of_practitioners", "number", "int", "derived",
                        "Practitioner Master", "practitioner_id", "count of practitioner_references", "2"),
                ColumnRule.of("volunteered_activity_name_practitionerN", "text", "label", "default", "", "",
                        "one column per requested practitioner; default \"Painting class\"", "Painting class"),
                ColumnRule.of("volunteered_activity_date_practitionerN", "date", "yyyy-MM-dd", "default", "", "",
                        "one column per requested practitioner; default 2024-02-11", "2024-02-11")
        ));

        g.sheets.add(SheetRule.of("Common: event_report",
                ColumnRule.of("S. No", "text", "prefixed-seq", "generated", "", "",
                        "event_report_<row> synthetic id per event session", "event_report_1"),
                ColumnRule.of("composition_id", "text", "uuid32", "derived",
                        "Event Session", "composition_id", "sanitized composition_id", "EVT123"),
                ColumnRule.of("version_id", "number", "int", "fixed", "", "",
                        "always 1", "1"),
                ColumnRule.of("last_updated", "datetime", "iso+08:00", "timestamp", "", "",
                        "nowIsoOffset(+08:00)", "2025-04-15T10:00:00+08:00"),
                ColumnRule.of("meta_code", "text", "uuid20", "random", "", "",
                        "RandomDataUtil.uuid32().substring(0,20)", "META_UUID20"),
                ColumnRule.of("extension_reporting_month", "text", "yyyy-MM", "derived",
                        "Event Session", "event_session_start_date1", "earliest session month for event_id", "2025-04"),
                ColumnRule.of("status", "text", "code", "fixed", "", "",
                        "completed", "completed"),
                ColumnRule.of("date", "datetime", "iso+08:00", "derived",
                        "Event Session", "event_session_start_date1",
                        "earliest start date in ISO offset", "2025-04-10T09:00:00+08:00"),
                ColumnRule.of("author_value", "text", "aac_id", "derived",
                        "Patient (Master)", "AAC", "AAC of first attendee or practitioner fallback", "AAC01"),
                ColumnRule.of("author_display", "text", "aac_label", "derived",
                        "Patient (Master)", "AAC", "Active Ageing Centre label from author_value", "Active Ageing Centre 01"),
                ColumnRule.of("event_id", "text", "alphanumeric", "derived",
                        "Event Session", "event_session_id1", "sanitized event_session_id1", "EVT001"),
                ColumnRule.of("event_name", "text", "label", "derived",
                        "Event Session", "event_session_id1", "event_id without digits or venue fallback", "Community Walk"),
                ColumnRule.of("event_type", "text", "label", "derived",
                        "Patient (Master)", "KPI Type", "majority KPI type of attending patients", "Robust"),
                ColumnRule.of("event_domain", "text", "code", "fixed", "", "",
                        "Community Well-Being", "Community Well-Being"),
                ColumnRule.of("event_target_attendees", "text", "label", "derived",
                        "Patient (Master)", "KPI Type", "AAC <event_type> string", "AAC Robust"),
                ColumnRule.of("event_category", "text", "label", "derived",
                        "Rules", "event_report_label", "derived from event_type or UI override", "Physical"),
                ColumnRule.of("aap_provider", "text", "aac_id", "derived",
                        "Patient (Master)", "AAC", "AAC of first attendee", "AAC01"),
                ColumnRule.of("minimum_required_sessions", "number", "int", "derived",
                        "Patient (Master)", "KPI Type", "Robust=1, Budding=12, Befriending=52", "12"),
                ColumnRule.of("event_is_gui", "text", "flag", "default", "", "",
                        "default FALSE", "FALSE"),
                ColumnRule.of("gui_partner", "text", "label", "default", "", "",
                        "blank partner placeholder", ""),
                ColumnRule.of("number_of_event_sessions", "number", "int", "derived",
                        "Event Session", "event_session_id1", "count sessions per event_id", "3"),
                ColumnRule.of("patient_references", "text", "list##uuid", "derived",
                        "Event Session", "event_session_patient_references1",
                        "unique attended patient ids (##-delimited)", "PAT1##PAT2"),
                ColumnRule.of("total_patient_references", "number", "int", "derived",
                        "Event Session", "event_session_patient_references1",
                        "size of attended patient list", "2"),
                ColumnRule.of("is_attended_session_patient1", "text", "flag", "derived",
                        "Event Session", "event_session_patient_references1",
                        "repeated TRUE/blank columns up to max attendee slots", "TRUE"),
                ColumnRule.of("Working Remarks", "text", "free-text", "user_input", "", "",
                        "left blank, highlighted for manual notes", "")
        ));

        g.sheets.add(SheetRule.of("Common: organization_report",
                ColumnRule.of("S. No", "text", "prefixed-seq", "generated", "", "",
                        "organization_report_<row>", "organization_report_1"),
                ColumnRule.of("id", "text", "organization_id", "derived",
                        "Master Data", "organization_id", "organizationId from master data", "ORG01"),
                ColumnRule.of("version_id", "number", "int", "fixed", "", "",
                        "always 1", "1"),
                ColumnRule.of("last_updated", "datetime", "iso+08:00", "timestamp", "", "",
                        "nowIsoOffset(+08:00)", "2025-04-15T10:00:00+08:00"),
                ColumnRule.of("meta_code", "text", "uuid20", "random", "", "",
                        "RandomDataUtil.uuid32().substring(0,20)", "META_UUID20"),
                ColumnRule.of("start", "date", "yyyy-MM-dd", "derived",
                        "Event Session", "event_session_start_date1",
                        "earliest session start across organization AACs", "2025-04-01"),
                ColumnRule.of("end", "date", "yyyy-MM-dd", "derived",
                        "Event Session", "event_session_end_date1",
                        "latest session end across organization AACs", "2026-03-31"),
                ColumnRule.of("aac_center_ids", "text", "list##aac", "derived",
                        "Master Data", "aac_center_id", "##-joined AAC ids for organization", "AAC01##AAC02"),
                ColumnRule.of("uen", "text", "uen", "random", "", "",
                        "RandomDataUtil.randomUen()", "202512345A"),
                ColumnRule.of("active", "text", "flag", "fixed", "", "",
                        "TRUE", "TRUE"),
                ColumnRule.of("organization_type_code", "text", "code", "derived",
                        "Master Data", "organization_type", "organizationType from master data", "AAC"),
                ColumnRule.of("organization_type_display", "text", "label", "fixed", "", "",
                        "AAC", "AAC"),
                ColumnRule.of("name", "text", "label", "derived",
                        "Master Data", "aac_center_name", "##-joined AAC center names", "Active Ageing Centre 01##02")
        ));

        g.sheets.add(SheetRule.of("Common: location_report",
                ColumnRule.of("S. No", "text", "prefixed-seq", "generated", "", "",
                        "location_report_<row>", "location_report_1"),
                ColumnRule.of("id", "text", "uuid32", "random", "", "",
                        "RandomDataUtil.uuid32() uppercase", "UUID32"),
                ColumnRule.of("version_id", "number", "int", "fixed", "", "",
                        "always 1", "1"),
                ColumnRule.of("last_updated", "datetime", "iso+08:00", "timestamp", "", "",
                        "nowIsoOffset(+08:00)", "2025-04-15T10:00:00+08:00"),
                ColumnRule.of("meta_code", "text", "uuid20", "random", "", "",
                        "RandomDataUtil.uuid32().substring(0,20)", "META_UUID20"),
                ColumnRule.of("start", "date", "yyyy-MM-dd", "fixed", "", "",
                        "FY start 2025-04-01", "2025-04-01"),
                ColumnRule.of("end", "date", "yyyy-MM-dd", "fixed", "", "",
                        "FY end 2026-03-31", "2026-03-31"),
                ColumnRule.of("postal_code", "text", "postal-6", "derived",
                        "Master Data", "location_postal_code", "postal code from master data location", "123456"),
                ColumnRule.of("reference", "text", "url", "derived",
                        "Master Data", "organization_id",
                        "https://pophealth.healthdpx.com/Organisation/<orgId>", "https://pophealth.healthdpx.com/Organisation/ORG01")
        ));

        g.scenarios = List.of(
                new ScenarioRule("Default Scenario", List.of(
                        new ScenarioOverride("Patient (Master)", "patient_birthdate", "fixed", "age=65")
                ))
        );
        return g;
    }

    public static class SheetRule {
        public String name;
        public List<ColumnRule> columns = new ArrayList<>();
        public String description = "";

        public static SheetRule of(String name, ColumnRule... cols) {
            SheetRule s = new SheetRule();
            s.name = name;
            s.columns = new ArrayList<>();
            if (cols != null) {
                for (ColumnRule c : cols) {
                    s.columns.add(c);
                }
            }
            return s;
        }
    }

    public static class ColumnRule {
        public String name;
        public String dataType;
        public String format;
        public String logicType;
        public String sourceSheet;
        public String sourceColumn;
        public String transform;
        public String previewPattern;
        public String delimiter;

        public static ColumnRule of(String name, String dataType, String format, String logicType,
                                    String sourceSheet, String sourceColumn, String transform, String previewPattern) {
            ColumnRule c = new ColumnRule();
            c.name = name;
            c.dataType = dataType;
            c.format = format;
            c.logicType = logicType;
            c.sourceSheet = sourceSheet;
            c.sourceColumn = sourceColumn;
            c.transform = transform;
            c.previewPattern = previewPattern;
            c.delimiter = "";
            return c;
        }
    }

    public static class ScenarioRule {
        public String name;
        public List<ScenarioOverride> overrides = new ArrayList<>();

        public ScenarioRule() {}

        public ScenarioRule(String name, List<ScenarioOverride> overrides) {
            this.name = name;
            this.overrides = overrides == null ? new ArrayList<>() : overrides;
        }
    }

    public static class ScenarioOverride {
        public String sheet;
        public String column;
        public String logicType;
        public String transform;

        public ScenarioOverride() {}

        public ScenarioOverride(String sheet, String column, String logicType, String transform) {
            this.sheet = sheet;
            this.column = column;
            this.logicType = logicType;
            this.transform = transform;
        }

        @Override
        public String toString() {
            return sheet + "." + column + " = " + logicType + " (" + transform + ")";
        }
    }
}
