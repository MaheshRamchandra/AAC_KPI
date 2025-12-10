package com.aac.kpi.service;

import com.aac.kpi.model.ScenarioTestCase;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ScenarioWriter {
    private ScenarioWriter() {}

    /**
     * Write the provided scenarios back to an Excel workbook. If the destination
     * file already exists, other sheets are preserved and only the target sheet
     * is replaced.
     */
    public static File write(File destination, List<ScenarioTestCase> scenarios, String sheetName) throws IOException {
        if (destination == null) {
            throw new IllegalArgumentException("Destination file is required");
        }
        File target = ensureXlsx(destination);
        List<ScenarioTestCase> rows = scenarios == null ? List.of() : scenarios;

        try (XSSFWorkbook workbook = target.exists() ? new XSSFWorkbook(new FileInputStream(target))
                : new XSSFWorkbook()) {
            String title = (sheetName == null || sheetName.isBlank()) ? "Scenarios" : sheetName;
            int idx = findSheetIndex(workbook, title);
            if (idx >= 0) {
                workbook.removeSheetAt(idx);
            }
            Sheet sheet = workbook.createSheet(title);
            List<ColumnSpec> columns = buildColumns(rows);

            Row header = sheet.createRow(0);
            for (int c = 0; c < columns.size(); c++) {
                header.createCell(c).setCellValue(columns.get(c).header());
            }
            for (int r = 0; r < rows.size(); r++) {
                ScenarioTestCase sc = rows.get(r);
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < columns.size(); c++) {
                    String value = columns.get(c).value(sc);
                    row.createCell(c).setCellValue(value == null ? "" : value);
                }
            }
            for (int c = 0; c < columns.size(); c++) {
                sheet.autoSizeColumn(c);
            }
            try (FileOutputStream fos = new FileOutputStream(target)) {
                workbook.write(fos);
            }
        }
        return target;
    }

    private static List<ColumnSpec> buildColumns(List<ScenarioTestCase> scenarios) {
        List<ColumnSpec> cols = new ArrayList<>();
        cols.add(new ColumnSpec("Id", sc -> nvl(sc.getId())));
        cols.add(new ColumnSpec("WorkItemType", sc -> nvl(sc.getWorkItemType())));
        cols.add(new ColumnSpec("Title", sc -> nvl(sc.getTitle())));
        cols.add(new ColumnSpec("TestStep", sc -> nvl(sc.getTestStep())));
        cols.add(new ColumnSpec("StepAction", sc -> nvl(sc.getStepAction())));
        cols.add(new ColumnSpec("StepExpected", sc -> nvl(sc.getStepExpected())));
        cols.add(new ColumnSpec("KPI Type", sc -> nvl(sc.getKpiType())));
        cols.add(new ColumnSpec("Number of seniors", sc -> nvl(sc.getNumberOfSeniors())));
        cols.add(new ColumnSpec("CFS", sc -> nvl(sc.getCfs())));
        cols.add(new ColumnSpec("Mode of event", sc -> nvl(sc.getModeOfEvent())));
        cols.add(new ColumnSpec("AAP Session Date", sc -> nvl(sc.getAapSessionDate())));
        cols.add(new ColumnSpec("No of AAP attendance in person", sc -> nvl(sc.getNumberOfAapAttendance())));
        cols.add(new ColumnSpec("Is Attended", sc -> nvl(sc.getAttendedIndicator())));
        cols.add(new ColumnSpec("Total registration", sc -> nvl(sc.getTotalRegistrations())));
        cols.add(new ColumnSpec("Within or out of boundary", sc -> nvl(sc.getWithinBoundary())));
        cols.add(new ColumnSpec("Purpose of contact", sc -> nvl(sc.getPurposeOfContact())));
        cols.add(new ColumnSpec("Date of contact", sc -> nvl(sc.getDateOfContact())));
        cols.add(new ColumnSpec("Encounter Start", sc -> nvl(sc.getEncounterStart())));
        cols.add(new ColumnSpec("Age", sc -> nvl(sc.getAge())));
        cols.add(new ColumnSpec("Remarks / Others", sc -> nvl(sc.getRemarks())));
        cols.add(new ColumnSpec("Contact logs (encounters)", sc -> nvl(sc.getContactLogs())));
        cols.add(new ColumnSpec("Patient Birthdate", sc -> nvl(sc.getPatientBirthdate())));
        cols.add(new ColumnSpec("Reporting Month", sc -> nvl(sc.getReportingMonth())));
        cols.add(new ColumnSpec("Date", sc -> nvl(sc.getReportDate())));
        cols.add(new ColumnSpec("Social Risk Factor Score", sc -> nvl(sc.getSocialRiskFactorScore())));
        cols.add(new ColumnSpec("Resident Buddying Programme Period Start",
                sc -> nvl(sc.getBuddyingProgrammePeriodStart())));
        cols.add(new ColumnSpec("Resident Buddying Programme Period End",
                sc -> nvl(sc.getBuddyingProgrammePeriodEnd())));
        cols.add(new ColumnSpec("Resident Befriending Programme Period Start",
                sc -> nvl(sc.getBefriendingProgrammePeriodStart())));
        cols.add(new ColumnSpec("Resident Befriending Programme Period End",
                sc -> nvl(sc.getBefriendingProgrammePeriodEnd())));

        Set<String> extraColumns = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (ScenarioTestCase sc : scenarios) {
            if (sc != null && sc.getExtraFields() != null) {
                extraColumns.addAll(sc.getExtraFields().keySet());
            }
        }
        for (String extra : extraColumns) {
            if (extra == null || extra.isBlank()) continue;
            cols.add(new ColumnSpec(extra, sc -> sc != null && sc.getExtraFields() != null
                    ? nvl(sc.getExtraFields().get(extra))
                    : ""));
        }

        cols.add(new ColumnSpec("Column Overrides", ScenarioWriter::formatOverrides));
        return cols;
    }

    private static int findSheetIndex(XSSFWorkbook workbook, String name) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (workbook.getSheetAt(i).getSheetName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private static File ensureXlsx(File file) {
        String name = file.getName().toLowerCase(Locale.ENGLISH);
        if (name.endsWith(".xlsx")) {
            return file;
        }
        return new File(file.getPath() + ".xlsx");
    }

    private static String nvl(String text) {
        return text == null ? "" : text;
    }

    private static String formatOverrides(ScenarioTestCase sc) {
        if (sc == null || sc.getColumnOverrides() == null || sc.getColumnOverrides().isEmpty()) {
            return "";
        }
        return sc.getColumnOverrides().stream()
                .map(ScenarioTestCase.ColumnOverride::toString)
                .collect(Collectors.joining("; "));
    }

    private record ColumnSpec(String header, Function<ScenarioTestCase, String> extractor) {
        String value(ScenarioTestCase scenario) {
            return extractor.apply(scenario);
        }
    }
}
