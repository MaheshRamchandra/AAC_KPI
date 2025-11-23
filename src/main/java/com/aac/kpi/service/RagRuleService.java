package com.aac.kpi.service;

import com.aac.kpi.model.CommonRow;
import com.aac.kpi.model.Encounter;
import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.QuestionnaireResponse;
import com.aac.kpi.model.RulesConfig;
import com.aac.kpi.model.RuleGraph;
import com.aac.kpi.model.ScenarioTestCase;
import com.aac.kpi.service.RuleGraphService;
import com.aac.kpi.service.RulesConfigService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Local RAG-style engine that surfaces rule logic, mappings, and previews
 * without requiring network access. Sources:
 * - Rule graph (config/rule-graph.json)
 * - Rules config (thresholds, column specs)
 * - In-memory workbook data (patients, events, etc.)
 * - User edits stored into a lightweight memory file (config/rag-memory.json)
 */
public class RagRuleService {
    private final ObservableList<Patient> patients;
    private final ObservableList<EventSession> sessions;
    private final ObservableList<Encounter> encounters;
    private final ObservableList<QuestionnaireResponse> questionnaires;
    private final ObservableList<CommonRow> commons;
    private final ObservableList<ScenarioTestCase> scenarios;

    private RuleGraph ruleGraph;
    private RulesConfig rulesConfig;
    private final MemoryStore memoryStore = new MemoryStore();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public RagRuleService(ObservableList<Patient> patients,
                          ObservableList<EventSession> sessions,
                          ObservableList<Encounter> encounters,
                          ObservableList<QuestionnaireResponse> questionnaires,
                          ObservableList<CommonRow> commons,
                          ObservableList<ScenarioTestCase> scenarios) {
        this.patients = patients;
        this.sessions = sessions;
        this.encounters = encounters;
        this.questionnaires = questionnaires;
        this.commons = commons;
        this.scenarios = scenarios;
        this.ruleGraph = RuleGraphService.ensureFile();
        this.rulesConfig = RulesConfigService.ensureFile();
        memoryStore.load();
    }

    public List<String> sheetNames() {
        if (ruleGraph == null || ruleGraph.sheets == null) return List.of();
        return ruleGraph.sheets.stream().map(s -> safe(s.name)).toList();
    }

    public List<String> scenarioNames() {
        List<String> names = new ArrayList<>();
        if (ruleGraph != null && ruleGraph.scenarios != null) {
            for (RuleGraph.ScenarioRule s : ruleGraph.scenarios) {
                names.add(s.name);
            }
        }
        if (scenarios != null) {
            for (int i = 0; i < scenarios.size(); i++) {
                ScenarioTestCase tc = scenarios.get(i);
                String label = tc.getTitle() != null && !tc.getTitle().isBlank()
                        ? tc.getTitle()
                        : "Scenario " + (i + 1);
                if (tc.getId() != null && !tc.getId().isBlank()) {
                    label = tc.getId() + " — " + label;
                }
                names.add(label);
            }
        }
        return names.stream().distinct().toList();
    }

    public List<RuleCard> ruleCards(String sheet, String scenarioName) {
        List<RuleCard> out = new ArrayList<>();
        if (ruleGraph == null || ruleGraph.sheets == null) return out;
        RuleGraph.SheetRule found = ruleGraph.sheets.stream()
                .filter(s -> safe(s.name).equals(sheet))
                .findFirst().orElse(null);
        if (found == null || found.columns == null) return out;
        for (RuleGraph.ColumnRule c : found.columns) {
            out.add(toCard(sheet, c, scenarioName));
        }
        return out;
    }

    public RagResult explain(String sheet, String column, String question, String scenarioName) {
        RuleCard card = findCard(sheet, column, scenarioName);
        if (card == null) {
            return new RagResult(null, "No rule found for " + sheet + "." + column, List.of(), List.of(), List.of(), List.of(), List.of());
        }
        List<Knowledge> kb = buildKnowledgeBase(card, scenarioName);
        List<MemoryHit> hits = rank(question, card, kb);
        List<String> previews = preview(card, scenarioName);
        List<String> deps = dependencies(card);
        List<String> validations = validate(card);
        String explanation = buildExplanation(card, hits, previews, deps, scenarioName);
        List<String> plan = List.of(
                "Loaded rule graph + rules config",
                "Retrieved top " + hits.size() + " context blocks from memory/base rules",
                "Applied scenario overrides and column metadata",
                "Generated live preview samples");
        return new RagResult(card, explanation, hits, plan, validations, previews, deps);
    }

