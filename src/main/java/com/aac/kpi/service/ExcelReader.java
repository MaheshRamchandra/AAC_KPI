package com.aac.kpi.service;

import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelReader {

    public static List<Patient> readPatients(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Patient (Master)");
            if (sheet == null) sheet = wb.getSheet("Patient_Master");
            if (sheet == null) return List.of();

            List<Patient> list = new ArrayList<>();
            // Build header index map for flexible column positions
            Row header = sheet.getRow(0);
            java.util.Map<String, Integer> idx = new java.util.HashMap<>();
            if (header != null) {
                for (int i = 0; i < header.getLastCellNum(); i++) {
                    String name = getStr(header, i);
                    if (!name.isEmpty()) idx.put(name, i);
                }
            }

            Iterator<Row> it = sheet.rowIterator();
            if (it.hasNext()) it.next(); // skip header
            while (it.hasNext()) {
                Row row = it.next();
                Patient p = new Patient();
                p.setPatientId(getStr(row, idx.getOrDefault("patient_id", 0)));
                p.setPatientIdentifierValue(getStr(row, idx.getOrDefault("patient_identifier_value", 1)));
                p.setPatientBirthdate(getStr(row, idx.getOrDefault("patient_birthdate", 2)));
                p.setPatientPostalCode(getStr(row, idx.getOrDefault("patient_postalcode", 3)));
                p.setAttendedEventReferences(getStr(row, idx.getOrDefault("attended_event_references", 4)));
                p.setWorkingRemarks(getStr(row, idx.getOrDefault("Working Remarks", 5)));
                try { p.setGroup((int) getNum(row, idx.getOrDefault("Group", 6))); } catch (Exception ignored) {}
                p.setType(getStr(row, idx.getOrDefault("Type", 7)));
                p.setAac(getStr(row, idx.getOrDefault("AAC", 8)));
                // Optional extended columns
                try { p.setCfs((int) getNum(row, idx.getOrDefault("CFS", 9))); } catch (Exception ignored) {}
                try { p.setSocialRiskFactor((int) getNum(row, idx.getOrDefault("RF", 10))); } catch (Exception ignored) {}
                try { p.setKpiType(getStr(row, idx.getOrDefault("KPI Type", 11))); } catch (Exception ignored) {}
                try { p.setKpiGroup(getStr(row, idx.getOrDefault("KPI Group", 12))); } catch (Exception ignored) {}
                if (p.getPatientId() != null && !p.getPatientId().isBlank()) list.add(p);
            }
            return list;
        }
    }

    public static List<com.aac.kpi.model.Encounter> readEncounters(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Encounter (Master)");
            if (sheet == null) sheet = wb.getSheet("Encounter_Master");
            if (sheet == null) return List.of();
            List<com.aac.kpi.model.Encounter> list = new ArrayList<>();
            Iterator<Row> it = sheet.rowIterator();
            if (it.hasNext()) it.next();
            while (it.hasNext()) {
                Row row = it.next();
                com.aac.kpi.model.Encounter e = new com.aac.kpi.model.Encounter();
                e.setEncounterId(getStr(row, 0));
                e.setEncounterStatus(getStr(row, 1));
                e.setEncounterDisplay(getStr(row, 2));
                e.setEncounterStart(getStr(row, 3));
                e.setEncounterPurpose(getStr(row, 4));
                e.setEncounterContactedStaffName(getStr(row, 5));
                e.setEncounterReferredBy(getStr(row, 6));
                e.setEncounterPatientReference(getStr(row, 7));
                if (e.getEncounterId()!=null && !e.getEncounterId().isBlank()) list.add(e);
            }
            return list;
        }
    }

    public static List<com.aac.kpi.model.QuestionnaireResponse> readQuestionnaires(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("QuestionnaireResponse (Master)");
            if (sheet == null) sheet = wb.getSheet("QuestionnaireResponse_Master");
            if (sheet == null) return List.of();
            List<com.aac.kpi.model.QuestionnaireResponse> list = new ArrayList<>();
            Iterator<Row> it = sheet.rowIterator();
            if (it.hasNext()) it.next();
            while (it.hasNext()) {
                Row row = it.next();
                com.aac.kpi.model.QuestionnaireResponse q = new com.aac.kpi.model.QuestionnaireResponse();
                q.setQuestionnaireId(getStr(row, 0));
                q.setQuestionnaireStatus(getStr(row, 1));
                q.setQ1(getDateStr(row, 2));
                q.setQ2(getStr(row, 3));
                q.setQ3(getDateStr(row, 4));
                q.setQ4(getStr(row, 5));
                q.setQ5(getDateStr(row, 6));
                q.setQ6(getStr(row, 7));
                q.setQ7(getDateStr(row, 8));
                q.setQ8(getStr(row, 9));
                q.setQ9(getDateStr(row, 10));
                q.setQ10(getStr(row, 11));
                q.setQuestionnairePatientReference(getStr(row, 12));
                if (q.getQuestionnaireId()!=null && !q.getQuestionnaireId().isBlank()) list.add(q);
            }
            return list;
        }
    }

    public static List<com.aac.kpi.model.CommonRow> readCommon(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Common");
            if (sheet == null) return List.of();
            List<com.aac.kpi.model.CommonRow> list = new ArrayList<>();
            Iterator<Row> it = sheet.rowIterator();
            if (it.hasNext()) it.next();
            while (it.hasNext()) {
                Row row = it.next();
                com.aac.kpi.model.CommonRow c = new com.aac.kpi.model.CommonRow();
                c.setCompositionId(getStr(row, 0));
                c.setVersionId((int) getNum(row, 1));
                c.setLastUpdated(getStr(row, 2));
                c.setMetaCode(getStr(row, 3));
                c.setReportingMonth(getStr(row, 4));
                c.setTotalOperatingDays((int) getNum(row, 5));
                c.setTotalClients((int) getNum(row, 6));
                c.setStatus(getStr(row, 7));
                c.setAuthorValue(getStr(row, 8));
                c.setAuthorDisplay(getStr(row, 9));
                c.setPatientReference(getStr(row, 10));
                c.setEncounterReferences(getStr(row, 11));
                c.setQuestionnaireReference(getStr(row, 12));
                c.setAttendedEventReferences(getStr(row, 13));
                if (c.getCompositionId()!=null && !c.getCompositionId().isBlank()) list.add(c);
            }
            return list;
        }
    }

    public static List<EventSession> readEventSessions(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Event Sessions");
            if (sheet == null) sheet = wb.getSheet("Event_Session");
            if (sheet == null) return List.of();

            List<EventSession> list = new ArrayList<>();
            Iterator<Row> it = sheet.rowIterator();
            if (it.hasNext()) it.next(); // header
            while (it.hasNext()) {
                Row row = it.next();
                EventSession s = new EventSession();
                s.setCompositionId(getStr(row, 0));
                s.setNumberOfEventSessions((int) getNum(row, 1));
                s.setEventSessionId1(getStr(row, 2));
                s.setEventSessionMode1(getStr(row, 3));
                s.setEventSessionStartDate1(getStr(row, 4));
                s.setEventSessionEndDate1(getStr(row, 5));
                s.setEventSessionDuration1((int) getNum(row, 6));
                s.setEventSessionVenue1(getStr(row, 7));
                s.setEventSessionCapacity1((int) getNum(row, 8));
                s.setEventSessionPatientReferences1(getStr(row, 9));
                String att = getStr(row, 10);
                if (!att.isEmpty()) s.setAttendedIndicator(att.equalsIgnoreCase("true") || att.equalsIgnoreCase("yes"));
                s.setPurposeOfContact(getStr(row, 11));
                if (s.getCompositionId() != null && !s.getCompositionId().isBlank()) list.add(s);
            }
            return list;
        }
    }

    public static List<com.aac.kpi.model.Practitioner> readPractitioners(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Practitioner (Master)");
            if (sheet == null) sheet = wb.getSheet("Practitioner_Master");
            if (sheet == null) return List.of();
            List<com.aac.kpi.model.Practitioner> list = new ArrayList<>();
            Iterator<Row> it = sheet.rowIterator();
            if (it.hasNext()) it.next(); // header
            while (it.hasNext()) {
                Row row = it.next();
                com.aac.kpi.model.Practitioner p = new com.aac.kpi.model.Practitioner();
                p.setPractitionerId(getStr(row, 0));
                p.setPractitionerIdentifierValue(getStr(row, 1));
                p.setPractitionerIdentifierSystem(getStr(row, 2));
                p.setPractitionerManpowerPosition(getStr(row, 3));
                p.setPractitionerVolunteerName(getStr(row, 4));
                p.setPractitionerManpowerCapacity(getNum(row, 5));
                p.setPractitionerVolunteerAge((int)getNum(row, 6));
                p.setWorkingRemarks(getStr(row, 7));
                if (p.getPractitionerId() != null && !p.getPractitionerId().isBlank()) list.add(p);
            }
            return list;
        }
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static String getDateStr(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
            return date.format(DATE_FORMAT);
        }
        return getStr(row, col);
    }

    private static String getStr(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private static double getNum(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return 0d;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        if (cell.getCellType() == CellType.STRING) {
            try { return Double.parseDouble(cell.getStringCellValue().trim()); } catch (Exception ignored) {}
        }
        return 0d;
    }
}
