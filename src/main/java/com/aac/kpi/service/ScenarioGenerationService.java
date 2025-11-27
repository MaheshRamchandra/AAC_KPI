package com.aac.kpi.service;

import com.aac.kpi.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scenario-driven generator.
 * Takes tester-provided ScenarioTestCase rows and produces
 * fully-linked master lists for all tabs using those fields
 * as direct inputs (no KPI logic beyond simple counts).
 */
public final class ScenarioGenerationService {
    private static final Random RND = new Random();

    public static final class Result {
        public final List<Patient> patients;
        public final List<EventSession> sessions;
        public final List<Encounter> encounters;
        public final List<QuestionnaireResponse> questionnaires;
        public final List<CommonRow> commonRows;
        public final List<Practitioner> practitioners;

        Result(List<Patient> patients,
               List<EventSession> sessions,
               List<Encounter> encounters,
               List<QuestionnaireResponse> questionnaires,
               List<CommonRow> commonRows,
               List<Practitioner> practitioners) {
            this.patients = patients;
            this.sessions = sessions;
            this.encounters = encounters;
            this.questionnaires = questionnaires;
            this.commonRows = commonRows;
            this.practitioners = practitioners;
        }
    }

    private static final DateTimeFormatter[] DATE_PATTERNS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d-MMM-yy"),
            DateTimeFormatter.ofPattern("d-MMM-yyyy"),
            DateTimeFormatter.ofPattern("dd-MMM-yy"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy")
    };

    private ScenarioGenerationService() {}