    public RuleCard updateRule(RuleEdit edit, String note, String scenarioName) {
        if (edit == null) return null;
        RuleGraph.ColumnRule col = upsertColumn(edit.sheet(), edit.column());
        if (edit.logicType() != null && !edit.logicType().isBlank()) col.logicType = edit.logicType();
        if (edit.format() != null && !edit.format().isBlank()) col.format = edit.format();
        if (edit.dataType() != null && !edit.dataType().isBlank()) col.dataType = edit.dataType();
        if (edit.previewPattern() != null && !edit.previewPattern().isBlank()) col.previewPattern = edit.previewPattern();
        if (edit.sourceSheet() != null) col.sourceSheet = edit.sourceSheet();
        if (edit.sourceColumn() != null) col.sourceColumn = edit.sourceColumn();
        if (edit.transform() != null) col.transform = edit.transform();
        persistGraph();
        String memo = "Updated " + edit.sheet() + "." + edit.column()
                + " -> logic=" + safe(col.logicType) + ", from=" + safe(col.sourceSheet) + "." + safe(col.sourceColumn)
                + ", transform=" + safe(col.transform) + ", pattern=" + safe(col.previewPattern);
        if (note != null && !note.isBlank()) memo += " | note: " + note;
        memoryStore.add(edit.sheet(), edit.column(), memo, scenarioName);
        return toCard(edit.sheet(), col, scenarioName);
    }

    public List<DependencyEdge> dependencyGraph() {
        List<DependencyEdge> edges = new ArrayList<>();
        if (ruleGraph == null || ruleGraph.sheets == null) return edges;
        for (RuleGraph.SheetRule s : ruleGraph.sheets) {
            if (s.columns == null) continue;
            for (RuleGraph.ColumnRule c : s.columns) {
                if (c.sourceSheet != null && !c.sourceSheet.isBlank()) {
                    edges.add(new DependencyEdge(c.sourceSheet, safe(c.sourceColumn), safe(s.name), safe(c.name)));
                }
            }
        }
        return edges;
    }

    public List<String> preview(RuleCard card, String scenarioName) {
        List<String> list = new ArrayList<>();
        if (card == null) return list;
        if (card.previewPattern() != null && !card.previewPattern().isBlank()) {
            list.add("Pattern: " + card.previewPattern());
        }
        list.addAll(sampleValues(card.sheet(), card.column()));
        if (scenarioName != null && !scenarioName.isBlank()) {
            list.addAll(scenarioOverrides(card.sheet(), card.column(), scenarioName).stream()
                    .map(ov -> "Scenario override: " + ov)
                    .toList());
        }
        if (list.isEmpty()) {
            list.add("No sample data yet; load a workbook or enter a pattern.");
        }
        return list;
    }

    public RuleGraph ruleGraph() { return ruleGraph; }

    /**
     * Build a graph JSON for the logic mind-map.
     */
    public String buildGraphJson(boolean includeDependencies) {
        Graph graph = buildGraph(includeDependencies);
        return GSON.toJson(graph);
    }

