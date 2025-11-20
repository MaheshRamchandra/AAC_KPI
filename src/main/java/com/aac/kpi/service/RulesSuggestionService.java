package com.aac.kpi.service;

import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.RulesConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight heuristic/ML-style suggestions to help non-coders spot issues in event/contact rows.
 * This does not mutate data; it only proposes changes with confidence hints.
 */
public final class RulesSuggestionService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd[ HH:mm][ HH:mm:ss]");

    private RulesSuggestionService() {}

    public static List<Suggestion> suggest(List<EventSession> sessions,
                                           List<Patient> patients,
                                           RulesConfig config,
                                           LocalDate fyStart,
                                           LocalDate fyEnd) {
        Map<String, Patient> byId = new HashMap<>();
        for (Patient p : patients) {
            if (p.getPatientId() != null) {
                byId.put(p.getPatientId(), p);
            }
        }
        List<Suggestion> suggestions = new ArrayList<>();
        if (sessions == null) return suggestions;
        for (EventSession s : sessions) {
            String mode = nvl(s.getEventSessionMode1());
            String purpose = nvl(s.getPurposeOfContact());
            String start = nvl(s.getEventSessionStartDate1());
            String rawRefs = nvl(s.getEventSessionPatientReferences1());

            Patient firstPatient = null;
            if (!rawRefs.isBlank()) {
                String[] parts = rawRefs.split("##");
                if (parts.length > 0) {
                    firstPatient = byId.get(parts[0].trim());
                }
            }

            if (mode.isBlank()) {
                suggestions.add(new Suggestion("mode",
                        "Missing mode; set to \"" + config.randomDefaults.defaultMode + "\" to align with AAP counting.",
                        0.72, s.getCompositionId()));
            }
            if (purpose.isBlank()) {
                String guess = guessPurpose(firstPatient, config);
                suggestions.add(new Suggestion("purpose",
                        "Missing purpose; likely \"" + guess + "\" based on risk/CFS and defaults.",
                        0.68, s.getCompositionId()));
            } else if (isRecognizedPurpose(purpose, config)) {
                // no-op
            } else {
                suggestions.add(new Suggestion("purpose",
                        "Unrecognized purpose \"" + purpose + "\"; consider mapping to buddying/befriending/screening.",
                        0.41, s.getCompositionId()));
            }

            LocalDate parsed = parseDate(start);
            if (parsed == null) {
                suggestions.add(new Suggestion("date",
                        "Bad or missing start date; set within FY " + fyStart + " to " + fyEnd + ".",
                        0.61, s.getCompositionId()));
            } else if (parsed.isBefore(fyStart) || parsed.isAfter(fyEnd)) {
                suggestions.add(new Suggestion("date",
                        "Date " + parsed + " is outside FY window; adjust into " + fyStart + " to " + fyEnd + ".",
                        0.57, s.getCompositionId()));
            }

            if (!purpose.isBlank() && !mode.isBlank()) {
                if (purpose.equalsIgnoreCase("befriending") && !mode.equalsIgnoreCase("In-person")) {
                    suggestions.add(new Suggestion("mode",
                            "Befriending contacts should be in-person per rules; change mode to In-person.",
                            0.64, s.getCompositionId()));
                }
            }
        }
        return suggestions;
    }

    private static boolean isRecognizedPurpose(String purpose, RulesConfig cfg) {
        String p = nvl(purpose).toLowerCase(Locale.ROOT);
        if (p.isBlank()) return false;
        if (p.contains("buddy")) return true;
        if (p.contains("befriend")) return true;
        if (p.contains("functional") || p.contains("screening")) return true;
        if (cfg != null && cfg.purposes != null) {
            for (RulesConfig.PurposeRule rule : cfg.purposes) {
                if (rule != null && rule.purpose != null
                        && p.contains(rule.purpose.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String guessPurpose(Patient p, RulesConfig cfg) {
        if (p != null) {
            int rf = p.getSocialRiskFactor();
            if (rf > 1) return "befriending";
            if (rf == 1) return "buddying";
        }
        if (cfg != null && cfg.screening != null) {
            return cfg.screening.purpose;
        }
        return "Functional or Health Screening Client Self-Declaration";
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            LocalDateTime dt = LocalDateTime.parse(s, DATE_TIME);
            return dt.toLocalDate();
        } catch (Exception ignored) {
            try {
                OffsetDateTime odt = OffsetDateTime.parse(s);
                return odt.toLocalDate();
            } catch (Exception ignored2) {
                try {
                    return LocalDate.parse(s);
                } catch (Exception ignored3) {
                    return null;
                }
            }
        }
    }

    private static String nvl(String v) {
        return v == null ? "" : v.trim();
    }

    public static final class Suggestion {
        public final String type;
        public final String message;
        public final double confidence;
        public final String compositionId;

        public Suggestion(String type, String message, double confidence, String compositionId) {
            this.type = type;
            this.message = message;
            this.confidence = confidence;
            this.compositionId = compositionId;
        }
    }
}
