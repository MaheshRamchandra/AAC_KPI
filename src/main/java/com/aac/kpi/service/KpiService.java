package com.aac.kpi.service;

import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class KpiService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd[ HH:mm][ HH:mm:ss]");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private KpiService() {}

    public static void computeForFY(List<Patient> patients, List<EventSession> sessions,
                                    LocalDate fyStart, LocalDate fyEnd) {
        computeForFY(patients, sessions, fyStart, fyEnd, com.aac.kpi.service.AppState.getKpiConfig());
    }

    public static void computeForFY(List<Patient> patients, List<EventSession> sessions,
                                    LocalDate fyStart, LocalDate fyEnd, KpiConfig config) {
        for (Patient p : patients) {
            p.setKpiType("");
            p.setKpiGroup("");

            int cfs = p.getCfs();
            int rf = p.getSocialRiskFactor();

            long inPersonCount = countInPerson(p.getPatientId(), sessions, fyStart, fyEnd, true);
            boolean hasFunctionalScreen = hasPurpose(p.getPatientId(), sessions,
                    "Functional or Health Screening Client Self-Declaration", fyStart, fyEnd);

            // Robust
            if (inRange(cfs,1,3)) {
                if (inPersonCount >= config.robustMinInPerson && hasFunctionalScreen) { p.setKpiType("Robust"); p.setKpiGroup("Group 1"); continue; }
            }
            if (inRange(cfs,6,9)) {
                if (inPersonCount >= config.robustMinInPerson && hasFunctionalScreen) { p.setKpiType("Robust (Group 2 – CFS 6-9)"); p.setKpiGroup("Group 2"); continue; }
            }

            // Frail
            if (inRange(cfs,4,5)) {
                if (inPersonCount >= config.frailMinInPerson && hasFunctionalScreen) { p.setKpiType("Frail"); p.setKpiGroup("Group 1"); continue; }
            }
            if (inRange(cfs,6,9)) {
                if (inPersonCount >= config.frailMinInPerson && hasFunctionalScreen) { p.setKpiType("Frail (Group 2 – Very Frail)"); p.setKpiGroup("Group 2"); continue; }
            }

            // Buddying
            boolean buddyActive = programActiveWithinFY(p.getBuddyingProgramStartDate(), p.getBuddyingProgramEndDate(), fyStart, fyEnd);
            long buddyCount = countPurpose(p.getPatientId(), sessions, "buddying", fyStart, fyEnd);
            if (rf == 1) {
                if (inRange(cfs,1,3) && inPersonCount >= config.buddyingMinInPerson && buddyCount >= 12 && buddyActive) { p.setKpiType("Buddying"); p.setKpiGroup("Group 1 (Robust)"); continue; }
                if (inRange(cfs,4,5) && inPersonCount >= config.buddyingMinInPerson && buddyCount >= 12 && buddyActive) { p.setKpiType("Buddying (Frail)"); p.setKpiGroup("Group 2"); continue; }
                if (inRange(cfs,6,9) && inPersonCount >= config.buddyingMinInPerson && buddyCount >= 12 && buddyActive) { p.setKpiType("Buddying (Very Frail)"); p.setKpiGroup("Group 3"); continue; }
            }

            // Befriending (assumption: RF > 1)
            boolean befActive = programActiveWithinFY(p.getBefriendingProgramStartDate(), p.getBefriendingProgramEndDate(), fyStart, fyEnd);
            long befCount = countPurpose(p.getPatientId(), sessions, "befriending", fyStart, fyEnd);
            long aap12 = countAAP(p.getPatientId(), sessions, fyStart, fyEnd);
            if (rf > 1) {
                if (inRange(cfs,1,3) && inPersonCount >= config.befriendingMinInPerson && aap12 >= 12 && befCount >= 52 && befActive) { p.setKpiType("Befriending (Robust)"); p.setKpiGroup("Group 1"); continue; }
                if (inRange(cfs,4,5) && inPersonCount >= config.befriendingMinInPerson && aap12 >= 12 && befCount >= 52 && befActive) { p.setKpiType("Befriending (Frail)"); p.setKpiGroup("Group 2"); continue; }
                if (inRange(cfs,6,9) && inPersonCount >= config.befriendingMinInPerson && aap12 >= 12 && befCount >= 52 && befActive) { p.setKpiType("Befriending (Very Frail)"); p.setKpiGroup("Group 3"); }
            }
        }
    }

    private static boolean inRange(int v, int lo, int hi) { return v >= lo && v <= hi; }

    private static long countInPerson(String pid, List<EventSession> sessions,
                                      LocalDate fyStart, LocalDate fyEnd, boolean attendedOnly) {
        return sessions.stream().filter(s -> sessionHasPatient(s, pid))
                .filter(s -> eqIgnoreCase(s.getEventSessionMode1(), "In-person"))
                .filter(s -> !attendedOnly || s.isAttendedIndicator())
                .filter(s -> inFY(s.getEventSessionStartDate1(), fyStart, fyEnd))
                .count();
    }

    private static long countPurpose(String pid, List<EventSession> sessions, String purpose,
                                     LocalDate fyStart, LocalDate fyEnd) {
        return sessions.stream().filter(s -> sessionHasPatient(s, pid))
                .filter(s -> eqIgnoreCase(s.getPurposeOfContact(), purpose))
                .filter(s -> inFY(s.getEventSessionStartDate1(), fyStart, fyEnd))
                .count();
    }

    private static boolean hasPurpose(String pid, List<EventSession> sessions, String purpose,
                                      LocalDate fyStart, LocalDate fyEnd) {
        return countPurpose(pid, sessions, purpose, fyStart, fyEnd) > 0;
    }

    private static long countAAP(String pid, List<EventSession> sessions,
                                 LocalDate fyStart, LocalDate fyEnd) {
        // Treat any attended in-person session that is not a buddying/befriending/screening contact as AAP
        return sessions.stream().filter(s -> sessionHasPatient(s, pid))
                .filter(s -> eqIgnoreCase(s.getEventSessionMode1(), "In-person"))
                .filter(EventSession::isAttendedIndicator)
                .filter(s -> inFY(s.getEventSessionStartDate1(), fyStart, fyEnd))
                .filter(s -> {
                    String p = s.getPurposeOfContact();
                    if (p == null) return true; // assume AAP
                    String x = p.toLowerCase(Locale.ROOT);
                    return !(x.contains("buddying") || x.contains("befriending")
                            || x.contains("functional") || x.contains("screening"));
                })
                .count();
    }

    private static boolean programActiveWithinFY(String start, String end,
                                                 LocalDate fyStart, LocalDate fyEnd) {
        if ((start == null || start.isBlank()) && (end == null || end.isBlank())) return true; // assume active
        LocalDate s = parseDate(start);
        LocalDate e = parseDate(end);
        if (s == null) s = LocalDate.MIN;
        if (e == null) e = LocalDate.MAX;
        // Active if start <= fyEnd and (end is null or end >= fyStart)
        return !s.isAfter(fyEnd) && !e.isBefore(fyStart);
    }

    private static boolean eqIgnoreCase(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }

    private static boolean inFY(String dateTimeStr, LocalDate fyStart, LocalDate fyEnd) {
        LocalDate d = parseDateTime(dateTimeStr);
        if (d == null) return false;
        return !(d.isBefore(fyStart) || d.isAfter(fyEnd));
    }

    private static LocalDate parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            // Try plain local datetime (yyyy-MM-dd, with optional time)
            LocalDateTime dt = LocalDateTime.parse(s, DATE_TIME);
            return dt.toLocalDate();
        } catch (Exception ignored) {
            try {
                // Try ISO 8601 with offset, e.g. 2025-08-24T09:00:00+08:00
                java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(s);
                return odt.toLocalDate();
            } catch (Exception ignored2) {
                try {
                    return LocalDate.parse(s, DATE_ONLY);
                } catch (Exception e) { return null; }
            }
        }
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s, DATE_ONLY); } catch (Exception e) { return null; }
    }

    private static boolean sessionHasPatient(EventSession s, String pid) {
        if (pid == null || pid.isBlank()) return false;
        String raw = s.getEventSessionPatientReferences1();
        if (raw == null || raw.isBlank()) return false;
        for (String part : raw.split("##")) {
            if (pid.equals(part.trim())) return true;
        }
        return false;
    }
}
