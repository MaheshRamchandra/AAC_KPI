package com.aac.kpi.service;

import com.aac.kpi.model.CommonRow;
import com.aac.kpi.model.Encounter;
import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.QuestionnaireResponse;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Stub/guarded AI overlay service.
 * - Stores API config (url/model/key) but does not call remote LLMs in this skeleton.
 * - Generates proposals based on simple keywords.
 * - applyLastProposal() mutates in-memory lists only when explicit.
 */
public class AiOverlayService {
    private final ObservableList<Patient> patients;
    private final ObservableList<EventSession> sessions;
    private final ObservableList<Encounter> encounters;
    private final ObservableList<QuestionnaireResponse> questionnaires;
    private final ObservableList<CommonRow> commons;

    private Proposal lastProposal = Proposal.empty();
    private String lastAnswer = "";
    private List<Validation> lastValidations = List.of();
    private String lastPlan = "";
    private String lastToolOutputs = "";
    private String apiUrl = "";
    private String model = "";
    private String apiKey = "";
    private final HttpClient http = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    public AiOverlayService(ObservableList<Patient> patients,
                            ObservableList<EventSession> sessions,
                            ObservableList<Encounter> encounters,
                            ObservableList<QuestionnaireResponse> questionnaires,
                            ObservableList<CommonRow> commons) {
        this.patients = patients;
        this.sessions = sessions;
        this.encounters = encounters;
        this.questionnaires = questionnaires;
        this.commons = commons;
    }

    public void setApiConfig(String apiUrl, String model, String apiKey) {
        this.apiUrl = apiUrl == null ? "" : apiUrl.trim();
        this.model = model == null ? "" : model.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public Response propose(String prompt) {
        // Try remote LLM first; if unavailable, fall back to heuristic
        LlmResult llm = callLlm(prompt);
        List<Action> actions = llm.actions().isEmpty() ? heuristicActions(prompt) : llm.actions();
        String answer = llm.answer().isBlank() ? heuristicAnswer(prompt) : llm.answer();
        List<Validation> validations = llm.validations().isEmpty() ? heuristicValidations() : llm.validations();
        String plan = llm.plan().isBlank() ? heuristicPlan(prompt) : llm.plan();
        List<String> toolOuts = executeTools(llm.toolCalls());
        String toolOutputText = String.join("\n", toolOuts);
        lastProposal = new Proposal(actions);
        lastAnswer = answer;
        lastValidations = validations;
        lastPlan = plan;
        lastToolOutputs = toolOutputText;
        return new Response(answer, lastProposal, validations, plan, toolOutputText);
    }

    public Result applyLastProposal() {
        if (lastProposal == null || lastProposal.actions().isEmpty()) {
            return new Result("No proposal to apply.");
        }
        return applyActions(lastProposal.actions());
    }

    public Result applyActions(List<Action> selected) {
        if (selected == null || selected.isEmpty()) {
            return new Result("No actions selected.");
        }
        int changed = 0;
        for (Action action : selected) {
            changed += applyAction(action);
        }
        return new Result("Applied selected actions. Rows touched: " + changed);
    }

    public record Response(String answer, Proposal proposal, List<Validation> validations, String plan, String toolOutputs) {}

    public record Proposal(List<Action> actions) {
        public static Proposal empty() { return new Proposal(List.of()); }
        public String summary() {
            if (actions == null || actions.isEmpty()) return "No actions proposed.";
            return actions.stream()
                    .map(Action::toString)
                    .collect(Collectors.joining("\n"));
        }
    }

    public record Result(String message) {}

    public record Action(String sheet, String column, String value, String condition) {
        @Override
        public String toString() {
            return String.format("Sheet=%s, Column=%s, Value=%s, Condition=%s",
                    sheet, column, value, condition);
        }
    }

    public record Validation(String sheet, String message) {
        @Override
        public String toString() { return (sheet == null ? "" : sheet + ": ") + message; }
    }

    private record LlmResult(List<Action> actions, String answer, List<Validation> validations, String plan,
                             List<ToolCall> toolCalls) {}

    private record ToolCall(String tool, java.util.Map<String,String> params) {}

    private LlmResult callLlm(String prompt) {
        if (apiUrl.isBlank() || model.isBlank()) return new LlmResult(List.of(), "", List.of(), "", List.of());
        try {
            String fullPrompt = """
You are an AI assistant for an Excel generator with sheets: Patient (Master), Event Sessions, Encounter (Master), QuestionnaireResponse (Master), Common.
Return JSON ONLY, shaped as:
{
  "answer": "<short natural language answer or explanation>",
  "actions": [
    { "sheet": "...", "column": "...", "value": "...", "condition": "<blank|always|...>" }
  ],
  "validations": [
    { "sheet": "...", "message": "..."}
  ],
  "plan": "<short bullet list of steps you took>",
  "tool_calls": [
    { "tool": "describe_sheets", "params": {} },
    { "tool": "summarize_sheet", "params": { "sheet": "Patient (Master)" } },
    { "tool": "search_sheet", "params": { "sheet": "Patient (Master)", "query": "RF blank" } },
    { "tool": "describe_rules", "params": {} }
  ]
}
Include actions only for changes; leave empty if none. Do not include any text outside JSON.
User request: %s
""".formatted(prompt);
            String body = GSON.toJson(java.util.Map.of(
                    "model", model,
                    "prompt", fullPrompt,
                    "stream", false
            ));
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (!apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }
            HttpResponse<String> resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                return new LlmResult(List.of(), "LLM call failed (HTTP " + resp.statusCode() + ")", List.of(), "", List.of());
            }
            String txt = resp.body();
            String payload = stripCodeFences(txt);
            try {
                payload = GSON.fromJson(txt, java.util.Map.class).getOrDefault("response", txt).toString();
            } catch (Exception ignored) {}
            payload = stripCodeFences(payload);
            try {
                java.util.Map<String,Object> m = GSON.fromJson(payload, new TypeToken<java.util.Map<String,Object>>(){}.getType());
                String answer = m.getOrDefault("answer", "").toString();
                Object arr = m.get("actions");
                List<Action> actions = new ArrayList<>();
                if (arr != null) {
                    actions = GSON.fromJson(GSON.toJson(arr), new TypeToken<List<Action>>(){}.getType());
                }
                List<Validation> validations = List.of();
                Object vArr = m.get("validations");
                if (vArr != null) {
                    validations = GSON.fromJson(GSON.toJson(vArr), new TypeToken<List<Validation>>(){}.getType());
                }
                String plan = m.getOrDefault("plan", "").toString();
                List<ToolCall> toolCalls = List.of();
                Object toolArr = m.get("tool_calls");
                if (toolArr != null) {
                    toolCalls = GSON.fromJson(GSON.toJson(toolArr), new TypeToken<List<ToolCall>>(){}.getType());
                }
                return new LlmResult(actions == null ? List.of() : actions, answer == null ? "" : answer,
                        validations == null ? List.of() : validations, plan == null ? "" : plan,
                        toolCalls == null ? List.of() : toolCalls);
            } catch (Exception parseEx) {
                // If response isn't JSON, treat it as a plain answer
                return new LlmResult(List.of(), payload, List.of(), "", List.of());
            }
        } catch (Exception ex) {
            return new LlmResult(List.of(), "LLM call failed: " + ex.getMessage(), List.of(), "", List.of());
        }
    }

