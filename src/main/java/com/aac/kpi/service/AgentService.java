package com.aac.kpi.service;

import com.aac.kpi.model.CommonRow;
import com.aac.kpi.model.Encounter;
import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.QuestionnaireResponse;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Lightweight, offline agent facade. Uses simple keyword matching over
 * in-memory data to keep the "Assist" tab usable without network or LLMs.
 */
public class AgentService {
    private final ObservableList<Patient> patients;
    private final ObservableList<EventSession> sessions;
    private final ObservableList<Encounter> encounters;
    private final ObservableList<QuestionnaireResponse> questionnaires;
    private final ObservableList<CommonRow> commons;

    public AgentService(ObservableList<Patient> patients,
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

    public Response answer(String prompt) {
        String lower = prompt.toLowerCase(Locale.ENGLISH);
        List<String> trace = new ArrayList<>();

        if (patients == null || patients.isEmpty()) {
            return new Response("No workbook loaded. Please upload Excel first.", "No data available.");
        }

        // Heuristic intents
        if (lower.contains("social risk") || lower.contains("rf")) {
            trace.add("intent: social risk summary");
            String summary = summarizeSocialRisk();
            return new Response(summary, String.join("\n", trace));
        }
        if (lower.contains("cfs")) {
            trace.add("intent: cfs summary");
            String summary = summarizeCfs();
            return new Response(summary, String.join("\n", trace));
        }
        if (lower.contains("event") || lower.contains("session")) {
            trace.add("intent: event/session recap");
            String summary = summarizeEvents();
            return new Response(summary, String.join("\n", trace));
        }
        if (lower.contains("encounter")) {
            trace.add("intent: encounter recap");
            String summary = summarizeEncounters();
            return new Response(summary, String.join("\n", trace));
        }
        if (lower.contains("common")) {
            trace.add("intent: common sheet recap");
            String summary = summarizeCommons();
            return new Response(summary, String.join("\n", trace));
        }

        // Default: quick patient sample
        trace.add("intent: default patient sample");
        String sample = samplePatients();
        return new Response(sample, String.join("\n", trace));
    }

    private String summarizeSocialRisk() {
        long gt1 = patients.stream().filter(p -> p.getSocialRiskFactor() > 1).count();
        long eq1 = patients.stream().filter(p -> p.getSocialRiskFactor() == 1).count();
        long zero = patients.stream().filter(p -> p.getSocialRiskFactor() == 0).count();
        return String.format("Social risk factors: >1 = %d, =1 = %d, 0/blank = %d. Total patients = %d.",
                gt1, eq1, zero, patients.size());
    }

    private String summarizeCfs() {
        long cfs1_3 = patients.stream().filter(p -> p.getCfs() >= 1 && p.getCfs() <= 3).count();
        long cfs4_5 = patients.stream().filter(p -> p.getCfs() >= 4 && p.getCfs() <= 5).count();
        long cfs6_9 = patients.stream().filter(p -> p.getCfs() >= 6 && p.getCfs() <= 9).count();
        return String.format("CFS buckets: 1-3=%d, 4-5=%d, 6-9=%d (total %d).",
                cfs1_3, cfs4_5, cfs6_9, patients.size());
    }

    private String summarizeEvents() {
        if (sessions == null || sessions.isEmpty()) {
            return "No event sessions loaded.";
        }
        int attended = (int) sessions.stream().filter(EventSession::isAttendedIndicator).count();
        String latest = sessions.stream()
                .map(EventSession::getEventSessionStartDate1)
                .filter(s -> s != null && !s.isBlank())
                .max(String::compareTo).orElse("n/a");
        return String.format("Event sessions: %d total, %d marked attended. Latest start: %s.",
                sessions.size(), attended, latest);
    }

    private String summarizeEncounters() {
        if (encounters == null || encounters.isEmpty()) {
            return "No encounters loaded.";
        }
        long finished = encounters.stream()
                .filter(e -> "finished".equalsIgnoreCase(e.getEncounterStatus()))
                .count();
        return String.format("Encounters: %d total, %d finished. Recent: %s.",
                encounters.size(), finished, recentEncounter());
    }

    private String recentEncounter() {
        return encounters.stream()
                .map(Encounter::getEncounterStart)
                .filter(s -> s != null && !s.isBlank())
                .sorted(Comparator.reverseOrder())
                .findFirst().orElse("n/a");
    }

    private String summarizeCommons() {
        if (commons == null || commons.isEmpty()) return "No Common rows loaded.";
        long residents = commons.stream().filter(c -> c.getPatientReference() != null && !c.getPatientReference().isBlank()).count();
        return String.format("Common sheet rows: %d total, %d resident rows. Example reporting_month: %s.",
                commons.size(), residents, commons.get(0).getReportingMonth());
    }

    private String samplePatients() {
        List<String> rows = patients.stream().limit(5)
                .map(p -> String.format("patient_id=%s, CFS=%d, RF=%d, KPI=%s, AAC=%s",
                        nvl(p.getPatientId()), p.getCfs(), p.getSocialRiskFactor(), nvl(p.getKpiType()), nvl(p.getAac())))
                .collect(Collectors.toList());
        return "Sample patients:\n" + String.join("\n", rows);
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    public record Response(String answer, String trace) {}
}
