package com.aac.kpi.service;

import com.aac.kpi.model.CommonRow;
import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.Encounter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-only validation helpers for basic completeness and reference integrity.
 * Does not modify data or generation rules.
 */
public final class ValidationService {
    private ValidationService() {}

    public static Summary summarize(List<Patient> patients,
                                    List<EventSession> sessions,
                                    List<Encounter> encounters,
                                    List<CommonRow> commons) {
        int missingPatients = (int) patients.stream().filter(p -> isBlank(p.getPatientId())).count();
        Map<String, Long> patientCounts = patients.stream()
                .map(Patient::getPatientId)
                .filter(id -> !isBlank(id))
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        int duplicatePatients = (int) patientCounts.values().stream().filter(c -> c > 1).count();
        Set<String> patientIds = new HashSet<>(patientCounts.keySet());

        int missingSessions = (int) sessions.stream().filter(s -> isBlank(s.getCompositionId())).count();
        int missingSessionDates = (int) sessions.stream()
                .filter(s -> isBlank(s.getEventSessionStartDate1()) || isBlank(s.getEventSessionEndDate1()))
                .count();
        Set<String> sessionIds = sessions.stream()
                .map(EventSession::getCompositionId)
                .filter(id -> !isBlank(id))
                .collect(Collectors.toSet());

        int missingCommonPatient = (int) commons.stream().filter(c -> isBlank(c.getPatientReference())).count();
        int missingCommonEvents = (int) commons.stream().filter(c -> isBlank(c.getAttendedEventReferences())).count();

        int invalidCommonPatientRefs = 0;
        int invalidCommonEventRefs = 0;
        for (CommonRow row : commons) {
            if (!isBlank(row.getPatientReference()) && !patientIds.contains(row.getPatientReference())) {
                invalidCommonPatientRefs++;
            }
            if (!isBlank(row.getAttendedEventReferences())) {
                boolean bad = false;
                for (String token : splitRefs(row.getAttendedEventReferences())) {
                    if (!token.isBlank() && !sessionIds.contains(token)) {
                        bad = true;
                        break;
                    }
                }
                if (bad) invalidCommonEventRefs++;
            }
        }

        return new Summary(missingPatients, duplicatePatients, missingSessions, missingSessionDates,
                missingCommonPatient, missingCommonEvents, invalidCommonPatientRefs, invalidCommonEventRefs);
    }

    private static List<String> splitRefs(String value) {
        return List.of(value.split("[,#|;]+|##"));
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    public record Summary(int missingPatients,
                          int duplicatePatients,
                          int missingSessions,
                          int missingSessionDates,
                          int missingCommonPatient,
                          int missingCommonEvents,
                          int invalidCommonPatientRefs,
                          int invalidCommonEventRefs) {
        public boolean hasIssues() {
            return missingPatients > 0 || duplicatePatients > 0 || missingSessions > 0 || missingSessionDates > 0
                    || missingCommonPatient > 0 || missingCommonEvents > 0 || invalidCommonPatientRefs > 0
                    || invalidCommonEventRefs > 0;
        }

        public String summaryLine() {
            if (!hasIssues()) return "Health: OK";
            Map<String, Integer> parts = new HashMap<>();
            parts.put("missing patient_id", missingPatients);
            parts.put("duplicate patient_id", duplicatePatients);
            parts.put("missing composition_id", missingSessions);
            parts.put("missing session dates", missingSessionDates);
            parts.put("missing common patient_reference", missingCommonPatient);
            parts.put("missing common attended_event_references", missingCommonEvents);
            parts.put("invalid patient_reference", invalidCommonPatientRefs);
            parts.put("invalid attended_event_references", invalidCommonEventRefs);
            return parts.entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(" | ", "Health: ", ""));
        }
    }
}