    public Graph buildGraph(boolean includeDependencies) {
        Graph g = new Graph();
        if (ruleGraph == null || ruleGraph.sheets == null) return g;
        for (RuleGraph.SheetRule s : ruleGraph.sheets) {
            String sheetId = "sheet|" + safe(s.name);
            g.nodes.add(new Graph.Node(sheetId, safe(s.name), "sheet", safe(s.name), "", "", "", "", "", "", ""));
            if (s.columns != null) {
                for (RuleGraph.ColumnRule c : s.columns) {
                    String colId = "col|" + safe(s.name) + "|" + safe(c.name);
                    String ruleId = "rule|" + safe(s.name) + "|" + safe(c.name);
                    String preview = previewForColumn(s.name, c);
                    g.nodes.add(new Graph.Node(colId, safe(c.name), "column", safe(s.name), safe(c.name),
                            safe(c.dataType), safe(c.logicType), safe(c.format),
                            safe(c.sourceSheet), safe(c.sourceColumn), preview));
                    g.nodes.add(new Graph.Node(ruleId,
                            safe(c.logicType) + " " + safe(c.format),
                            "rule", safe(s.name), safe(c.name), safe(c.dataType),
                            safe(c.logicType), safe(c.format), safe(c.sourceSheet), safe(c.sourceColumn), preview));
                    g.edges.add(new Graph.Edge(sheetId, colId, "has column", "contains"));
                    g.edges.add(new Graph.Edge(colId, ruleId, "rule", "rule"));
                    if (includeDependencies && c.sourceSheet != null && !c.sourceSheet.isBlank()) {
                        String srcColId = "col|" + safe(c.sourceSheet) + "|" + safe(c.sourceColumn);
                        g.edges.add(new Graph.Edge(srcColId, colId, "derived from", "dependency"));
                    }
                }
            }
        }
        if (includeDependencies) {
            for (DependencyEdge d : dependencyGraph()) {
                String from = "sheet|" + safe(d.fromSheet());
                String to = "sheet|" + safe(d.toSheet());
                g.edges.add(new Graph.Edge(from, to, safe(d.fromColumn()) + " → " + safe(d.toColumn()), "sheet-dependency"));
            }
        }
        return g;
    }

    private String previewForColumn(String sheet, RuleGraph.ColumnRule c) {
        RuleCard card = new RuleCard(sheet, safe(c.name), safe(c.dataType), safe(c.format),
                safe(c.logicType), safe(c.previewPattern), safe(c.sourceSheet),
                safe(c.sourceColumn), safe(c.transform), List.of(), List.of(), List.of());
        List<String> vals = sampleValues(sheet, c.name);
        if (!vals.isEmpty()) return vals.get(0);
        if (c.previewPattern != null && !c.previewPattern.isBlank()) return c.previewPattern;
        return "";
    }

    private RuleCard findCard(String sheet, String column, String scenarioName) {
        if (ruleGraph == null || ruleGraph.sheets == null) return null;
        RuleGraph.SheetRule s = ruleGraph.sheets.stream()
                .filter(sh -> safe(sh.name).equals(sheet))
                .findFirst().orElse(null);
        if (s == null || s.columns == null) return null;
        for (RuleGraph.ColumnRule c : s.columns) {
            if (safe(c.name).equals(column)) {
                return toCard(sheet, c, scenarioName);
            }
        }
        return null;
    }

    private RuleGraph.ColumnRule upsertColumn(String sheet, String column) {
        if (ruleGraph == null) ruleGraph = RuleGraph.defaults();
        if (ruleGraph.sheets == null) ruleGraph.sheets = new ArrayList<>();
        RuleGraph.SheetRule s = ruleGraph.sheets.stream()
                .filter(sh -> safe(sh.name).equals(sheet))
                .findFirst().orElse(null);
        if (s == null) {
            s = RuleGraph.SheetRule.of(sheet);
            ruleGraph.sheets.add(s);
        }
        if (s.columns == null) s.columns = new ArrayList<>();
        RuleGraph.ColumnRule col = s.columns.stream()
                .filter(c -> safe(c.name).equals(column))
                .findFirst().orElse(null);
        if (col == null) {
            col = RuleGraph.ColumnRule.of(column, "text", "", "derived", "", "", "", "");
            s.columns.add(col);
        }
        return col;
    }

    private RuleCard toCard(String sheet, RuleGraph.ColumnRule c, String scenarioName) {
        RuleGraph.ColumnRule clone = copy(c);
        // Apply scenario overrides from rule graph
        if (ruleGraph != null && ruleGraph.scenarios != null && scenarioName != null) {
            for (RuleGraph.ScenarioRule s : ruleGraph.scenarios) {
                if (scenarioName.equals(s.name) && s.overrides != null) {
                    for (RuleGraph.ScenarioOverride ov : s.overrides) {
                        if (sheet.equals(ov.sheet) && safe(ov.column).equals(c.name)) {
                            if (ov.logicType != null) clone.logicType = ov.logicType;
                            if (ov.transform != null) clone.transform = ov.transform;
                        }
                    }
                }
            }
        }
        return new RuleCard(sheet, safe(clone.name), safe(clone.dataType), safe(clone.format),
                safe(clone.logicType), safe(clone.previewPattern), safe(clone.sourceSheet),
                safe(clone.sourceColumn), safe(clone.transform),
                dependencies(new RuleCard(sheet, safe(clone.name), safe(clone.dataType), safe(clone.format),
                        safe(clone.logicType), safe(clone.previewPattern), safe(clone.sourceSheet),
                        safe(clone.sourceColumn), safe(clone.transform), List.of(), List.of(), List.of())),
                allowedOverrides(sheet, clone.name, scenarioName),
                preview(new RuleCard(sheet, safe(clone.name), safe(clone.dataType), safe(clone.format),
                        safe(clone.logicType), safe(clone.previewPattern), safe(clone.sourceSheet),
                        safe(clone.sourceColumn), safe(clone.transform), List.of(), List.of(), List.of()), scenarioName));
    }