    private List<Action> heuristicActions(String prompt) {
        String lower = prompt.toLowerCase(Locale.ENGLISH);
        List<Action> actions = new ArrayList<>();
        if (lower.contains("rf") || lower.contains("social risk")) {
            actions.add(new Action("Patient (Master)", "RF", "1", "blank"));
        }
        if (lower.contains("kpi group")) {
            actions.add(new Action("Patient (Master)", "KPI Group", "Befriending", "blank"));
        }
        return actions;
    }

    private String heuristicAnswer(String prompt) {
        if (prompt.toLowerCase(Locale.ENGLISH).contains("rf")) {
            long blanks = patients.stream().filter(p -> p.getSocialRiskFactor() == 0).count();
            return "Detected social risk request. " + blanks + " patients have RF=0/blank. Proposed to set RF to 1 where blank.";
        }
        return "No LLM available; generated a simple proposal. Review actions before applying.";
    }

    private List<Validation> heuristicValidations() {
        List<Validation> list = new ArrayList<>();
        long rfBlank = patients.stream().filter(p -> p.getSocialRiskFactor() == 0).count();
        if (rfBlank > 0) list.add(new Validation("Patient (Master)", rfBlank + " rows have RF blank/0."));
        long cfsBlank = patients.stream().filter(p -> p.getCfs() == 0).count();
        if (cfsBlank > 0) list.add(new Validation("Patient (Master)", cfsBlank + " rows have CFS blank/0."));
        return list;
    }

    private String heuristicPlan(String prompt) {
        return "- Parsed request\n- Suggested simple actions/validations\n- Waiting for user to apply.";
    }

    private String safeStr(String v) { return v == null ? "" : v; }

