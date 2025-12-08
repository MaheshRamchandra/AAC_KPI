package com.aac.kpi.service;

import com.aac.kpi.model.ScenarioTestCase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ScenarioReader {
    private static final Set<String> SHEET_NAMES = new LinkedHashSet<>(List.of(
            "Scenarios", "Scenario", "Test Cases", "Test Case", "Scenario Config", "Scenario Configuration"
    ));

    private static final DataFormatter FORMATTER = new DataFormatter(Locale.ENGLISH);

    public static List<ScenarioTestCase> readScenarios(File file) throws IOException {
        return readScenarios(file, null);
    }

    public static List<ScenarioTestCase> readScenarios(File file, String sheetName) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = null;
            if (sheetName != null && !sheetName.isBlank()) {
                sheet = findSheetByName(workbook, sheetName);
            }
            if (sheet == null) {
                sheet = findSheet(workbook);
            }
            if (sheet == null) return List.of();
            HeaderIndex headerIndex = buildHeaderIndex(sheet);
            List<ScenarioTestCase> scenarios = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Set<Integer> usedColumns = new HashSet<>();
                ScenarioTestCase scenario = new ScenarioTestCase();
                scenario.setId(getValue(row, headerIndex, usedColumns, "id"));
                scenario.setWorkItemType(getValue(row, headerIndex, usedColumns, "workitemtype"));
                scenario.setTitle(getValue(row, headerIndex, usedColumns, "title"));
                scenario.setTestStep(getValue(row, headerIndex, usedColumns, "teststep"));
                scenario.setStepAction(getValue(row, headerIndex, usedColumns, "stepaction", "teststepdetail", "step"));
                scenario.setStepExpected(getValue(row, headerIndex, usedColumns, "stepexpected", "expected"));
                scenario.setKpiType(getValue(row, headerIndex, usedColumns, "kpi", "kpitype", "kpi_type", "kpi type"));
                scenario.setNumberOfSeniors(getValue(row, headerIndex, usedColumns, "numberofseniors", "seniors"));
                scenario.setCfs(getValue(row, headerIndex, usedColumns, "cfs"));
                scenario.setModeOfEvent(getValue(row, headerIndex, usedColumns, "modeofevent", "mode of event", "event_session_mode1"));
                scenario.setAapSessionDate(getValue(row, headerIndex, usedColumns, "aapsessiondate", "latestaapsessiondate", "event_session_start_date1"));
                scenario.setNumberOfAapAttendance(getValue(row, headerIndex, usedColumns, "noofaapattendanceinperson", "noofaapattendance", "aapattendance"));
                scenario.setAttendedIndicator(getValue(row, headerIndex, usedColumns, "is_attended", "attended", "attendedindicator"));
                scenario.setTotalRegistrations(getValue(row, headerIndex, usedColumns, "nooftotalregistration", "totalregistration", "registrations"));
                scenario.setWithinBoundary(getValue(row, headerIndex, usedColumns, "withinoroutofserviceboundary", "boundary"));
                scenario.setPurposeOfContact(getValue(row, headerIndex, usedColumns, "purposeofcontact", "encounter_purpose"));
                scenario.setEncounterStart(getValue(row, headerIndex, usedColumns, "encounter_start"));
                scenario.setDateOfContact(getExactValue(row, headerIndex, usedColumns, "dateofcontact", "contactdate"));
                scenario.setAge(getValue(row, headerIndex, usedColumns, "age"));
                scenario.setContactLogs(getValue(row, headerIndex, usedColumns, "contactlogs", "contactlog", "contact logs", "encounters", "contactlogs(encounters)"));
                scenario.setRemarks(getValue(row, headerIndex, usedColumns, "remarks", "others"));
                scenario.setPatientBirthdate(getValue(row, headerIndex, usedColumns, "patient_birthdate", "birthdate", "dob"));
                scenario.setReportingMonth(getValue(row, headerIndex, usedColumns, "extension_reporting_month", "reporting_month"));
                scenario.setReportDate(getExactValue(row, headerIndex, usedColumns, "date", "report_date", "reportdate", "reporting_date"));
                scenario.setSocialRiskFactorScore(getValue(row, headerIndex, usedColumns, "social_risk_factor_score", "socialriskfactorscore", "socialriskfactor", "rf"));
                scenario.setBuddyingProgrammePeriodStart(getValue(row, headerIndex, usedColumns, "resident_buddying_programme_period_start", "buddying_programme_period_start"));
                scenario.setBuddyingProgrammePeriodEnd(getValue(row, headerIndex, usedColumns, "resident_buddying_programme_period_end", "buddying_programme_period_end"));
                scenario.setBefriendingProgrammePeriodStart(getValue(row, headerIndex, usedColumns, "resident_befriending_programme_period_start", "befriending_programme_period_start"));
                scenario.setBefriendingProgrammePeriodEnd(getValue(row, headerIndex, usedColumns, "resident_befriending_programme_period_end", "befriending_programme_period_end"));
                scenario.setExtraFields(captureExtraFields(row, headerIndex, usedColumns));
                if (isBlank(scenario.getReportDate())) {
                    String fromExtras = firstExtraWithKey(scenario.getExtraFields(), "date", "report_date", "reportdate", "reporting_date");
                    if (!isBlank(fromExtras)) {
                        scenario.setReportDate(fromExtras);
                    }
                }
                if (isBlank(scenario.getDateOfContact()) && !isBlank(scenario.getEncounterStart())) {
                    scenario.setDateOfContact(scenario.getEncounterStart());
                }
                if (isEmptyRow(scenario)) continue;
                if (isBlank(scenario.getKpiType()) && sheetName != null && !sheetName.isBlank()) {
                    scenario.setKpiType(inferKpiTypeFromSheet(sheetName));
                }
                scenarios.add(scenario);
            }
            return scenarios;
        }
    }

    public static List<String> listSheetNames(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                names.add(workbook.getSheetAt(i).getSheetName());
            }
            return names;
        }
    }

    public static String guessPreferredSheet(List<String> sheetNames) {
        if (sheetNames == null || sheetNames.isEmpty()) return "";
        for (String preferred : SHEET_NAMES) {
            for (String actual : sheetNames) {
                if (preferred.equalsIgnoreCase(actual)) {
                    return actual;
                }
            }
        }
        return sheetNames.get(0);
    }

    private static boolean isEmptyRow(ScenarioTestCase scenario) {
        // Treat a row as a usable scenario if any of the core
        // Scenario Builder columns has data, or if the Excel row
        // contains at least one extra column value.
        boolean coreEmpty = isBlank(scenario.getNumberOfSeniors())
                && isBlank(scenario.getCfs())
                && isBlank(scenario.getModeOfEvent())
                && isBlank(scenario.getAapSessionDate())
                && isBlank(scenario.getNumberOfAapAttendance())
                && isBlank(scenario.getWithinBoundary())
                && isBlank(scenario.getPurposeOfContact())
                && isBlank(scenario.getDateOfContact())
                && isBlank(scenario.getAge())
                && isBlank(scenario.getContactLogs())
                && isBlank(scenario.getKpiType())
                && isBlank(scenario.getTotalRegistrations())
                && isBlank(scenario.getAttendedIndicator())
                && isBlank(scenario.getEncounterStart())
                && isBlank(scenario.getPatientBirthdate())
                && isBlank(scenario.getReportingMonth())
                && isBlank(scenario.getReportDate())
                && isBlank(scenario.getSocialRiskFactorScore())
                && isBlank(scenario.getBuddyingProgrammePeriodStart())
                && isBlank(scenario.getBuddyingProgrammePeriodEnd())
                && isBlank(scenario.getBefriendingProgrammePeriodStart())
                && isBlank(scenario.getBefriendingProgrammePeriodEnd());
        boolean extrasEmpty = scenario.getExtraFields() == null || scenario.getExtraFields().isEmpty();
        return coreEmpty && extrasEmpty;
    }

    private static Sheet findSheet(XSSFWorkbook workbook) {
        for (String name : SHEET_NAMES) {
            Sheet sheet = workbook.getSheet(name);
            if (sheet != null) return sheet;
        }
        return workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
    }

    private static Sheet findSheetByName(XSSFWorkbook workbook, String sheetName) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet s = workbook.getSheetAt(i);
            if (s.getSheetName().equalsIgnoreCase(sheetName)) {
                return s;
            }
        }
        return null;
    }

    private static String inferKpiTypeFromSheet(String sheetName) {
        if (sheetName == null) return "";
        String lower = sheetName.toLowerCase(Locale.ENGLISH);
        if (lower.contains("robust")) return "Robust";
        if (lower.contains("frail")) return "Frail";
        if (lower.contains("buddy")) return "Buddying";
        if (lower.contains("befriend")) return "Befriending";
        return "";
    }

    private static HeaderIndex buildHeaderIndex(Sheet sheet) {
        HeaderIndex headerIndex = new HeaderIndex();
        Row header = sheet.getRow(0);
        if (header == null) return headerIndex;
        for (int i = 0; i < header.getLastCellNum(); i++) {
            String raw = getString(header, i);
            if (raw.isBlank()) continue;
            headerIndex.byKey.put(normalize(raw), i);
            headerIndex.rawByIndex.put(i, raw.trim());
        }
        return headerIndex;
    }

    private static String getValue(Row row, HeaderIndex headerIndex, Set<Integer> usedColumns, String... keys) {
        for (String key : keys) {
            String normKey = normalize(key);
            // First try exact normalized header match
            Integer idx = headerIndex.byKey.get(normKey);
            if (idx != null) {
                String value = getString(row, idx);
                if (!value.isBlank()) {
                    usedColumns.add(idx);
                    return value;
                }
            }
            // Fallback: allow headers that contain the search token, so
            // columns like "AAP Session Date (Listed date is the latest AAP attended)"
            // still match the simpler "AAP Session Date" key.
            for (Map.Entry<String, Integer> entry : headerIndex.byKey.entrySet()) {
                String headerKey = entry.getKey();
                if (headerKey.contains(normKey) || normKey.contains(headerKey)) {
                    String value = getString(row, entry.getValue());
                    if (!value.isBlank()) {
                        usedColumns.add(entry.getValue());
                        return value;
                    }
                }
            }
        }
        return "";
    }

    private static String getExactValue(Row row, HeaderIndex headerIndex, Set<Integer> usedColumns, String... keys) {
        for (String key : keys) {
            String normKey = normalize(key);
            Integer idx = headerIndex.byKey.get(normKey);
            if (idx == null) continue;
            if (usedColumns.contains(idx)) continue;
            String value = getString(row, idx);
            if (!value.isBlank()) {
                usedColumns.add(idx);
                return value;
            }
        }
        return "";
    }

    private static String firstExtraWithKey(Map<String, String> extras, String... keys) {
        if (extras == null || extras.isEmpty()) return "";
        for (String key : keys) {
            String normKey = normalize(key);
            for (Map.Entry<String, String> entry : extras.entrySet()) {
                String entryNorm = normalize(entry.getKey());
                if (entryNorm.equals(normKey) || entryNorm.contains(normKey) || normKey.contains(entryNorm)) {
                    if (!entry.getValue().isBlank()) {
                        return entry.getValue();
                    }
                }
            }
        }
        return "";
    }

    private static Map<String, String> captureExtraFields(Row row, HeaderIndex headerIndex, Set<Integer> usedColumns) {
        Map<String, String> extras = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : headerIndex.rawByIndex.entrySet()) {
            int idx = entry.getKey();
            if (usedColumns.contains(idx)) continue;
            String value = getString(row, idx);
            if (value.isBlank()) continue;
            extras.put(entry.getValue(), value);
        }
        return extras;
    }

    private static String getString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return FORMATTER.formatCellValue(cell).trim();
    }

    private static String normalize(String input) {
        if (input == null) return "";
        return input.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", "");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static final class HeaderIndex {
        private final Map<String, Integer> byKey = new HashMap<>();
        private final Map<Integer, String> rawByIndex = new HashMap<>();
    }
}
