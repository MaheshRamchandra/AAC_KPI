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
        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = findSheet(workbook);
            if (sheet == null) return List.of();
            Map<String, Integer> headerIndex = buildHeaderIndex(sheet);
            List<ScenarioTestCase> scenarios = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                ScenarioTestCase scenario = new ScenarioTestCase();
                scenario.setId(getValue(row, headerIndex, "id"));
                scenario.setWorkItemType(getValue(row, headerIndex, "workitemtype"));
                scenario.setTitle(getValue(row, headerIndex, "title"));
                scenario.setTestStep(getValue(row, headerIndex, "teststep"));
                scenario.setStepAction(getValue(row, headerIndex, "stepaction", "teststepdetail", "step"));
                scenario.setStepExpected(getValue(row, headerIndex, "stepexpected", "expected"));
                scenario.setNumberOfSeniors(getValue(row, headerIndex, "numberofseniors", "seniors"));
                scenario.setCfs(getValue(row, headerIndex, "cfs"));
                scenario.setModeOfEvent(getValue(row, headerIndex, "modeofevent", "mode of event"));
                scenario.setAapSessionDate(getValue(row, headerIndex, "aapsessiondate", "latestaapsessiondate"));
                scenario.setNumberOfAapAttendance(getValue(row, headerIndex, "noofaapattendanceinperson", "noofaapattendance", "aapattendance"));
                scenario.setWithinBoundary(getValue(row, headerIndex, "withinoroutofserviceboundary", "boundary"));
                scenario.setPurposeOfContact(getValue(row, headerIndex, "purposeofcontact"));
                scenario.setDateOfContact(getValue(row, headerIndex, "dateofcontact", "contactdate"));
                scenario.setAge(getValue(row, headerIndex, "age"));
                scenario.setRemarks(getValue(row, headerIndex, "remarks", "others"));
                if (!isEmptyRow(scenario)) scenarios.add(scenario);
            }
            return scenarios;
        }
    }

    private static boolean isEmptyRow(ScenarioTestCase scenario) {
        // Treat a row as a usable scenario if any of the core
        // Scenario Builder columns has data. This allows sheets
        // without ID/Title to still be recognised as test cases.
        return isBlank(scenario.getNumberOfSeniors())
                && isBlank(scenario.getCfs())
                && isBlank(scenario.getModeOfEvent())
                && isBlank(scenario.getAapSessionDate())
                && isBlank(scenario.getNumberOfAapAttendance())
                && isBlank(scenario.getWithinBoundary())
                && isBlank(scenario.getPurposeOfContact())
                && isBlank(scenario.getDateOfContact())
                && isBlank(scenario.getAge());
    }

    private static Sheet findSheet(XSSFWorkbook workbook) {
        for (String name : SHEET_NAMES) {
            Sheet sheet = workbook.getSheet(name);
            if (sheet != null) return sheet;
        }
        return workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
    }

    private static Map<String, Integer> buildHeaderIndex(Sheet sheet) {
        Map<String, Integer> headerIndex = new HashMap<>();
        Row header = sheet.getRow(0);
        if (header == null) return headerIndex;
        for (int i = 0; i < header.getLastCellNum(); i++) {
            String raw = getString(header, i);
            if (raw.isBlank()) continue;
            headerIndex.put(normalize(raw), i);
        }
        return headerIndex;
    }

    private static String getValue(Row row, Map<String, Integer> headerIndex, String... keys) {
        for (String key : keys) {
            String normKey = normalize(key);
            // First try exact normalized header match
            Integer idx = headerIndex.get(normKey);
            if (idx != null) {
                String value = getString(row, idx);
                if (!value.isBlank()) return value;
            }
            // Fallback: allow headers that contain the search token, so
            // columns like "AAP Session Date (Listed date is the latest AAP attended)"
            // still match the simpler "AAP Session Date" key.
            for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
                String headerKey = entry.getKey();
                if (headerKey.contains(normKey) || normKey.contains(headerKey)) {
                    String value = getString(row, entry.getValue());
                    if (!value.isBlank()) return value;
                }
            }
        }
        return "";
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
}
