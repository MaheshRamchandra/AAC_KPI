package com.aac.kpi.service;

import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;

import java.util.*;

/**
 * Links sessions back to patients for UI display, mirroring export behavior.
 */
public final class LinkService {
    private LinkService() {}

    public static void fillPatientAttendedRefs(List<Patient> patients, List<EventSession> sessions) {
        Map<String, List<String>> map = new HashMap<>();
        for (EventSession s : sessions) {
            String raw = s.getEventSessionPatientReferences1();
            if (raw == null || raw.isBlank()) continue;
            // Use the event composition_id (sanitized) so patient references
            // align with the Event Sessions sheet and downstream exports.
            String eventId = sanitizeAlphaNum(s.getCompositionId());
            if (eventId.isEmpty()) continue;
            for (String part : raw.split("##")) {
                String pid = part == null ? "" : part.trim();
                if (pid.isEmpty()) continue;
                map.computeIfAbsent(pid, k -> new ArrayList<>()).add(eventId);
            }
        }
        for (Patient p : patients) {
            List<String> refs = map.getOrDefault(p.getPatientId(), Collections.emptyList());
            p.setAttendedEventReferences(String.join("##", refs));
        }
    }

    private static String sanitizeAlphaNum(String s) {
        if (s == null) return "";
        return s.replaceAll("[^A-Za-z0-9]", "");
    }
}