    private List<String> executeTools(List<ToolCall> toolCalls) {
        List<String> outs = new ArrayList<>();
        if (toolCalls == null) return outs;
        for (ToolCall call : toolCalls) {
            if (call == null || call.tool() == null) continue;
            String t = call.tool().toLowerCase(Locale.ENGLISH);
            java.util.Map<String,String> p = call.params() == null ? java.util.Map.of() : call.params();
            switch (t) {
                case "describe_sheets" -> outs.add("Sheets: " + String.join(", ", sheetNames()));
                case "summarize_sheet" -> outs.add(summarizeSheet(p.getOrDefault("sheet", "")));
                case "search_sheet" -> outs.add(searchSheet(p.getOrDefault("sheet", ""), p.getOrDefault("query", "")));
                case "describe_rules" -> outs.add(describeRules());
                default -> outs.add("Unknown tool: " + call.tool());
            }
        }
        return outs;
    }

    private List<String> sheetNames() {
        return List.of("Patient (Master)", "Event Sessions", "Encounter (Master)", "QuestionnaireResponse (Master)", "Common");
    }

    private String summarizeSheet(String sheet) {
        String s = sheet.toLowerCase(Locale.ENGLISH);
        if (s.contains("patient")) return "Patient rows: " + patients.size();
        if (s.contains("event")) return "Event session rows: " + sessions.size();
        if (s.contains("encounter")) return "Encounter rows: " + encounters.size();
        if (s.contains("question")) return "Questionnaire rows: " + questionnaires.size();
        if (s.contains("common")) return "Common rows: " + commons.size();
        return "Unknown sheet: " + sheet;
    }

