package com.aac.kpi.service;

import com.aac.kpi.model.*;
import com.aac.kpi.service.AppState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public final class CommonBuilderService {
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private CommonBuilderService() {}

    public static List<CommonRow> build(List<Patient> patients,
                                        List<EventSession> sessions,
                                        List<Encounter> encounters,
                                        List<QuestionnaireResponse> questionnaires,
                                        List<Practitioner> practitioners) {
        Map<String, List<EventSession>> byPatientSessions = sessions.stream()
                .filter(s -> s.getEventSessionPatientReferences1() != null)
                .collect(Collectors.groupingBy(EventSession::getEventSessionPatientReferences1));
        Map<String, List<Encounter>> byPatientEnc = encounters.stream()
                .filter(e -> e.getEncounterPatientReference()!=null)
                .collect(Collectors.groupingBy(Encounter::getEncounterPatientReference));
        Map<String, List<QuestionnaireResponse>> byPatientQ = questionnaires.stream()
                .filter(q -> q.getQuestionnairePatientReference()!=null)
                .collect(Collectors.groupingBy(QuestionnaireResponse::getQuestionnairePatientReference));

        // Sanitized maps (strip non-alphanumerics) to handle refs like "Patient/<id>"
        Map<String, List<EventSession>> byPatientSessionsSan = new HashMap<>();
        for (EventSession s : sessions) {
            String raw = s.getEventSessionPatientReferences1();
            if (raw == null || raw.isBlank()) continue;
            for (String part : raw.split("##")) {
                String k = sanitizeAlphaNum(part);
                if (!k.isEmpty()) byPatientSessionsSan.computeIfAbsent(k, x -> new ArrayList<>()).add(s);
            }
        }
        Map<String, List<Encounter>> byPatientEncSan = new HashMap<>();
        for (Encounter e : encounters) {
            String k = sanitizeAlphaNum(e.getEncounterPatientReference());
            if (!k.isEmpty()) byPatientEncSan.computeIfAbsent(k, x -> new ArrayList<>()).add(e);
        }
        Map<String, List<QuestionnaireResponse>> byPatientQSan = new HashMap<>();
        for (QuestionnaireResponse q : questionnaires) {
            String k = sanitizeAlphaNum(q.getQuestionnairePatientReference());
            if (!k.isEmpty()) byPatientQSan.computeIfAbsent(k, x -> new ArrayList<>()).add(q);
        }

        List<CommonRow> rows = new ArrayList<>();
        Map<String, Long> aacCounts = patients.stream()
                .collect(Collectors.groupingBy(Patient::getAac, Collectors.counting()));

        String reportingMonth = LocalDate.now().format(MONTH_FMT);

        for (Patient p : patients) {
            CommonRow r = new CommonRow();
            r.setCompositionId(RandomDataUtil.uuid32().toUpperCase());
            r.setVersionId(1);
            // We'll set 'date' (c.lastUpdated used in ExcelWriter for resident_report 'date') later from latest session
            r.setLastUpdated("");
            r.setMetaCode(RandomDataUtil.uuid32());
            r.setReportingMonth(reportingMonth);
            r.setTotalOperatingDays(20);
            r.setTotalClients(aacCounts.getOrDefault(p.getAac(), 0L).intValue());
            r.setStatus("final");

            // author_value / author_display should reflect the AAC id, not a random practitioner id
            String aac = p.getAac() == null ? "" : p.getAac();
            if (aac == null || aac.isBlank()) aac = "AAC";
            r.setAuthorValue(aac);
            String digits = aac.replaceAll("[^0-9]", "");
            r.setAuthorDisplay("Active Ageing Centre " + digits);

            // patient_reference should map to patient_id, or fallback to identifier
            String patientId = p.getPatientId();
            String patientIdent = p.getPatientIdentifierValue();
            String patientRef = (patientId != null && !patientId.isBlank())
                    ? patientId
                    : (patientIdent != null ? patientIdent : "");
            if (patientRef.isBlank()) continue; // cannot build Common row without a patient reference
            r.setPatientReference(patientRef);

            // encounters for patient (finished only) and valid purposes; join with '#'
            String pidSan = sanitizeAlphaNum(p.getPatientId());
            List<Encounter> encList = !byPatientEncSan.getOrDefault(pidSan, List.of()).isEmpty()
                    ? byPatientEncSan.get(pidSan)
                    : byPatientEnc.getOrDefault(p.getPatientId(), List.of());
            // Prefer finished encounters, but include all for the resident if none are marked finished.
            java.util.LinkedHashSet<String> finishedIds = encList.stream()
                    .filter(e -> "finished".equalsIgnoreCase(e.getEncounterStatus()))
                    .map(Encounter::getEncounterId)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            java.util.LinkedHashSet<String> allIds = encList.stream()
                    .map(Encounter::getEncounterId)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            List<String> encRefs = finishedIds.isEmpty()
                    ? new java.util.ArrayList<>(allIds)
                    : new java.util.ArrayList<>(finishedIds);
            if (encRefs.isEmpty()) continue; // Must have at least one encounter to build a resident row
            r.setEncounterReferences(String.join("##", encRefs));

            // questionnaire: latest completed questionnaire reference per resident (by latest date found in answers)
            List<QuestionnaireResponse> qList = !byPatientQSan.getOrDefault(pidSan, List.of()).isEmpty()
                    ? byPatientQSan.get(pidSan)
                    : byPatientQ.getOrDefault(p.getPatientId(), List.of());
            String qRef = qList.stream()
                    .filter(q -> "completed".equalsIgnoreCase(q.getQuestionnaireStatus()))
                    .sorted((a,b) -> latestDateIn(b).compareTo(latestDateIn(a)))
                    .map(QuestionnaireResponse::getQuestionnaireId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("");
            r.setQuestionnaireReference(qRef);

            // Extended resident_report fields populated from Patient where available
            r.setCfs(p.getCfs());
            r.setSocialRiskFactorScore(p.getSocialRiskFactor());
            r.setAapRecommendation(p.getKpiType());
            r.setSocialSupportRecommendation(p.getKpiGroup());
            r.setResidentBefriendingProgrammePeriodStart(p.getBefriendingProgramStartDate());
            r.setResidentBefriendingProgrammePeriodEnd(p.getBefriendingProgramEndDate());
            r.setResidentBuddyingProgrammePeriodStart(p.getBuddyingProgramStartDate());
            r.setResidentBuddyingProgrammePeriodEnd(p.getBuddyingProgramEndDate());

            // Apply defaults to match requested example if fields are empty
            if (isBlank(r.getResidentVolunteerStatus())) r.setResidentVolunteerStatus("TRUE");
            if (isBlank(r.getCstDate())) r.setCstDate("2025-12-31");
            if (r.getCfs() <= 0) r.setCfs(3); // will display as (1-3)
            r.setCfsLabel(CfsUtil.formatCfs(r.getCfs()));
            r.setSocialRiskLabel(r.getSocialRiskFactorScore() > 1 ? ">1" : "1");
            if (r.getSocialRiskFactorScore() <= 0) r.setSocialRiskFactorScore(1);
            if (isBlank(r.getAapRecommendation())) r.setAapRecommendation("AAP Robust");
            if (isBlank(r.getSocialSupportRecommendation())) r.setSocialSupportRecommendation("Befriender");
            if (isBlank(r.getAacOptOutStatus())) r.setAacOptOutStatus("TRUE");
            if (isBlank(r.getAapOptOutStatus())) r.setAapOptOutStatus("TRUE");
            if (isBlank(r.getScreeningDeclarationDate())) r.setScreeningDeclarationDate("2025-12-31");
            if (isBlank(r.getBefriendingOptOutStatus())) r.setBefriendingOptOutStatus("TRUE");
            if (isBlank(r.getBuddyingOptOutStatus())) r.setBuddyingOptOutStatus("TRUE");
            if (isBlank(r.getIrmsReferralRaisedDate())) r.setIrmsReferralRaisedDate("2025-04-01");
            if (isBlank(r.getIrmsReferralAcceptedDate())) r.setIrmsReferralAcceptedDate("2025-04-01");
            if (isBlank(r.getAsgReferralRaisedBy())) r.setAsgReferralRaisedBy("Mr Staff A");
            if (isBlank(r.getAsgReferralAcceptedBy())) r.setAsgReferralAcceptedBy("Mr Staff A");

            // sessions attended (by event_id, joined with '##')
            List<EventSession> sessList = !byPatientSessionsSan.getOrDefault(pidSan, List.of()).isEmpty()
                    ? byPatientSessionsSan.get(pidSan)
                    : byPatientSessions.getOrDefault(p.getPatientId(), List.of());
            List<String> sessionRefs = sessList.stream()
                    .filter(EventSession::isAttendedIndicator)
                    .map(EventSession::getEventSessionId1)
                    .filter(Objects::nonNull)
                    .map(CommonBuilderService::sanitizeAlphaNum)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            r.setAttendedEventReferences(String.join("##", sessionRefs));

            // Derive programme period start/end from attended sessions (min start, max end)
            LocalDateTime minStart = null;
            LocalDateTime maxEnd = null;
            for (EventSession s : sessList) {
                if (!s.isAttendedIndicator()) continue;
                LocalDateTime st = parseDateTime(s.getEventSessionStartDate1());
                LocalDateTime en = parseDateTime(Optional.ofNullable(s.getEventSessionEndDate1()).orElse(""));
                if (st != null) {
                    if (minStart == null || st.isBefore(minStart)) minStart = st;
                    if (en == null) en = st;
                    if (maxEnd == null || en.isAfter(maxEnd)) maxEnd = en;
                }
            }
            java.time.format.DateTimeFormatter D_ONLY = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
            if (minStart != null) {
                String d = minStart.toLocalDate().format(D_ONLY);
                if (isBlank(r.getResidentBefriendingProgrammePeriodStart())) r.setResidentBefriendingProgrammePeriodStart(d);
                if (isBlank(r.getResidentBuddyingProgrammePeriodStart()) && !AppState.shouldSkipBuddyingDerive(p.getPatientId()))
                    r.setResidentBuddyingProgrammePeriodStart(d);
            }
            if (maxEnd != null) {
                String d = maxEnd.toLocalDate().format(D_ONLY);
                if (isBlank(r.getResidentBefriendingProgrammePeriodEnd())) r.setResidentBefriendingProgrammePeriodEnd(d);
                if (isBlank(r.getResidentBuddyingProgrammePeriodEnd()) && !AppState.shouldSkipBuddyingDerive(p.getPatientId()))
                    r.setResidentBuddyingProgrammePeriodEnd(d);
            }

            // 'date' column in resident_report = latest activity (use latest attended session end)
            if (maxEnd != null) {
                r.setLastUpdated(maxEnd.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                // extension_reporting_month based on latest session
                r.setReportingMonth(maxEnd.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
            }

            // Ensure lastUpdated is never blank so Common sheet date cell is a valid date
            if (isBlank(r.getLastUpdated())) {
                r.setLastUpdated(ExcelWriter.nowStamp());
            }

            // Derive IRMS referral dates and staff from encounters if available
            LocalDateTime encMin = null, encMax = null;
            String staff = null;
            for (Encounter e : encList) {
                if (!"finished".equalsIgnoreCase(String.valueOf(e.getEncounterStatus()))) continue;
                LocalDateTime est = parseDateTime(e.getEncounterStart());
                if (est != null) {
                    if (encMin == null || est.isBefore(encMin)) encMin = est;
                    if (encMax == null || est.isAfter(encMax)) encMax = est;
                }
                if (staff == null && e.getEncounterContactedStaffName() != null && !e.getEncounterContactedStaffName().isBlank()) {
                    staff = e.getEncounterContactedStaffName();
                }
            }
            if (encMin != null && isBlank(r.getIrmsReferralRaisedDate())) r.setIrmsReferralRaisedDate(encMin.toLocalDate().format(D_ONLY));
            if (encMax != null && isBlank(r.getIrmsReferralAcceptedDate())) r.setIrmsReferralAcceptedDate(encMax.toLocalDate().format(D_ONLY));
            if (staff != null) {
                if (isBlank(r.getAsgReferralRaisedBy())) r.setAsgReferralRaisedBy(staff);
                if (isBlank(r.getAsgReferralAcceptedBy())) r.setAsgReferralAcceptedBy(staff);
            }

            rows.add(r);
        }

        // AAC summary rows
        for (Map.Entry<String, Long> e : aacCounts.entrySet()) {
            CommonRow r = new CommonRow();
            r.setCompositionId(RandomDataUtil.uuid32().toUpperCase());
            r.setVersionId(1);
            r.setLastUpdated(ExcelWriter.nowStamp());
            r.setMetaCode("aac_report");
            r.setReportingMonth(reportingMonth);
            r.setTotalOperatingDays(20);
            r.setTotalClients(e.getValue().intValue());
            r.setStatus("final");
            r.setAuthorDisplay(e.getKey()); // AAC code
            rows.add(r);
        }

        return rows;
    }

    private static LocalDate latestDateIn(QuestionnaireResponse q) {
        List<String> ds = List.of(q.getQ1(), q.getQ3(), q.getQ5(), q.getQ7(), q.getQ9());
        LocalDate latest = LocalDate.MIN;
        for (String s : ds) {
            try { LocalDate d = LocalDate.parse(s); if (d.isAfter(latest)) latest = d; } catch (Exception ignored) {}
        }
        return latest;
    }

    private static String sanitizeAlphaNum(String s) {
        if (s == null) return "";
        return s.replaceAll("[^A-Za-z0-9]", "");
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {}
        try {
            return LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception ignored) {}
        try {
            return java.time.OffsetDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")).toLocalDateTime();
        } catch (Exception ignored) {}
        try {
            return LocalDate.parse(s).atStartOfDay();
        } catch (Exception ignored) {}
        return null;
    }
}
