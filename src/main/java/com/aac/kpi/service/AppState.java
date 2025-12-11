package com.aac.kpi.service;

import com.aac.kpi.service.MasterDataService.MasterData;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public final class AppState {
    private static volatile File currentExcelFile;
    private static volatile boolean dirty;
    private static volatile KpiConfig kpiConfig = new KpiConfig();
    private static volatile int volunteerPractitionerCount = 0;
    private static volatile MasterData masterData;
    private static volatile String jsonConverterJarPath = "lib/KPITool-1.0-SNAPSHOT-jar-with-dependencies.jar";
    private static volatile String javaFxModulePath = "";
    private static volatile String eventReportLabel = "";
    private static volatile int volunteersPerCenter = 3;
    private static volatile int aacCenterCount = 20;
    private static volatile com.aac.kpi.model.RulesConfig rulesConfig = com.aac.kpi.model.RulesConfig.defaults();
    private static volatile String reportingMonthOverride = "";
    private static volatile String reportDateOverride = "";
    private static volatile int robustRegistrationCount = 2;
    private static volatile int frailRegistrationCount = 1;
    private static volatile int buddingRegistrationCount = 6;
    private static volatile int befriendingRegistrationCount = 12;
    private static volatile String registrationOverrideType = "";
    private static final Map<String, List<Boolean>> scenarioRegistrationValues = new HashMap<>();
    private static volatile boolean scenarioSkipPrompts = false;
    private static final Set<String> scenarioSkipBuddyingDeriveIds = new LinkedHashSet<>();
    private static volatile String scenarioSheetName = "";
    private static volatile String scenarioSheetKpiType = "";

    private static final Set<String> highlightedPatientIds = new LinkedHashSet<>();
    private static final Set<String> highlightedEventSessionCompositionIds = new LinkedHashSet<>();
    private static final Set<String> highlightedPractitionerIds = new LinkedHashSet<>();
    private static final Set<String> highlightedEncounterIds = new LinkedHashSet<>();
    private static final Set<String> highlightedQuestionnaireIds = new LinkedHashSet<>();
    // Track per-id highlight color index so all rows from the same scenario share a color
    private static final Map<String, Integer> highlightColorById = new HashMap<>();
    private static final Map<String, Integer> scenarioOrderById = new HashMap<>();

    private AppState() {}

    public static File getCurrentExcelFile() { return currentExcelFile; }
    public static void setCurrentExcelFile(File f) { currentExcelFile = f; }

    public static boolean isDirty() { return dirty; }
    public static void setDirty(boolean d) { dirty = d; }

    public static KpiConfig getKpiConfig() { return kpiConfig; }
    public static void setKpiConfig(KpiConfig cfg) { if (cfg != null) kpiConfig = cfg; }

    // Preferred number of practitioners to include in volunteer_attendance_report
    public static int getVolunteerPractitionerCount() { return volunteerPractitionerCount; }
    public static void setVolunteerPractitionerCount(int n) { volunteerPractitionerCount = Math.max(0, n); }

    public static MasterData getMasterData() { return masterData; }
    public static void setMasterData(MasterData data) { masterData = data; }

    public static String getJsonConverterJarPath() { return jsonConverterJarPath; }
    public static void setJsonConverterJarPath(String path) { if (path != null) jsonConverterJarPath = path; }

    public static String getJavaFxModulePath() { return javaFxModulePath; }
    public static void setJavaFxModulePath(String path) { if (path != null) javaFxModulePath = path; }

    public static String getEventReportLabel() { return eventReportLabel; }
    public static void setEventReportLabel(String label) { eventReportLabel = label == null ? "" : label; }

    public static int getVolunteersPerCenter() { return volunteersPerCenter; }
    public static void setVolunteersPerCenter(int value) { volunteersPerCenter = Math.max(0, value); }

    public static int getAacCenterCount() { return aacCenterCount; }
    public static void setAacCenterCount(int value) {
        // keep within a reasonable range to avoid accidental huge sheets
        if (value <= 0) value = 10;
        aacCenterCount = Math.min(value, 1000);
    }

    public static String getReportingMonthOverride() { return reportingMonthOverride; }
    public static void setReportingMonthOverride(String value) {
        reportingMonthOverride = value == null ? "" : value.trim();
    }

    public static String getReportDateOverride() { return reportDateOverride; }
    public static void setReportDateOverride(String value) {
        reportDateOverride = value == null ? "" : value.trim();
    }

    public static int getRobustRegistrationCount() { return robustRegistrationCount; }
    public static void setRobustRegistrationCount(int value) { robustRegistrationCount = Math.max(0, value); }

    public static int getFrailRegistrationCount() { return frailRegistrationCount; }
    public static void setFrailRegistrationCount(int value) { frailRegistrationCount = Math.max(0, value); }

    public static int getBuddingRegistrationCount() { return buddingRegistrationCount; }
    public static void setBuddingRegistrationCount(int value) { buddingRegistrationCount = Math.max(0, value); }

    public static int getBefriendingRegistrationCount() { return befriendingRegistrationCount; }
    public static void setBefriendingRegistrationCount(int value) { befriendingRegistrationCount = Math.max(0, value); }

    public static String getRegistrationOverrideType() { return registrationOverrideType; }
    public static void setRegistrationOverrideType(String value) {
        registrationOverrideType = value == null ? "" : value.trim();
    }

    public static void clearScenarioRegistrationValues() { scenarioRegistrationValues.clear(); }
    public static void putScenarioRegistrationValues(String key, List<Boolean> values) {
        if (key == null || key.isBlank()) return;
        if (values == null) return;
        scenarioRegistrationValues.put(key, List.copyOf(values));
    }
    public static List<Boolean> getScenarioRegistrationValues(String key) {
        if (key == null || key.isBlank()) return List.of();
        List<Boolean> vals = scenarioRegistrationValues.get(key);
        return vals == null ? List.of() : vals;
    }

    public static boolean isScenarioSkipPrompts() { return scenarioSkipPrompts; }
    public static void setScenarioSkipPrompts(boolean value) { scenarioSkipPrompts = value; }

    public static void addSkipBuddyingDeriveId(String patientId) {
        if (patientId != null && !patientId.isBlank()) {
            scenarioSkipBuddyingDeriveIds.add(patientId);
        }
    }

    public static boolean shouldSkipBuddyingDerive(String patientId) {
        return patientId != null && scenarioSkipBuddyingDeriveIds.contains(patientId);
    }

    public static void clearSkipBuddyingDerive() { scenarioSkipBuddyingDeriveIds.clear(); }

    public static String getScenarioSheetName() { return scenarioSheetName; }
    public static void setScenarioSheetName(String name) { scenarioSheetName = name == null ? "" : name; }

    public static String getScenarioSheetKpiType() { return scenarioSheetKpiType; }
    public static void setScenarioSheetKpiType(String kpiType) { scenarioSheetKpiType = kpiType == null ? "" : kpiType; }

    public static Set<String> getHighlightedPatientIds() { return Collections.unmodifiableSet(highlightedPatientIds); }
    public static void addHighlightedPatientId(String id) { addHighlightedPatientId(id, 0); }
    public static void addHighlightedPatientId(String id, int colorIndex) {
        if (id != null && !id.isBlank()) {
            highlightedPatientIds.add(id);
            highlightColorById.put(id, colorIndex);
        }
    }
    public static void clearHighlightedPatientIds() {
        highlightedPatientIds.clear();
        highlightColorById.clear();
        scenarioOrderById.clear();
    }

    public static Set<String> getHighlightedEventSessionCompositionIds() {
        return Collections.unmodifiableSet(highlightedEventSessionCompositionIds);
    }
    public static void addHighlightedEventSessionCompositionId(String id) { addHighlightedEventSessionCompositionId(id, 0); }
    public static void addHighlightedEventSessionCompositionId(String id, int colorIndex) {
        if (id != null && !id.isBlank()) {
            highlightedEventSessionCompositionIds.add(id);
            highlightColorById.put(id, colorIndex);
        }
    }
    public static void clearHighlightedEventSessionCompositionIds() { highlightedEventSessionCompositionIds.clear(); }

    public static Set<String> getHighlightedPractitionerIds() { return Collections.unmodifiableSet(highlightedPractitionerIds); }
    public static void addHighlightedPractitionerId(String id) { addHighlightedPractitionerId(id, 0); }
    public static void addHighlightedPractitionerId(String id, int colorIndex) {
        if (id != null && !id.isBlank()) {
            highlightedPractitionerIds.add(id);
            highlightColorById.put(id, colorIndex);
        }
    }
    public static void clearHighlightedPractitionerIds() { highlightedPractitionerIds.clear(); }

    public static Set<String> getHighlightedEncounterIds() { return Collections.unmodifiableSet(highlightedEncounterIds); }
    public static void addHighlightedEncounterId(String id) { addHighlightedEncounterId(id, 0); }
    public static void addHighlightedEncounterId(String id, int colorIndex) {
        if (id != null && !id.isBlank()) {
            highlightedEncounterIds.add(id);
            highlightColorById.put(id, colorIndex);
        }
    }
    public static void clearHighlightedEncounterIds() { highlightedEncounterIds.clear(); }

    public static Set<String> getHighlightedQuestionnaireIds() {
        return Collections.unmodifiableSet(highlightedQuestionnaireIds);
    }
    public static void addHighlightedQuestionnaireId(String id) { addHighlightedQuestionnaireId(id, 0); }
    public static void addHighlightedQuestionnaireId(String id, int colorIndex) {
        if (id != null && !id.isBlank()) {
            highlightedQuestionnaireIds.add(id);
            highlightColorById.put(id, colorIndex);
        }
    }
    public static void clearHighlightedQuestionnaireIds() { highlightedQuestionnaireIds.clear(); }

    public static Integer getHighlightColorIndex(String id) {
        if (id == null || id.isBlank()) return null;
        return highlightColorById.get(id);
    }

    public static void setScenarioOrder(String id, int order) {
        if (id == null || id.isBlank()) return;
        scenarioOrderById.put(id, order);
    }

    public static Integer getScenarioOrder(String id) {
        if (id == null || id.isBlank()) return null;
        return scenarioOrderById.get(id);
    }

    public static com.aac.kpi.model.RulesConfig getRulesConfig() { return rulesConfig; }
    public static void setRulesConfig(com.aac.kpi.model.RulesConfig cfg) {
        if (cfg != null) rulesConfig = cfg;
    }
}