    private String searchSheet(String sheet, String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ENGLISH);
        if (q.isBlank()) return "Search query blank.";
        String s = sheet.toLowerCase(Locale.ENGLISH);
        if (s.contains("patient")) {
            long hits = patients.stream().filter(p ->
                    safeStr(p.getPatientId()).toLowerCase(Locale.ENGLISH).contains(q)
                            || safeStr(p.getKpiType()).toLowerCase(Locale.ENGLISH).contains(q)
                            || String.valueOf(p.getSocialRiskFactor()).contains(q)).count();
            return "Patient search hits: " + hits;
        }
        if (s.contains("event")) {
            long hits = sessions.stream().filter(es ->
                    safeStr(es.getEventSessionId1()).toLowerCase(Locale.ENGLISH).contains(q)
                            || safeStr(es.getPurposeOfContact()).toLowerCase(Locale.ENGLISH).contains(q)).count();
            return "Event search hits: " + hits;
        }
        if (s.contains("encounter")) {
            long hits = encounters.stream().filter(e ->
                    safeStr(e.getEncounterPurpose()).toLowerCase(Locale.ENGLISH).contains(q)
                            || safeStr(e.getEncounterStatus()).toLowerCase(Locale.ENGLISH).contains(q)).count();
            return "Encounter search hits: " + hits;
        }
        if (s.contains("question")) {
            long hits = questionnaires.stream().filter(qr ->
                    safeStr(qr.getQuestionnaireStatus()).toLowerCase(Locale.ENGLISH).contains(q)).count();
            return "Questionnaire search hits: " + hits;
        }
        return "Unknown sheet: " + sheet;
    }

    private String describeRules() {
        try {
            com.aac.kpi.model.RuleGraph g = com.aac.kpi.service.RuleGraphService.ensureFile();
            List<String> parts = new ArrayList<>();
            if (g.sheets != null) {
                for (com.aac.kpi.model.RuleGraph.SheetRule s : g.sheets) {
                    String cols = s.columns == null ? "" : s.columns.stream().map(c -> c.name).limit(5).collect(Collectors.joining(", "));
                    parts.add(s.name + " cols: " + cols);
                }
            }
            return "Rules: " + String.join(" | ", parts);
        } catch (Exception ex) {
            return "Rules not available: " + ex.getMessage();
        }
    }

    private String stripCodeFences(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("^```[a-zA-Z0-9]*", "");
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.lastIndexOf("```"));
        }
        return trimmed.trim();
    }

    private int applyAction(Action action) {
        if (action == null || action.sheet() == null) return 0;
        String sheet = action.sheet().toLowerCase(Locale.ENGLISH);
        String column = action.column() == null ? "" : action.column();
        String value = action.value() == null ? "" : action.value();
        String condition = action.condition() == null ? "" : action.condition().toLowerCase(Locale.ENGLISH);
        int changed = 0;
        if (sheet.contains("patient")) {
            for (Patient p : patients) {
                changed += applyPatientAction(p, column, value, condition);
            }
        } else if (sheet.contains("event")) {
            for (EventSession s : sessions) {
                changed += applyEventAction(s, column, value, condition);
            }
        } else if (sheet.contains("encounter")) {
            for (Encounter e : encounters) {
                changed += applyEncounterAction(e, column, value, condition);
            }
        } else if (sheet.contains("questionnaire")) {
            for (QuestionnaireResponse q : questionnaires) {
                changed += applyQuestionnaireAction(q, column, value, condition);
            }
        }
        return changed;
    }

    private boolean shouldApply(String existing, String condition) {
        if (condition.contains("blank")) {
            return existing == null || existing.isBlank() || existing.equals("0");
        }
        return true; // default always
    }

    private int applyPatientAction(Patient p, String column, String value, String condition) {
        String norm = normalizeKey(column);
        if (norm.equals("rf") || norm.contains("socialrisk")) {
            if (shouldApply(String.valueOf(p.getSocialRiskFactor()), condition)) {
                p.setSocialRiskFactor(parseInt(value, p.getSocialRiskFactor()));
                return 1;
            }
        } else if (norm.contains("kpigroup")) {
            if (shouldApply(p.getKpiGroup(), condition)) {
                p.setKpiGroup(value);
                return 1;
            }
        } else if (norm.contains("kpitype")) {
            if (shouldApply(p.getKpiType(), condition)) {
                p.setKpiType(value);
                return 1;
            }
        } else if (norm.contains("aac")) {
            if (shouldApply(p.getAac(), condition)) {
                p.setAac(value);
                return 1;
            }
        } else if (norm.equals("group")) {
            if (shouldApply(String.valueOf(p.getGroup()), condition)) {
                p.setGroup(parseInt(value, p.getGroup()));
                return 1;
            }
        } else if (norm.contains("type")) {
            if (shouldApply(p.getType(), condition)) {
                p.setType(value);
                return 1;
            }
        }
        return 0;
    }

    private int applyEventAction(EventSession s, String column, String value, String condition) {
        String norm = normalizeKey(column);
        if (norm.contains("mode")) {
            if (shouldApply(s.getEventSessionMode1(), condition)) {
                s.setEventSessionMode1(value);
                return 1;
            }
        } else if (norm.contains("purpose")) {
            if (shouldApply(s.getPurposeOfContact(), condition)) {
                s.setPurposeOfContact(value);
                return 1;
            }
        }
        return 0;
    }

    private int applyEncounterAction(Encounter e, String column, String value, String condition) {
        String norm = normalizeKey(column);
        if (norm.contains("status")) {
            if (shouldApply(e.getEncounterStatus(), condition)) {
                e.setEncounterStatus(value);
                return 1;
            }
        } else if (norm.contains("purpose")) {
            if (shouldApply(e.getEncounterPurpose(), condition)) {
                e.setEncounterPurpose(value);
                return 1;
            }
        }
        return 0;
    }

    private int applyQuestionnaireAction(QuestionnaireResponse q, String column, String value, String condition) {
        String norm = normalizeKey(column);
        if (norm.equals("q1")) { if (shouldApply(q.getQ1(), condition)) { q.setQ1(value); return 1; } }
        if (norm.equals("q2")) { if (shouldApply(q.getQ2(), condition)) { q.setQ2(value); return 1; } }
        if (norm.equals("q3")) { if (shouldApply(q.getQ3(), condition)) { q.setQ3(value); return 1; } }
        if (norm.equals("q4")) { if (shouldApply(q.getQ4(), condition)) { q.setQ4(value); return 1; } }
        if (norm.equals("q5")) { if (shouldApply(q.getQ5(), condition)) { q.setQ5(value); return 1; } }
        if (norm.equals("q6")) { if (shouldApply(q.getQ6(), condition)) { q.setQ6(value); return 1; } }
        if (norm.equals("q7")) { if (shouldApply(q.getQ7(), condition)) { q.setQ7(value); return 1; } }
        if (norm.equals("q8")) { if (shouldApply(q.getQ8(), condition)) { q.setQ8(value); return 1; } }
        if (norm.equals("q9")) { if (shouldApply(q.getQ9(), condition)) { q.setQ9(value); return 1; } }
        if (norm.equals("q10")) { if (shouldApply(q.getQ10(), condition)) { q.setQ10(value); return 1; } }
        return 0;
    }

    private int parseInt(String v, int fallback) {
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return fallback; }
    }

    private String normalizeKey(String key) {
        if (key == null) return "";
        return key.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", "");
    }
}