    private RuleGraph.ColumnRule copy(RuleGraph.ColumnRule c) {
        RuleGraph.ColumnRule clone = new RuleGraph.ColumnRule();
        clone.name = c.name;
        clone.dataType = c.dataType;
        clone.format = c.format;
        clone.logicType = c.logicType;
        clone.sourceSheet = c.sourceSheet;
        clone.sourceColumn = c.sourceColumn;
        clone.transform = c.transform;
        clone.previewPattern = c.previewPattern;
        clone.delimiter = c.delimiter;
        return clone;
    }

    private List<String> dependencies(RuleCard card) {
        List<String> deps = new ArrayList<>();
        if (card == null) return deps;
        if (!card.sourceSheet().isBlank()) {
            deps.add("Depends on " + card.sourceSheet() + "." + safe(card.sourceColumn()));
        }
        // Reverse lookup
        if (ruleGraph != null && ruleGraph.sheets != null) {
            for (RuleGraph.SheetRule s : ruleGraph.sheets) {
                if (s.columns == null) continue;
                for (RuleGraph.ColumnRule c : s.columns) {
                    if (safe(c.sourceSheet).equals(card.sheet()) && safe(c.sourceColumn).equals(card.column())) {
                        deps.add("Feeds " + safe(s.name) + "." + safe(c.name));
                    }
                }
            }
        }
        if (card.column().toLowerCase(Locale.ENGLISH).contains("patient_reference")) {
            deps.add("KPI link: patient_reference must map to Patient Master patient_id.");
        }
        if (card.column().toLowerCase(Locale.ENGLISH).contains("encounter")) {
            deps.add("Encounter purpose pulled from Encounter Master / input sheet.");
        }
        return deps;
    }

    private List<String> allowedOverrides(String sheet, String column, String scenarioName) {
        List<String> list = new ArrayList<>();
        list.add("Override logic type, mapping source, or pattern.");
        list.addAll(scenarioOverrides(sheet, column, scenarioName));
        return list;
    }

    private List<String> scenarioOverrides(String sheet, String column, String scenarioName) {
        List<String> overrides = new ArrayList<>();
        if (scenarioName == null) return overrides;
        // From rule graph
        if (ruleGraph != null && ruleGraph.scenarios != null) {
            for (RuleGraph.ScenarioRule s : ruleGraph.scenarios) {
                if (scenarioName.equals(s.name) && s.overrides != null) {
                    for (RuleGraph.ScenarioOverride ov : s.overrides) {
                        if (sheet.equals(ov.sheet) && safe(ov.column).equals(column)) {
                            overrides.add("RuleGraph: " + ov.logicType + " / " + ov.transform);
                        }
                    }
                }
            }
        }
        // From scenario test cases
        if (scenarios != null) {
            for (ScenarioTestCase tc : scenarios) {
                String tcName = tc.getTitle() != null && !tc.getTitle().isBlank() ? tc.getTitle() : tc.getId();
                if (!scenarioName.equals(tcName) && !scenarioName.equals(tc.getId())) continue;
                if (tc.getColumnOverrides() != null) {
                    for (ScenarioTestCase.ColumnOverride ov : tc.getColumnOverrides()) {
                        if (sheet.equals(ov.getSheet()) && safe(ov.getColumn()).equals(column)) {
                            overrides.add("Scenario TC override -> " + ov.getValue());
                        }
                    }
                }
            }
        }
        return overrides;
    }

