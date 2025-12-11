package com.aac.kpi.converter;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.aac.kpi.converter.ReportConstants.*;

public class ExcelOperations {
    private String filePath;
    private Workbook workbook;

    static {
        // Allow slightly denser compression to avoid false zip-bomb hits on style-heavy workbooks.
        ZipSecureFile.setMinInflateRatio(0.005);
    }

    public ExcelOperations(String filePath) {
        this.filePath = filePath;
    }

    private Sheet getSheet(Workbook workbook, int sheetNumber) throws IOException {
        this.workbook = workbook;
        Sheet sheet = workbook.getSheetAt(sheetNumber);
        return sheet;
    }

    public HashMap<String, HashMap<String, String>> getReportsMap(String typeOfReport, int numAacReports, int startRow) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = getSheet(workbook, 0); // common sheet

        String reportMapPrefix;
        switch (typeOfReport) {
            case AAC_TYPE:
                reportMapPrefix = "aac_report_";
                break;
            case RESIDENT_TYPE:
                reportMapPrefix = "resident_report_";
                break;
            case VOLUNTEER_ATTENDANCE_TYPE:
                reportMapPrefix = "volunteer_attendance_report_";
                break;
            case EVENT_TYPE:
                reportMapPrefix = "event_report_";
                break;
            case ORGANIZATION_TYPE:
                reportMapPrefix = "organization_report_";
                break;
            case LOCATION_TYPE:
                reportMapPrefix = "location_report_";
                break;
            default:
                reportMapPrefix = "";
        }

        HashMap<String, HashMap<String, String>> reportMap = new HashMap<String, HashMap<String, String>>();
        for (int i = 0; i < numAacReports; i++) {
            String reportMapKey = reportMapPrefix + (i + 1);

            HashMap<String, String> valueMap = new HashMap<String, String>();
            Row keyRow = sheet.getRow(startRow);
            Row valueRow = sheet.getRow((startRow + 1) + i);

            int valueRowIndex = 0;
            for (Cell cell : keyRow) {
                String rowKey = getCellValueAsString(cell);
                String rowValue = getCellValueAsString(valueRow.getCell(valueRowIndex));
                valueMap.put(rowKey, rowValue);
                valueRowIndex++;
            }

            reportMap.put(reportMapKey, valueMap);
        }

