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
