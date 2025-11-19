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
            String comp = sanitizeAlphaNum(s.getCompositionId());
            if (comp.isEmpty()) continue;
            for (String part : raw.split("##")) {
                String pid = part == null ? "" : part.trim();
                if (pid.isEmpty()) continue;
                map.computeIfAbsent(pid, k -> new ArrayList<>()).add(comp);
            }
        }
        for (Patient p : patients) {
            List<String> comps = map.getOrDefault(p.getPatientId(), Collections.emptyList());
            p.setAttendedEventReferences(String.join("\n", comps));
        }
    }

    private static String sanitizeAlphaNum(String s) {
        if (s == null) return "";
        return s.replaceAll("[^A-Za-z0-9]", "");
    }
}