        return reportMap;
    }

    /**
     * Auto-detect the number of rows for each report table in the common sheet by
     * scanning the first column for names like aac_report_<anyType>_<index> and
     * taking the highest index found per report family. This tolerates blank rows
     * between blocks.
     */
    public ReportCounts detectCountsFromCommonSheet() throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = getSheet(workbook, 0); // common sheet

            int aacMax = 0;
            int residentMax = 0;
            int volunteerMax = 0;
            int eventMax = 0;
            int orgMax = 0;
            int locationMax = 0;

            // Capture any digits at the end, with or without an extra type token.
            Pattern aacPattern = Pattern.compile("^aac_report_.*?(\\d+)$", Pattern.CASE_INSENSITIVE);
            Pattern residentPattern = Pattern.compile("^resident_report_.*?(\\d+)$", Pattern.CASE_INSENSITIVE);
            Pattern volunteerPattern = Pattern.compile("^volunteer_attendance_report_.*?(\\d+)$", Pattern.CASE_INSENSITIVE);
            Pattern eventPattern = Pattern.compile("^event_report_.*?(\\d+)$", Pattern.CASE_INSENSITIVE);
            Pattern orgPattern = Pattern.compile("^organization_report_.*?(\\d+)$", Pattern.CASE_INSENSITIVE);
            Pattern locationPattern = Pattern.compile("^location_report_.*?(\\d+)$", Pattern.CASE_INSENSITIVE);

            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String value = getCellValueAsString(row.getCell(0));
                if (value == null || value.trim().isEmpty()) {
                    continue;
                }
                String trimmed = value.trim();
                aacMax = Math.max(aacMax, extractIndex(trimmed, aacPattern));
                residentMax = Math.max(residentMax, extractIndex(trimmed, residentPattern));
                volunteerMax = Math.max(volunteerMax, extractIndex(trimmed, volunteerPattern));
                eventMax = Math.max(eventMax, extractIndex(trimmed, eventPattern));
                orgMax = Math.max(orgMax, extractIndex(trimmed, orgPattern));
                locationMax = Math.max(locationMax, extractIndex(trimmed, locationPattern));
            }

            return new ReportCounts(aacMax, residentMax, volunteerMax, eventMax, orgMax, locationMax);
        }
    }

    private int extractIndex(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (Cell cell : row) {
            String value = getCellValueAsString(cell);
            if (cell != null && cell.getCellType() != org.apache.poi.ss.usermodel.CellType.BLANK && value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public HashMap<String, HashMap<String, String>> getMasterDataMap(String typeOfSheet) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(fis);

        int sheetNumber;
        switch (typeOfSheet) {
            case EVENT_SESSIONS_TYPE:
                sheetNumber = EVENT_SESSIONS_SHEET_NO;
                break;
            case EVENT_SESSIONS_NRIC_TYPE:
                sheetNumber = EVENT_SESSIONS_NRIC_SHEET_NO;
                break;
            case PATIENT_TYPE:
                sheetNumber = PATIENT_SHEET_NO;
                break;
            case PRACTITIONER_TYPE:
                sheetNumber = PRACTITIONER_SHEET_NO;
                break;
            case ENCOUNTER_TYPE:
                sheetNumber = ENCOUNTER_SHEET_NO;
                break;
            case QUESTIONNAIRE_TYPE:
                sheetNumber = QUESTIONNAIRE_SHEET_NO;
                break;
            default:
                sheetNumber = 0;
        }

        Sheet sheet = getSheet(workbook, sheetNumber);

        HashMap<String, HashMap<String, String>> mastersMap = new HashMap<String, HashMap<String, String>>();

        int startRow = 0;
        for (int i = 0; i < sheet.getLastRowNum(); i++) {
            HashMap<String, String> valueMap = new HashMap<String, String>();
            Row keyRow = sheet.getRow(startRow);
            Row valueRow = sheet.getRow((startRow + 1) + i);

            int valueRowIndex = 0;
            for (Cell cell : keyRow) {
                String rowKey = getCellValueAsString(cell);
                //System.out.println(rowKey);
                String rowValue = getCellValueAsString(valueRow.getCell(valueRowIndex));
                //System.out.println(rowValue);
                valueMap.put(rowKey, rowValue);
                valueRowIndex++;
            }

            String practitionersMapKey = getCellValueAsString(valueRow.getCell(0));
            mastersMap.put(practitionersMapKey, valueMap);
        }

        return mastersMap;
    }


    public List<HashMap<String, String>> getExcelAsMap() throws IOException {

        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(fis);
        int sheetCount = workbook.getNumberOfSheets();

        List<HashMap<String, String>> completeSheetData = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> singleRowData = null;

        for (int i = 0; i < workbook.getSheetAt(0).getLastRowNum(); i++) {
            singleRowData = new HashMap<String, String>();
            for (int j = 0; j < sheetCount; j++) {
                Sheet sheet = getSheet(workbook, j);

                Row keyRow = sheet.getRow(0);
                Row valueRow = sheet.getRow(i + 1);
                int index = 0;

                if (valueRow != null) {
                    for (Cell mycell : keyRow) {
                        String rowKey = getCellValueAsString(mycell);
                        String rowValue = getCellValueAsString(valueRow.getCell(index));
                        System.out.println(rowKey);
                        singleRowData.put(rowKey, rowValue);
                        index++;
                    }
                }
            }
            completeSheetData.add(singleRowData);
        }
        workbook.close();
        fis.close();
        return completeSheetData;
    }

    public String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA, BLANK -> null;
            default -> cell.getStringCellValue();
        };
    }
}