    private List<Knowledge> buildKnowledgeBase(RuleCard card, String scenarioName) {
        List<Knowledge> list = new ArrayList<>();
        // Rule graph columns
        if (ruleGraph != null && ruleGraph.sheets != null) {
            for (RuleGraph.SheetRule s : ruleGraph.sheets) {
                if (s.columns == null) continue;
                for (RuleGraph.ColumnRule c : s.columns) {
                    String text = "%s.%s type=%s format=%s logic=%s src=%s.%s transform=%s preview=%s"
                            .formatted(safe(s.name), safe(c.name), safe(c.dataType), safe(c.format),
                                    safe(c.logicType), safe(c.sourceSheet), safe(c.sourceColumn),
                                    safe(c.transform), safe(c.previewPattern));
                    list.add(new Knowledge(s.name, c.name, text));
                }
            }
        }
        // Rules config column specs
        if (rulesConfig != null && rulesConfig.columns != null) {
            for (RulesConfig.ColumnSpec col : rulesConfig.columns) {
                String text = "Column spec %s.%s required=%s default=%s notes=%s"
                        .formatted(safe(col.sheet), safe(col.column), safe(col.required),
                                safe(col.defaultValue), safe(col.notes));
                list.add(new Knowledge(col.sheet, col.column, text));
            }
        }
        // Backend logic hints
        for (String logic : backendLogic()) {
            list.add(new Knowledge(card != null ? card.sheet() : "", card != null ? card.column() : "", logic));
        }
        // Scenario overrides
        list.addAll(scenarioOverridesAsKnowledge(scenarioName));
        // Memory store
        for (MemoryEntry e : memoryStore.entries()) {
            list.add(new Knowledge(e.sheet, e.column, e.text + " (user memory @ " + e.timestamp + ")"));
        }
        // Live metadata from data
        list.add(new Knowledge(card != null ? card.sheet() : "", card != null ? card.column() : "",
                "Live preview: " + String.join(", ", preview(card, scenarioName))));
        return list;
    }

    private List<Knowledge> scenarioOverridesAsKnowledge(String scenarioName) {
        List<Knowledge> list = new ArrayList<>();
        if (scenarioName == null) return list;
        if (ruleGraph != null && ruleGraph.scenarios != null) {
            for (RuleGraph.ScenarioRule s : ruleGraph.scenarios) {
                if (!scenarioName.equals(s.name)) continue;
                if (s.overrides != null) {
                    for (RuleGraph.ScenarioOverride ov : s.overrides) {
                        String text = "Scenario override %s.%s logic=%s transform=%s"
                                .formatted(ov.sheet, ov.column, ov.logicType, ov.transform);
                        list.add(new Knowledge(ov.sheet, ov.column, text));
                    }
                }
            }
        }
        if (scenarios != null) {
            for (ScenarioTestCase tc : scenarios) {
                String tcName = tc.getTitle() != null && !tc.getTitle().isBlank() ? tc.getTitle() : tc.getId();
                if (!scenarioName.equals(tcName) && !scenarioName.equals(tc.getId())) continue;
                if (tc.getColumnOverrides() != null) {
                    for (ScenarioTestCase.ColumnOverride ov : tc.getColumnOverrides()) {
                        String text = "Test case override %s.%s=%s".formatted(ov.getSheet(), ov.getColumn(), ov.getValue());
                        list.add(new Knowledge(ov.getSheet(), ov.getColumn(), text));
                    }
                }
            }
        }
        return list;
    }

    private List<String> backendLogic() {
        return List.of(
                "Age must be >= 60 for KPI counting; derive from patient_birthdate if provided.",
                "Befriending/Buddying sessions per patient drive KPI group.",
                "Encounter purpose pulled from input sheet and Event Session purpose.",
                "Within / Outside boundary computed from postal code linkage.",
                "Practitioner references pulled from Practitioner Master.",
                "Event session patient count = number of attendance in person.",
                "Questionnaire_response patient_reference must mirror Encounter patient_reference."
        );
    }

    private List<MemoryHit> rank(String question, RuleCard card, List<Knowledge> kb) {
        String query = (safe(card.sheet()) + " " + safe(card.column()) + " " + safe(question)).trim();
        Set<String> qTokens = tokens(query);
        List<MemoryHit> hits = new ArrayList<>();
        for (Knowledge k : kb) {
            double score = similarity(qTokens, tokens(k.text));
            if (safe(k.sheet).equals(card.sheet())) score += 0.2;
            if (safe(k.column).equals(card.column())) score += 0.3;
            if (score > 0) {
                hits.add(new MemoryHit(k.text, score));
            }
        }
        hits.sort(Comparator.comparingDouble((MemoryHit h) -> h.score).reversed());
        return hits.stream().limit(6).toList();
    }

