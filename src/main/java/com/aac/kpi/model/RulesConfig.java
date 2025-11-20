package com.aac.kpi.model;

import com.aac.kpi.service.KpiConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * User-editable rules/config definition for KPI thresholds, purposes, column schema, and
 * random generation defaults. This is intentionally additive and does not replace the
 * built-in rule engine unless explicitly applied for preview/export.
 */
public class RulesConfig {
    public Meta meta = new Meta();
    public Thresholds thresholds = new Thresholds();
    public ScreeningRule screening = new ScreeningRule();
    public List<PurposeRule> purposes = new ArrayList<>();
    public List<ColumnSpec> columns = new ArrayList<>();
    public RandomDefaults randomDefaults = new RandomDefaults();
    public boolean applyToGeneration = false;

    public static RulesConfig defaults() {
        RulesConfig cfg = new RulesConfig();
        cfg.meta = new Meta();
        cfg.thresholds = new Thresholds();
        cfg.screening = new ScreeningRule();
        cfg.randomDefaults = new RandomDefaults();

        cfg.purposes = new ArrayList<>();
        cfg.purposes.add(new PurposeRule("Robust AAP", "physical-activity", "In-person", 2,
                "Count AAP attendances for CFS 1-3 and CFS 6-9 (grouped separately)."));
        cfg.purposes.add(new PurposeRule("Frail AAP", "physical-activity", "In-person", 6,
                "Count AAP attendances for CFS 4-5 and CFS 6-9 (very frail group)."));
        cfg.purposes.add(new PurposeRule("Buddying", "buddying", "In-person", 12,
                "Buddying contacts; AAP minimum of 6 in-person sessions."));
        cfg.purposes.add(new PurposeRule("Befriending", "befriending", "In-person", 52,
                "Befriending contacts; AAP minimum of 12 in-person sessions."));

        cfg.columns = new ArrayList<>();
        cfg.columns.add(new ColumnSpec("Patient (Master)", "CFS", "", "", "yes", "", "CFS range drives KPI bucket."));
        cfg.columns.add(new ColumnSpec("Patient (Master)", "RF", "", "", "yes", "", "Risk factor count; 1 = Buddying, >1 = Befriending."));
        cfg.columns.add(new ColumnSpec("Event Session", "purpose_of_contact", "", "", "yes", "", "Purpose drives KPI contact counts."));
        cfg.columns.add(new ColumnSpec("Event Session", "event_session_mode1", "", "", "yes", "In-person", "Mode filter for AAP/Buddying/Befriending."));
        cfg.columns.add(new ColumnSpec("Event Session", "event_session_start_date1", "", "", "yes", "", "FY window 01 Apr–31 Mar check."));
        cfg.columns.add(new ColumnSpec("Patient (Master)", "attended_event_references", "Event Session", "composition_id", "yes", "", "List of event IDs this patient attended."));
        cfg.columns.add(new ColumnSpec("Event Session", "event_session_patient_references1", "Patient (Master)", "patient_id", "yes", "", "Attendee list referencing patients (## delimited)."));

        return cfg;
    }

    public KpiConfig toKpiConfig() {
        KpiConfig c = new KpiConfig();
        c.robustMinInPerson = thresholds.robustMinInPerson;
        c.frailMinInPerson = thresholds.frailMinInPerson;
        c.buddyingMinInPerson = thresholds.buddyingMinInPerson;
        c.befriendingMinInPerson = thresholds.befriendingMinInPerson;
        return c;
    }

    public static final class Meta {
        public String version = "1.0";
        public String notes = "Editable UI configuration for KPI preview and generation defaults.";
    }

    public static final class Thresholds {
        public int robustMinInPerson = 2;
        public int frailMinInPerson = 6;
        public int buddyingMinInPerson = 6;
        public int befriendingMinInPerson = 12;
        public int befriendingMinContacts = 52;
        public String dateWindow = "01 Apr – 31 Mar (FY)";
    }

    public static final class ScreeningRule {
        public String purpose = "Functional or Health Screening Client Self-Declaration";
        public String window = "01 Apr – 31 Mar (FY)";
        public boolean requireInPersonAAP = true;
    }

    public static final class PurposeRule {
        public String label;
        public String purpose;
        public String mode;
        public int minCount;
        public String description;

        public PurposeRule() {}

        public PurposeRule(String label, String purpose, String mode, int minCount, String description) {
            this.label = label;
            this.purpose = purpose;
            this.mode = mode;
            this.minCount = minCount;
            this.description = description;
        }
    }

    public static final class ColumnSpec {
        public String sheet;
        public String column;
        /**
         * Optional source sheet/column that this value is linked from (e.g., Event Session.patient_references comes from Patient Master.patient_id).
         */
        public String sourceSheet;
        public String sourceColumn;
        public String required;
        public String defaultValue;
        public String notes;

        public ColumnSpec() {}

        public ColumnSpec(String sheet, String column, String required, String defaultValue, String notes) {
            this(sheet, column, "", "", required, defaultValue, notes);
        }

        public ColumnSpec(String sheet, String column, String sourceSheet, String sourceColumn,
                          String required, String defaultValue, String notes) {
            this.sheet = sheet;
            this.column = column;
            this.sourceSheet = sourceSheet;
            this.sourceColumn = sourceColumn;
            this.required = required;
            this.defaultValue = defaultValue;
            this.notes = notes;
        }
    }

    public static final class RandomDefaults {
        public String defaultMode = "In-person";
        public int defaultAAPSessions = 6;
        public int defaultBuddyingContacts = 12;
        public int defaultBefriendingContacts = 52;
        public String dateWindow = "FY 01 Apr – 31 Mar";
    }
}