    public static Result generate(List<ScenarioTestCase> scenarios) {
        if (scenarios == null || scenarios.isEmpty()) {
            return new Result(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }
        // Ensure master data exists so we can derive
        // AAC centres and their served postal codes.
        MasterDataService.MasterData masterData = AppState.getMasterData();
        if (masterData == null) {
            masterData = MasterDataService.generate();
            AppState.setMasterData(masterData);
        }

        Map<String, Set<String>> aacPostalCodes = new HashMap<>();
        Set<String> allPostalCodes = new HashSet<>();
        for (MasterDataService.Volunteer v : masterData.getVolunteers()) {
            String aacId = v.aacCenterId();
            String postal = v.postalCode();
            if (aacId == null || aacId.isBlank() || postal == null || postal.isBlank()) continue;
            aacPostalCodes.computeIfAbsent(aacId, k -> new HashSet<>()).add(postal);
            allPostalCodes.add(postal);
        }

        List<Patient> patients = new ArrayList<>();
        List<EventSession> sessions = new ArrayList<>();
        List<Encounter> encounters = new ArrayList<>();
        List<QuestionnaireResponse> questionnaires = new ArrayList<>();
        List<Practitioner> practitioners = new ArrayList<>();

        // Scenario header highlights: first patient/encounter/etc of each scenario
        AppState.clearHighlightedPatientIds();
        AppState.clearHighlightedEventSessionCompositionIds();
        AppState.clearHighlightedEncounterIds();
        AppState.clearHighlightedQuestionnaireIds();
        AppState.clearHighlightedPractitionerIds();

        List<MasterDataService.AacCenter> centers = masterData.getAacCenters();
        for (int idx = 0; idx < scenarios.size(); idx++) {
            ScenarioTestCase scenario = scenarios.get(idx);
            MasterDataService.AacCenter center = centers.isEmpty()
                    ? null
                    : centers.get(idx % centers.size());
            generateForScenario(scenario, patients, sessions, encounters, questionnaires, practitioners,
                    masterData, aacPostalCodes, allPostalCodes, center);
        }

        // Build Common rows from the aggregated lists; Common rows stay
        // scenario-isolated because patients/events/encounters/questionnaires
        // are generated per scenario with unique ids.
        List<CommonRow> commonRows = CommonBuilderService.build(patients, sessions, encounters, questionnaires,
                practitioners);

        return new Result(patients, sessions, encounters, questionnaires, commonRows, practitioners);
    }

    private static void generateForScenario(ScenarioTestCase scenario,
                                            List<Patient> patients,
                                            List<EventSession> sessions,
                                            List<Encounter> encounters,
                                            List<QuestionnaireResponse> questionnaires,
                                            List<Practitioner> practitioners,
                                            MasterDataService.MasterData masterData,
                                            Map<String, Set<String>> aacPostalCodes,
                                            Set<String> allPostalCodes,
                                            MasterDataService.AacCenter selectedCenter) {
        if (scenario == null) return;
        int numberOfSeniors = parsePositiveInt(scenario.getNumberOfSeniors(), 0);
        if (numberOfSeniors <= 0) return;

        int aapAttendanceCount = parsePositiveInt(scenario.getNumberOfAapAttendance(), 1);
        String cfsText = scenario.getCfs();
        IntRange cfsRange = parseCfsRange(cfsText);
        Map<String, String> extras = scenario.getExtraFields() == null ? Map.of() : scenario.getExtraFields();
        List<ScenarioTestCase.ColumnOverride> overrides = scenario.getColumnOverrides() == null ? List.of() : scenario.getColumnOverrides();

        int contactLogCount = parsePositiveInt(scenario.getContactLogs(), 1);
        if (contactLogCount <= 0) {
            contactLogCount = parsePositiveInt(getExtra(extras, "contactlogs", "contact log", "encounters", "encountercount", "contact logs (encounters)"), 1);
        }

        String modeOfEvent = nvl(scenario.getModeOfEvent());
        String boundary = nvl(scenario.getWithinBoundary());
        String purpose = nvl(scenario.getPurposeOfContact());
        int age = parsePositiveInt(scenario.getAge(), 60);
        int socialRisk = parseSocialRisk(getExtra(extras, "socialriskfactor", "rf", "socialrisk", "social risk factor"), 0);
        String kpiTypeOverride = getExtra(extras, "kpi type", "kpi_type", "kpi");
        String kpiGroupOverride = getExtra(extras, "kpi group", "kpi_group");

        String aapRaw = scenario.getAapSessionDate();
        LocalDate aapDate = parseDateFlexible(aapRaw);
        LocalDate contactDate = parseDateFlexible(scenario.getDateOfContact());

        LocalDate baseDate = contactDate != null ? contactDate
                : (aapDate != null ? aapDate : LocalDate.now());

        String aacId = selectedCenter != null ? selectedCenter.aacCenterId() : RandomDataUtil.randomAAC();
        Set<String> servedPostals = aacPostalCodes.getOrDefault(aacId, Set.of());

        boolean isWithin = boundary.toLowerCase(Locale.ENGLISH).contains("within");
        boolean isOutside = boundary.toLowerCase(Locale.ENGLISH).contains("out");

        List<Patient> scenarioPatients = new ArrayList<>();
        for (int i = 0; i < numberOfSeniors; i++) {
            Patient p = new Patient();
            String patientId = RandomDataUtil.uuid32();
            p.setPatientId(patientId);
            // keep identifiers offline/dummy; no Selenium
            p.setPatientIdentifierValue(NRICGeneratorUtil.generateFakeNRIC());
            p.setPatientBirthdate(RandomDataUtil.dobForExactAge(age));
            String postal;
            if (isOutside) {
                postal = chooseOutsidePostal(servedPostals, allPostalCodes);
            } else {
                postal = chooseWithinPostal(servedPostals, allPostalCodes);
            }
            p.setPatientPostalCode(postal);
            p.setWorkingRemarks("Generated from Scenario Builder");
            p.setGroup(RandomDataUtil.randomGroup());
            p.setType(boundary);
            p.setAac(aacId);
            p.setCfs(randomIntInRange(cfsRange));
            if (socialRisk > 0) {
                p.setSocialRiskFactor(socialRisk);
            }
            if (kpiTypeOverride != null && !kpiTypeOverride.isBlank()) {
                p.setKpiType(kpiTypeOverride);
            }
            if (kpiGroupOverride != null && !kpiGroupOverride.isBlank()) {
                p.setKpiGroup(kpiGroupOverride);
            }
            applyPatientOverrides(p, overrides);
            scenarioPatients.add(p);
            patients.add(p);
            if (i == 0) {
                AppState.addHighlightedPatientId(patientId);
            }

            Practitioner pr = new Practitioner();
            String prId = RandomDataUtil.uuid32();
            pr.setPractitionerId(prId);
            pr.setPractitionerIdentifierValue(NRICGeneratorUtil.generateFakeNRIC());
            pr.setPractitionerIdentifierSystem("http://ihis.sg/identifier/aac-staff-id");
            pr.setPractitionerManpowerPosition("Volunteer");
            pr.setPractitionerVolunteerName(RandomDataUtil.randomVolunteerName());
            pr.setPractitionerManpowerCapacity(0.8);
            pr.setPractitionerVolunteerAge(RandomDataUtil.randomInt(25, 65));
            pr.setWorkingRemarks("Generated from Scenario Builder");
            practitioners.add(pr);
            if (i == 0) {
                AppState.addHighlightedPractitionerId(prId);
            }
        }

        if (aapAttendanceCount > 0) {
            List<EventSession> scenarioSessions = generateEventSessionsForScenario(
                    scenarioPatients, aapAttendanceCount, modeOfEvent,
                    aapRaw, aapDate != null ? aapDate : baseDate, purpose, overrides);
            sessions.addAll(scenarioSessions);
        }

        List<Encounter> scenarioEncounters = generateEncountersForScenario(
                scenarioPatients, contactDate != null ? contactDate : baseDate, purpose, contactLogCount, overrides);
        encounters.addAll(scenarioEncounters);

        List<QuestionnaireResponse> scenarioQuestionnaires = generateQuestionnairesForScenario(
                scenarioPatients, baseDate, overrides);
        questionnaires.addAll(scenarioQuestionnaires);
    }

    private static List<EventSession> generateEventSessionsForScenario(List<Patient> patients,
                                                                       int aapAttendanceCount,
                                                                       String modeOfEvent,
                                                                       String aapRaw,
                                                                       LocalDate aapDate,
                                                                       String purpose,
                                                                       List<ScenarioTestCase.ColumnOverride> overrides) {
        List<EventSession> list = new ArrayList<>();
        if (patients.isEmpty() || aapAttendanceCount <= 0) return List.of();

        LocalDateTime startBase;
        LocalDateTime endBase;
        if (aapRaw != null && aapRaw.toLowerCase(Locale.ENGLISH).contains("between")
                && aapRaw.contains("01 Apr") && aapRaw.contains("31 Mar")) {
            // Use FY 2025-04-01..2026-03-31 window when a range is specified
            String[] parts = RandomDataUtil.randomEventDateTimeFY2025_26AndDuration();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            startBase = LocalDateTime.parse(parts[0], fmt);
            endBase = LocalDateTime.parse(parts[1], fmt);
        } else {
            // If AAP Session Date is a concrete date (e.g. 4-May-24),
            // use that date as the session start and the end of the
            // corresponding FY (01 Apr..31 Mar) as the session end.
            LocalDate explicitAap = parseDateFlexible(aapRaw);
            if (explicitAap != null) {
                startBase = explicitAap.atTime(9, 0);
                LocalDate fyEnd = explicitAap.getMonthValue() >= 4
                        ? LocalDate.of(explicitAap.getYear() + 1, 3, 31)
                        : LocalDate.of(explicitAap.getYear(), 3, 31);
                endBase = fyEnd.atTime(17, 0);
            } else {
                // Fallback: treat the resolved base date as a simple
                // 2-hour window so generation still works.
                startBase = aapDate.atTime(9, 0);
                endBase = startBase.plusHours(2);
            }
        }
        int durationMinutes = (int) java.time.Duration.between(startBase, endBase).toMinutes();
        for (Patient patient : patients) {
            for (int attendanceIndex = 0; attendanceIndex < aapAttendanceCount; attendanceIndex++) {
                EventSession s = new EventSession();
                s.setCompositionId(RandomDataUtil.uuid32());
                s.setNumberOfEventSessions(1);
                s.setEventSessionId1(RandomDataUtil.randomEventId());
                s.setEventSessionMode1(modeOfEvent.isBlank() ? "In-person" : modeOfEvent);
                LocalDateTime sessionStart = startBase.plusDays(attendanceIndex);
                LocalDateTime sessionEnd = endBase.plusDays(attendanceIndex);
                s.setEventSessionStartDate1(RandomDataUtil.isoTimestampWithOffset(sessionStart, "+08:00"));
                s.setEventSessionEndDate1(RandomDataUtil.isoTimestampWithOffset(sessionEnd, "+08:00"));
                s.setEventSessionDuration1(durationMinutes);
                s.setEventSessionVenue1(RandomDataUtil.randomVenue());
                s.setEventSessionCapacity1(Math.max(1, RandomDataUtil.randomCapacity()));
                s.setEventSessionPatientReferences1(patient.getPatientId());
                s.setAttendedIndicator(true);
                s.setPurposeOfContact(purpose);
                applyEventOverrides(s, overrides);
                list.add(s);
            }
        }
        if (!list.isEmpty()) {
            AppState.addHighlightedEventSessionCompositionId(
                    list.get(0).getCompositionId());
        }
        return list;
    }

    private static List<Encounter> generateEncountersForScenario(List<Patient> patients,
                                                                 LocalDate contactDate,
                                                                 String purpose,
                                                                 int contactLogCount,
                                                                 List<ScenarioTestCase.ColumnOverride> overrides) {
        List<Encounter> list = new ArrayList<>();
        if (patients.isEmpty()) return List.of();
        if (contactLogCount <= 0) contactLogCount = 1;
        if (contactDate == null) contactDate = LocalDate.now();
        LocalDateTime start = contactDate.atTime(10, 0);

        String[] displays = {"Home Visit", "Video Call", "Centre Visit", "Phone Call"};
        String[] prefixes = {"Mr", "Mrs", "Ms", "Dr"};
        String[] staff = {"Staff A", "Staff B", "Staff C", "Staff D", "Nurse E", "Nurse F", "Counsellor G"};
        String[] referred = {"GP", "Family", "Self-referral", "Neighbour", "Social Worker"};
        Random rnd = new Random();

        for (Patient p : patients) {
            for (int i = 0; i < contactLogCount; i++) {
                Encounter e = new Encounter();
                e.setEncounterId(RandomDataUtil.uuid32().toUpperCase(Locale.ROOT));
                e.setEncounterStatus("finished");
                e.setEncounterDisplay(displays[rnd.nextInt(displays.length)]);
                LocalDateTime encounterStart = start.plusDays(i % 3).plusMinutes(30L * i);
                e.setEncounterStart(RandomDataUtil.isoTimestampWithOffset(encounterStart, "+08:00"));
                e.setEncounterPurpose(purpose.isBlank() ? "Befriending" : purpose);
                String staffName = prefixes[rnd.nextInt(prefixes.length)] + " " + staff[rnd.nextInt(staff.length)];
                e.setEncounterContactedStaffName(staffName);
                e.setEncounterReferredBy(referred[rnd.nextInt(referred.length)]);
                e.setEncounterPatientReference(p.getPatientId());
                applyEncounterOverrides(e, overrides);
                list.add(e);
            }
        }
        if (!list.isEmpty()) {
            AppState.addHighlightedEncounterId(list.get(0).getEncounterId());
        }
        return list;
    }

    private static List<QuestionnaireResponse> generateQuestionnairesForScenario(List<Patient> patients,
                                                                                 LocalDate baseDate,
                                                                                 List<ScenarioTestCase.ColumnOverride> overrides) {
        List<QuestionnaireResponse> list = new ArrayList<>();
        if (patients.isEmpty()) return List.of();
        if (baseDate == null) baseDate = LocalDate.now();
        LocalDate start = baseDate.minusDays(30);
        LocalDate end = baseDate.plusDays(30);

        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);
            QuestionnaireResponse qr = new QuestionnaireResponse();
            qr.setQuestionnaireId(RandomDataUtil.uuid32().toUpperCase(Locale.ROOT));
            qr.setQuestionnaireStatus("completed");
            qr.setQ1(RandomDataUtil.randomDateBetween(start, end));
            qr.setQ2(String.valueOf(RandomDataUtil.randomInt(1, 5)));
            qr.setQ3(RandomDataUtil.randomDateBetween(start, end));
            qr.setQ4(String.valueOf(RandomDataUtil.randomInt(1, 5)));
            qr.setQ5(RandomDataUtil.randomDateBetween(start, end));
            qr.setQ6(String.valueOf(RandomDataUtil.randomInt(1, 5)));
            qr.setQ7(RandomDataUtil.randomDateBetween(start, end));
            qr.setQ8(String.valueOf(RandomDataUtil.randomInt(1, 5)));
            qr.setQ9(RandomDataUtil.randomDateBetween(start, end));
            qr.setQ10(String.valueOf(RandomDataUtil.randomInt(1, 5)));
            qr.setQuestionnairePatientReference(p.getPatientId());
            applyQuestionnaireOverrides(qr, overrides);
            list.add(qr);
        }
        if (!list.isEmpty()) {
            AppState.addHighlightedQuestionnaireId(list.get(0).getQuestionnaireId());
        }
        return list;
    }

    private static int parsePositiveInt(String raw, int defaultValue) {
        if (raw == null) return defaultValue;
        String digits = raw.replaceAll("[^0-9-]", "");
        if (digits.isBlank()) return defaultValue;
        try {
            int value = Integer.parseInt(digits);
            return value > 0 ? value : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static int parseSocialRisk(String raw, int defaultValue) {
        if (raw == null || raw.isBlank()) return defaultValue;
        String norm = raw.toLowerCase(Locale.ENGLISH).replaceAll("\\s+", "");
        if (norm.contains(">1") || norm.contains("gt1")) return 2;
        if (norm.contains("<1") || norm.contains("=1") || norm.contains("1")) return 1;
        try {
            int v = Integer.parseInt(norm.replaceAll("[^0-9-]", ""));
            return v > 0 ? v : defaultValue;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static String getExtra(Map<String, String> extras, String... keys) {
        if (extras == null || extras.isEmpty()) return "";
        for (String key : keys) {
            String normKey = normalizeKey(key);
            for (Map.Entry<String, String> entry : extras.entrySet()) {
                String entryNorm = normalizeKey(entry.getKey());
                if (entryNorm.contains(normKey) || normKey.contains(entryNorm)) {
                    return entry.getValue();
                }
            }
        }
        return "";
    }

    private static String normalizeKey(String key) {
        if (key == null) return "";
        return key.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", "");
    }

    private static IntRange parseCfsRange(String raw) {
        if (raw == null || raw.isBlank()) {
            return new IntRange(1, 3);
        }
        Matcher m = Pattern.compile("(\\d+)(?:\\s*-\\s*(\\d+))?").matcher(raw);
        if (m.find()) {
            int min = Integer.parseInt(m.group(1));
            int max = min;
            if (m.group(2) != null) {
                max = Integer.parseInt(m.group(2));
            }
            if (min > max) {
                int t = min; min = max; max = t;
            }
            return new IntRange(min, max);
        }
        int v = parsePositiveInt(raw, 3);
        return new IntRange(v, v);
    }

    private static void applyPatientOverrides(Patient p, List<ScenarioTestCase.ColumnOverride> overrides) {
        for (ScenarioTestCase.ColumnOverride ov : overrides) {
            if (!matchesSheet(ov, "patient")) continue;
            String col = normalizeKey(ov.getColumn());
            String val = nvl(ov.getValue());
            switch (col) {
                case "cfs" -> p.setCfs(parsePositiveInt(val, p.getCfs()));
                case "rf", "socialriskfactor" -> p.setSocialRiskFactor(parseSocialRisk(val, p.getSocialRiskFactor()));
                case "kpi", "kpitype", "kpi_type" -> p.setKpiType(val);
                case "kpigroup", "kpi_group" -> p.setKpiGroup(val);
                case "aac" -> p.setAac(val);
                case "type" -> p.setType(val);
                case "group" -> p.setGroup(parsePositiveInt(val, p.getGroup()));
                case "patient_postalcode" -> p.setPatientPostalCode(val);
                case "patient_birthdate" -> p.setPatientBirthdate(val);
                case "workingremarks", "working_remarks" -> p.setWorkingRemarks(val);
                default -> { }
            }
        }
    }

    private static void applyEventOverrides(EventSession s, List<ScenarioTestCase.ColumnOverride> overrides) {
        for (ScenarioTestCase.ColumnOverride ov : overrides) {
            if (!matchesSheet(ov, "event")) continue;
            String col = normalizeKey(ov.getColumn());
            String val = nvl(ov.getValue());
            switch (col) {
                case "event_session_mode1" -> s.setEventSessionMode1(val);
                case "event_session_start_date1" -> s.setEventSessionStartDate1(val);
                case "event_session_end_date1" -> s.setEventSessionEndDate1(val);
                case "event_session_venue1" -> s.setEventSessionVenue1(val);
                case "event_session_capacity1" -> s.setEventSessionCapacity1(parsePositiveInt(val, s.getEventSessionCapacity1()));
                case "purposeofcontact", "purpose_of_contact" -> s.setPurposeOfContact(val);
                case "attendedindicator", "attended_indicator" -> s.setAttendedIndicator(val.equalsIgnoreCase("true") || val.equalsIgnoreCase("yes"));
                default -> { }
            }
        }
    }

    private static void applyEncounterOverrides(Encounter e, List<ScenarioTestCase.ColumnOverride> overrides) {
        for (ScenarioTestCase.ColumnOverride ov : overrides) {
            if (!matchesSheet(ov, "encounter")) continue;
            String col = normalizeKey(ov.getColumn());
            String val = nvl(ov.getValue());
            switch (col) {
                case "encounter_status" -> e.setEncounterStatus(val);
                case "encounter_purpose" -> e.setEncounterPurpose(val);
                case "encounter_contacted_staff_name" -> e.setEncounterContactedStaffName(val);
                case "encounter_referred_by" -> e.setEncounterReferredBy(val);
                case "encounter_display" -> e.setEncounterDisplay(val);
                case "encounter_start" -> e.setEncounterStart(val);
                default -> { }
            }
        }
    }

    private static void applyQuestionnaireOverrides(QuestionnaireResponse qr, List<ScenarioTestCase.ColumnOverride> overrides) {
        for (ScenarioTestCase.ColumnOverride ov : overrides) {
            if (!matchesSheet(ov, "questionnaire")) continue;
            String col = normalizeKey(ov.getColumn());
            String val = nvl(ov.getValue());
            switch (col) {
                case "questionnaire_status" -> qr.setQuestionnaireStatus(val);
                case "q1" -> qr.setQ1(val);
                case "q2" -> qr.setQ2(val);
                case "q3" -> qr.setQ3(val);
                case "q4" -> qr.setQ4(val);
                case "q5" -> qr.setQ5(val);
                case "q6" -> qr.setQ6(val);
                case "q7" -> qr.setQ7(val);
                case "q8" -> qr.setQ8(val);
                case "q9" -> qr.setQ9(val);
                case "q10" -> qr.setQ10(val);
                default -> { }
            }
        }
    }

    private static boolean matchesSheet(ScenarioTestCase.ColumnOverride ov, String token) {
        if (ov == null) return false;
        String sheet = normalizeKey(ov.getSheet());
        return sheet.contains(normalizeKey(token));
    }


    private static int randomIntInRange(IntRange range) {
        if (range == null) return 3;
        if (range.min == range.max) return range.min;
        return range.min + new Random().nextInt((range.max - range.min) + 1);
    }

    private static LocalDate parseDateFlexible(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();
        for (DateTimeFormatter fmt : DATE_PATTERNS) {
            try {
                return LocalDate.parse(trimmed, fmt);
            } catch (Exception ignored) {}
        }
        try {
            // final fallback: ExcelWriter-like flexible parser
            return java.time.LocalDate.parse(trimmed);
        } catch (Exception ignored) {}
        return null;
    }

    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }

    private static final class IntRange {
        final int min;
        final int max;

        IntRange(int min, int max) {
            this.min = min;
            this.max = max;
        }
    }

    private static String chooseWithinPostal(Set<String> servedPostals, Set<String> allPostals) {
        if (servedPostals != null && !servedPostals.isEmpty()) {
            int idx = RND.nextInt(servedPostals.size());
            return servedPostals.stream().skip(idx).findFirst().orElse(RandomDataUtil.randomPostal6());
        }
        if (allPostals != null && !allPostals.isEmpty()) {
            int idx = RND.nextInt(allPostals.size());
            return allPostals.stream().skip(idx).findFirst().orElse(RandomDataUtil.randomPostal6());
        }
        return RandomDataUtil.randomPostal6();
    }

    private static String chooseOutsidePostal(Set<String> servedPostals, Set<String> allPostals) {
        // First try picking from global known postals that are not in this AAC's list
        if (allPostals != null && !allPostals.isEmpty()) {
            List<String> candidates = new ArrayList<>();
            for (String p : allPostals) {
                if (servedPostals == null || !servedPostals.contains(p)) {
                    candidates.add(p);
                }
            }
            if (!candidates.isEmpty()) {
                return candidates.get(RND.nextInt(candidates.size()));
            }
        }
        // Fallback: random until it falls outside the served set
        for (int i = 0; i < 1000; i++) {
            String p = RandomDataUtil.randomPostal6();
            if (servedPostals == null || !servedPostals.contains(p)) {
                return p;
            }
        }
        return RandomDataUtil.randomPostal6();
    }
}