    private double similarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) inter.size() / (double) union.size();
    }

    private Set<String> tokens(String text) {
        Set<String> set = new HashSet<>();
        if (text == null) return set;
        for (String t : text.toLowerCase(Locale.ENGLISH).split("[^a-z0-9]+")) {
            if (!t.isBlank()) set.add(t);
        }
        return set;
    }

    private String buildExplanation(RuleCard card, List<MemoryHit> hits, List<String> previews,
                                    List<String> deps, String scenarioName) {
        StringBuilder sb = new StringBuilder();
        sb.append(card.sheet()).append(".").append(card.column())
                .append(" uses ").append(card.logicType()).append(" logic (").append(card.dataType())
                .append(" / ").append(card.format()).append(")");
        if (!card.sourceSheet().isBlank()) {
            sb.append(" sourced from ").append(card.sourceSheet()).append(".").append(card.sourceColumn());
        }
        if (!card.transform().isBlank()) {
            sb.append(" with transform: ").append(card.transform());
        }
        if (scenarioName != null && !scenarioName.isBlank()) {
            sb.append(" | scenario: ").append(scenarioName);
        }
        if (!previews.isEmpty()) {
            sb.append("\nPreview: ").append(String.join(", ", previews.stream().limit(3).toList()));
        }
        if (!deps.isEmpty()) {
            sb.append("\nDependencies: ").append(String.join(" | ", deps));
        }
        if (!hits.isEmpty()) {
            sb.append("\nContext: ").append(String.join(" | ",
                    hits.stream().map(h -> trim(h.text(), 160)).toList()));
        }
        return sb.toString();
    }

    private List<String> validate(RuleCard card) {
        List<String> vals = new ArrayList<>();
        if (card.logicType().equalsIgnoreCase("derived") && card.sourceSheet().isBlank()) {
            vals.add("Derived logic without source mapping; set From Sheet/Column.");
        }
        if (card.dataType().equalsIgnoreCase("date") && card.format().isBlank()) {
            vals.add("Date format missing; e.g., yyyy-MM-dd or ISO+08:00.");
        }
        return vals;
    }

    private List<String> sampleValues(String sheet, String column) {
        List<String> list = new ArrayList<>();
        String norm = normalize(column);
        String sheetNorm = sheet.toLowerCase(Locale.ENGLISH);
        if (sheetNorm.contains("patient") && patients != null) {
            for (Patient p : patients.stream().limit(4).toList()) {
                switch (norm) {
                    case "patient_id" -> list.add(p.getPatientId());
                    case "patient_postal_code" -> list.add(p.getPatientPostalCode());
                    case "attended_event_references" -> list.add(p.getAttendedEventReferences());
                    case "cfs" -> list.add(String.valueOf(p.getCfs()));
                    case "socialriskfactor", "rf" -> list.add(String.valueOf(p.getSocialRiskFactor()));
                    case "kpigroup" -> list.add(p.getKpiGroup());
                    case "kpitype" -> list.add(p.getKpiType());
                    default -> {}
                }
            }
        } else if (sheetNorm.contains("event") && sessions != null) {
            for (EventSession s : sessions.stream().limit(4).toList()) {
                switch (norm) {
                    case "event_session_patient_references1" -> list.add(s.getEventSessionPatientReferences1());
                    case "purposeofcontact", "purpose" -> list.add(s.getPurposeOfContact());
                    case "event_session_start_date1" -> list.add(s.getEventSessionStartDate1());
                    case "event_session_mode1" -> list.add(s.getEventSessionMode1());
                    default -> {}
                }
            }
        } else if (sheetNorm.contains("encounter") && encounters != null) {
            for (Encounter e : encounters.stream().limit(4).toList()) {
                switch (norm) {
                    case "encounter_purpose" -> list.add(e.getEncounterPurpose());
                    case "encounter_status" -> list.add(e.getEncounterStatus());
                    case "encounter_patient_reference" -> list.add(e.getEncounterPatientReference());
                    case "encounter_practitioner_reference" -> list.add(e.getEncounterContactedStaffName());
                    default -> {}
                }
            }
        } else if (sheetNorm.contains("questionnaire") && questionnaires != null) {
            for (QuestionnaireResponse q : questionnaires.stream().limit(4).toList()) {
                switch (norm) {
                    case "questionnaire_patient_reference" -> list.add(q.getQuestionnairePatientReference());
                    case "q1" -> list.add(q.getQ1());
                    case "q2" -> list.add(q.getQ2());
                    case "q3" -> list.add(q.getQ3());
                    case "q4" -> list.add(q.getQ4());
                    case "q5" -> list.add(q.getQ5());
                    case "q6" -> list.add(q.getQ6());
                    case "q7" -> list.add(q.getQ7());
                    case "q8" -> list.add(q.getQ8());
                    case "q9" -> list.add(q.getQ9());
                    case "q10" -> list.add(q.getQ10());
                    default -> {}
                }
            }
        } else if (sheetNorm.contains("common") && commons != null) {
            for (CommonRow c : commons.stream().limit(4).toList()) {
                switch (norm) {
                    case "patient_reference" -> list.add(c.getPatientReference());
                    case "event_reference", "attended_event_references" -> list.add(c.getAttendedEventReferences());
                    case "encounter_reference", "encounter_references" -> list.add(c.getEncounterReferences());
                    case "reporting_month", "extension_reporting_month" -> list.add(c.getReportingMonth());
                    case "questionnaire_reference" -> list.add(c.getQuestionnaireReference());
                    default -> {}
                }
            }
        }
        list.removeIf(Objects::isNull);
        return list;
    }

    private void persistGraph() {
        try {
            RuleGraphService.save(ruleGraph);
        } catch (Exception ignored) {}
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private String normalize(String v) {
        return safe(v).toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", "_");
    }

    private String trim(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }

    public record RuleCard(String sheet, String column, String dataType, String format, String logicType,
                           String previewPattern, String sourceSheet, String sourceColumn, String transform,
                           List<String> dependencies, List<String> allowedOverrides, List<String> livePreview) {}

    public record RagResult(RuleCard card, String explanation, List<MemoryHit> hits, List<String> plan,
                            List<String> validations, List<String> previews, List<String> dependencies) {}

    public record MemoryHit(String text, double score) {}

    public record DependencyEdge(String fromSheet, String fromColumn, String toSheet, String toColumn) {}

    public record RuleEdit(String sheet, String column, String sourceSheet, String sourceColumn,
                           String transform, String logicType, String format, String dataType,
                           String previewPattern) {}

    private record Knowledge(String sheet, String column, String text) {}

    public static final class Graph {
        public final java.util.List<Node> nodes = new java.util.ArrayList<>();
        public final java.util.List<Edge> edges = new java.util.ArrayList<>();
        public record Node(String id, String label, String type, String sheet, String column,
                           String dataType, String logicType, String format,
                           String sourceSheet, String sourceColumn, String preview) {}
        public record Edge(String from, String to, String label, String kind) {}
    }

    private static final class MemoryEntry {
        String id;
        String sheet;
        String column;
        String text;
        String scenario;
        LocalDateTime timestamp;
    }

    private static final class MemoryStore {
        private final File file = new File("config/rag-memory.json");
        private final List<MemoryEntry> entries = new ArrayList<>();

        void load() {
            entries.clear();
            if (!file.exists()) return;
            try (FileReader r = new FileReader(file)) {
                List<MemoryEntry> list = GSON.fromJson(r, new TypeToken<List<MemoryEntry>>() {}.getType());
                if (list != null) entries.addAll(list);
            } catch (Exception ignored) {}
        }

        void add(String sheet, String column, String text, String scenario) {
            MemoryEntry e = new MemoryEntry();
            e.id = UUID.randomUUID().toString();
            e.sheet = sheet;
            e.column = column;
            e.text = text;
            e.scenario = scenario;
            e.timestamp = LocalDateTime.now();
            entries.add(0, e);
            save();
        }

        void save() {
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                try (FileWriter w = new FileWriter(file)) {
                    GSON.toJson(entries, w);
                }
            } catch (Exception ignored) {}
        }

        List<MemoryEntry> entries() {
            return entries;
        }
    }
}
