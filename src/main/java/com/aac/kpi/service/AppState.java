package com.aac.kpi.service;

import com.aac.kpi.service.MasterDataService.MasterData;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AppState {
    private static volatile File currentExcelFile;
    private static volatile boolean dirty;
    private static volatile KpiConfig kpiConfig = new KpiConfig();
    private static volatile int volunteerPractitionerCount = 0;
    private static volatile MasterData masterData;
    private static volatile String jsonConverterJarPath = "lib/KPITool-1.0-SNAPSHOT-jar-with-dependencies.jar";
    private static volatile String eventReportLabel = "";

    private static final Set<String> highlightedPatientIds = new LinkedHashSet<>();
    private static final Set<String> highlightedEventSessionCompositionIds = new LinkedHashSet<>();
    private static final Set<String> highlightedPractitionerIds = new LinkedHashSet<>();
    private static final Set<String> highlightedEncounterIds = new LinkedHashSet<>();
    private static final Set<String> highlightedQuestionnaireIds = new LinkedHashSet<>();

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

    public static String getEventReportLabel() { return eventReportLabel; }
    public static void setEventReportLabel(String label) { eventReportLabel = label == null ? "" : label; }

    public static Set<String> getHighlightedPatientIds() { return Collections.unmodifiableSet(highlightedPatientIds); }
    public static void addHighlightedPatientId(String id) {
        if (id != null && !id.isBlank()) highlightedPatientIds.add(id);
    }
    public static void clearHighlightedPatientIds() { highlightedPatientIds.clear(); }

    public static Set<String> getHighlightedEventSessionCompositionIds() {
        return Collections.unmodifiableSet(highlightedEventSessionCompositionIds);
    }
    public static void addHighlightedEventSessionCompositionId(String id) {
        if (id != null && !id.isBlank()) highlightedEventSessionCompositionIds.add(id);
    }
    public static void clearHighlightedEventSessionCompositionIds() { highlightedEventSessionCompositionIds.clear(); }

    public static Set<String> getHighlightedPractitionerIds() { return Collections.unmodifiableSet(highlightedPractitionerIds); }
    public static void addHighlightedPractitionerId(String id) {
        if (id != null && !id.isBlank()) highlightedPractitionerIds.add(id);
    }
    public static void clearHighlightedPractitionerIds() { highlightedPractitionerIds.clear(); }

    public static Set<String> getHighlightedEncounterIds() { return Collections.unmodifiableSet(highlightedEncounterIds); }
    public static void addHighlightedEncounterId(String id) {
        if (id != null && !id.isBlank()) highlightedEncounterIds.add(id);
    }
    public static void clearHighlightedEncounterIds() { highlightedEncounterIds.clear(); }

    public static Set<String> getHighlightedQuestionnaireIds() {
        return Collections.unmodifiableSet(highlightedQuestionnaireIds);
    }
    public static void addHighlightedQuestionnaireId(String id) {
        if (id != null && !id.isBlank()) highlightedQuestionnaireIds.add(id);
    }
    public static void clearHighlightedQuestionnaireIds() { highlightedQuestionnaireIds.clear(); }
}
