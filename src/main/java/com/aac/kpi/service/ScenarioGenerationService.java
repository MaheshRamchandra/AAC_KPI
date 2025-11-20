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

        int aapAttendanceCount = parsePositiveInt(scenario.getNumberOfAapAttendance(), 0);
        String cfsText = scenario.getCfs();
        IntRange cfsRange = parseCfsRange(cfsText);

        String modeOfEvent = nvl(scenario.getModeOfEvent());
        String boundary = nvl(scenario.getWithinBoundary());
        String purpose = nvl(scenario.getPurposeOfContact());
        int age = parsePositiveInt(scenario.getAge(), 60);

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
                    aapRaw, aapDate != null ? aapDate : baseDate, purpose);
            sessions.addAll(scenarioSessions);
        }

        List<Encounter> scenarioEncounters = generateEncountersForScenario(
                scenarioPatients, contactDate != null ? contactDate : baseDate, purpose);
        encounters.addAll(scenarioEncounters);

        List<QuestionnaireResponse> scenarioQuestionnaires = generateQuestionnairesForScenario(
                scenarioPatients, baseDate);
        questionnaires.addAll(scenarioQuestionnaires);
    }

    private static List<EventSession> generateEventSessionsForScenario(List<Patient> patients,
                                                                       int aapAttendanceCount,
                                                                       String modeOfEvent,
                                                                       String aapRaw,
                                                                       LocalDate aapDate,
                                                                       String purpose) {
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
        String startText = RandomDataUtil.isoTimestampWithOffset(startBase, "+08:00");
        String endText = RandomDataUtil.isoTimestampWithOffset(endBase, "+08:00");

        List<String> ids = new ArrayList<>();
        for (Patient p : patients) ids.add(p.getPatientId());
        int n = ids.size();

        for (int row = 0; row < n; row++) {
            EventSession s = new EventSession();
            s.setCompositionId(RandomDataUtil.uuid32());
            s.setNumberOfEventSessions(1);
            s.setEventSessionId1(RandomDataUtil.randomEventId());
            s.setEventSessionMode1(modeOfEvent.isBlank() ? "In-person" : modeOfEvent);
            s.setEventSessionStartDate1(startText);
            s.setEventSessionEndDate1(endText);
            s.setEventSessionDuration1(durationMinutes);
            s.setEventSessionVenue1(RandomDataUtil.randomVenue());
            s.setEventSessionCapacity1(Math.max(aapAttendanceCount, RandomDataUtil.randomCapacity()));
            List<String> refs = new ArrayList<>();
            for (int k = 0; k < aapAttendanceCount; k++) {
                String pid = ids.get((row * aapAttendanceCount + k) % n);
                refs.add(pid);
            }
            s.setEventSessionPatientReferences1(String.join("##", refs));
            s.setAttendedIndicator(true);
            s.setPurposeOfContact(purpose);
            list.add(s);
        }
        if (!list.isEmpty()) {
            AppState.addHighlightedEventSessionCompositionId(
                    list.get(0).getCompositionId());
        }
        return list;
    }

    private static List<Encounter> generateEncountersForScenario(List<Patient> patients,
                                                                 LocalDate contactDate,
                                                                 String purpose) {
        List<Encounter> list = new ArrayList<>();
        if (patients.isEmpty()) return List.of();
        if (contactDate == null) contactDate = LocalDate.now();
        LocalDateTime start = contactDate.atTime(10, 0);

        String[] displays = {"Home Visit", "Video Call", "Centre Visit", "Phone Call"};
        String[] prefixes = {"Mr", "Mrs", "Ms", "Dr"};
        String[] staff = {"Staff A", "Staff B", "Staff C", "Staff D", "Nurse E", "Nurse F", "Counsellor G"};
        String[] referred = {"GP", "Family", "Self-referral", "Neighbour", "Social Worker"};
        Random rnd = new Random();

        for (Patient p : patients) {
            Encounter e = new Encounter();
            e.setEncounterId(RandomDataUtil.uuid32().toUpperCase(Locale.ROOT));
            e.setEncounterStatus("finished");
            e.setEncounterDisplay(displays[rnd.nextInt(displays.length)]);
            e.setEncounterStart(RandomDataUtil.isoTimestampWithOffset(start, "+08:00"));
            e.setEncounterPurpose(purpose.isBlank() ? "Befriending" : purpose);
            String staffName = prefixes[rnd.nextInt(prefixes.length)] + " " + staff[rnd.nextInt(staff.length)];
            e.setEncounterContactedStaffName(staffName);
            e.setEncounterReferredBy(referred[rnd.nextInt(referred.length)]);
            e.setEncounterPatientReference(p.getPatientId());
            list.add(e);
        }
        if (!list.isEmpty()) {
            AppState.addHighlightedEncounterId(list.get(0).getEncounterId());
        }
        return list;
    }

    private static List<QuestionnaireResponse> generateQuestionnairesForScenario(List<Patient> patients,
                                                                                LocalDate baseDate) {
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
