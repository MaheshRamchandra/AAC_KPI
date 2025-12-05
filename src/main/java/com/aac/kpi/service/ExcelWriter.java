package com.aac.kpi.service;

import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.Practitioner;
import com.aac.kpi.service.AppState;
import com.aac.kpi.service.MasterDataService;
import com.aac.kpi.service.MasterDataService.MasterData;
import com.aac.kpi.util.StringUtils;
import java.awt.Color;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ExcelWriter {

    public static File defaultExportPath() {
        String home = System.getProperty("user.home");
        return new File(home + File.separator + "Documents" + File.separator + "KPI_Data.xlsx");
    }

    public static File saveToExcel(List<Patient> patients, List<EventSession> sessions, File file) throws IOException {
        return saveToExcel(
                patients,
                sessions,
                java.util.Collections.<com.aac.kpi.model.Practitioner>emptyList(),
                java.util.Collections.<com.aac.kpi.model.Encounter>emptyList(),
                java.util.Collections.<com.aac.kpi.model.QuestionnaireResponse>emptyList(),
                java.util.Collections.<com.aac.kpi.model.CommonRow>emptyList(),
                file);
    }

    public static File saveToExcel(List<Patient> patients, List<EventSession> sessions,
            List<com.aac.kpi.model.Practitioner> practitioners,
            File file) throws IOException {
        return saveToExcel(
                patients,
                sessions,
                practitioners,
                java.util.Collections.<com.aac.kpi.model.Encounter>emptyList(),
                java.util.Collections.<com.aac.kpi.model.QuestionnaireResponse>emptyList(),
                java.util.Collections.<com.aac.kpi.model.CommonRow>emptyList(),
                file);
    }

    public static File saveToExcel(List<Patient> patients, List<EventSession> sessions,
            List<com.aac.kpi.model.Practitioner> practitioners,
            List<com.aac.kpi.model.Encounter> encounters,
            List<com.aac.kpi.model.QuestionnaireResponse> questionnaires,
            List<com.aac.kpi.model.CommonRow> commonRows,
            File file) throws IOException {
        if (file == null)
            file = defaultExportPath();
        MasterData masterData = AppState.getMasterData();
        if (masterData == null) {
            masterData = MasterDataService.generate();
            AppState.setMasterData(masterData);
        }

        RegistrationConfig registrationConfig = RegistrationConfig.fromAppState();
        RegistrationData registrationData = buildRegistrationData(patients, registrationConfig, AppState.getRegistrationOverrideType());

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle highlightStyle = createHighlightStyle(wb);
            writeMasterDataSheet(wb, masterData);
            writeCombinedCommonSheet(wb, patients, sessions, practitioners, encounters, questionnaires, commonRows,
                    masterData, highlightStyle, registrationData);
            writeEventSessionSheet(wb, sessions, highlightStyle, registrationData);
            writeEventSessionsNricSheet(wb, patients, registrationData);
            writePatientSheet(wb, patients, sessions, highlightStyle);
            writePractitionerSheet(wb, practitioners, masterData, highlightStyle);
            if (encounters != null && !encounters.isEmpty())
                writeEncounterSheet(wb, encounters, highlightStyle);
            if (questionnaires != null && !questionnaires.isEmpty())
                writeQuestionnaireSheet(wb, questionnaires, highlightStyle);

            reorderSheetsForKpiTool(wb);

            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IOException("Failed to create directories: " + file.getParent());
                }
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                wb.write(out);
            }
        }

        return file;
    }

    private static void writePatientSheet(XSSFWorkbook wb, List<Patient> patients, List<EventSession> sessions, CellStyle highlightStyle) {
        // Rename sheet to match expected name
        XSSFSheet sheet = wb.createSheet("Patient (Master)");
        String[] headers = new String[] {
                "patient_id",
                "patient_identifier_value",
                // Inserted columns between patient_identifier_value and patient_birthdate
                "patient_name",
                "patient_telecom_system",
                "patient_telecom_value",
                "patient_gender",
                "patient_birthdate",
                "patient_postalcode",
                "patient_residentialstatuscode",
                "patient_residentialstatusdisplay",
                "patient_racecode",
                "patient_racedisplay",
                "attended_event_references",
                "Working Remarks",
                "Group",
                "Type",
                "AAC",
                "CFS",
                "RF",
                "KPI Type",
                "KPI Group"
        };

        Map<String, List<String>> patientToEvents = new HashMap<>();
        for (EventSession s : sessions) {
            String raw = s.getEventSessionPatientReferences1();
            if (raw == null || raw.isBlank())
                continue;
            // Use sanitized composition_id so references in the patient sheet
            // line up with the Event Sessions sheet.
            String eventId = StringUtils.sanitizeAlphaNum(s.getCompositionId());
            if (eventId.isEmpty())
                continue;
            for (String part : raw.split("##")) {
                String pid = part == null ? "" : part.trim();
                if (pid.isEmpty())
                    continue;
                patientToEvents.computeIfAbsent(pid, k -> new ArrayList<>()).add(eventId);
            }
        }

        createHeaderRow(sheet, headers);
        // Date style for birthdate
        CellStyle birthDateStyle = sheet.getWorkbook().createCellStyle();
        birthDateStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd"));
        int r = 1;
        for (Patient p : patients) {
            Row row = sheet.createRow(r++);
            int c = 0;
            String patientId = nvl(p.getPatientId());
            row.createCell(c++).setCellValue(patientId);
            row.createCell(c++).setCellValue(nvl(p.getPatientIdentifierValue()));
            // Newly added random columns
            row.createCell(c++).setCellValue("Befriending_data_" + (r - 1)); // patient_name sample value
            row.createCell(c++).setCellValue("phone"); // patient_telecom_system
            row.createCell(c++).setCellValue("99999999"); // patient_telecom_value
            row.createCell(c++).setCellValue(RandomDataUtil.randomGender()); // patient_gender
            String bd = nvl(p.getPatientBirthdate());
            if (bd.isEmpty())
                bd = RandomDataUtil.randomDOB60Plus();
            setDateCell(row, c++, bd, birthDateStyle);
            row.createCell(c++).setCellValue(RandomDataUtil.randomPostal6()); // patient_postalcode
            String[] residentialStatus = RandomDataUtil.randomResidentialStatus();
            row.createCell(c++).setCellValue(residentialStatus[0]);
            row.createCell(c++).setCellValue(residentialStatus[1]);
            String[] race = RandomDataUtil.randomRace();
            row.createCell(c++).setCellValue(race[0]);
            row.createCell(c++).setCellValue(race[1]);
            // Ensure attended_event_references contains only alphanumeric event composition_ids (##-delimited)
            List<String> comps = patientToEvents.getOrDefault(p.getPatientId(), Collections.emptyList());
            StringBuilder sb = new StringBuilder();
            for (String id : comps) {
                String clean = StringUtils.sanitizeAlphaNum(id);
                if (clean.isEmpty())
                    continue;
                if (sb.length() > 0)
                    sb.append("##");
                sb.append(clean);
            }
            String refs = sb.toString();
            row.createCell(c++).setCellValue(refs);
            row.createCell(c++).setCellValue(nvl(p.getWorkingRemarks()));
            row.createCell(c++).setCellValue(p.getGroup());
            row.createCell(c++).setCellValue(nvl(p.getType()));
            row.createCell(c++).setCellValue(nvl(p.getAac()));
            row.createCell(c++).setCellValue(p.getCfs());
            row.createCell(c++).setCellValue(p.getSocialRiskFactor());
            row.createCell(c++).setCellValue(nvl(p.getKpiType()));
            row.createCell(c++).setCellValue(nvl(p.getKpiGroup()));
            if (highlightStyle != null && !patientId.isEmpty()
                    && AppState.getHighlightedPatientIds().contains(patientId)) {
                applyHighlight(row, highlightStyle);
            }
        }

        autoSize(sheet, headers.length);
    }

    private static void writeMasterDataSheet(XSSFWorkbook wb, MasterData masterData) {
        XSSFSheet sheet = wb.createSheet("AAC_Organization_Location_Master");
        String[] headers = {
                "aac_center_id", "aac_center_name", "organization_id", "organization_name", "organization_type",
                "location_id", "location_name", "postal_code", "volunteer_id", "volunteer_name", "volunteer_role",
                "active", "working_remarks"
        };
        createHeaderRow(sheet, headers);
        int r = 1;
        for (MasterDataService.AacCenter center : masterData.getAacCenters()) {
            Row excelRow = sheet.createRow(r++);
            int c = 0;
            MasterDataService.Organization org = masterData.getOrganization(center.organizationId());
            MasterDataService.Location location = masterData.getPrimaryLocation(center.organizationId());
            excelRow.createCell(c++).setCellValue(center.aacCenterId());
            excelRow.createCell(c++).setCellValue(center.aacCenterName());
            excelRow.createCell(c++).setCellValue(org != null ? org.organizationId() : "");
            excelRow.createCell(c++).setCellValue(org != null ? org.name() : "");
            excelRow.createCell(c++).setCellValue(org != null ? org.organizationType() : "");
            excelRow.createCell(c++).setCellValue(location != null ? location.locationId() : "");
            excelRow.createCell(c++).setCellValue(location != null ? location.locationName() : "");
            excelRow.createCell(c++).setCellValue(location != null ? location.postalCode() : "");
            List<String> volunteerIds = masterData.getVolunteerIds(center.aacCenterId());
            List<String> volunteerNames = masterData.getVolunteerNames(center.aacCenterId());
            String joinedIds = volunteerIds.isEmpty() ? "" : String.join("##", volunteerIds);
            String patrolNames = volunteerNames.isEmpty() ? "" : String.join(", ", volunteerNames);
            excelRow.createCell(c++).setCellValue(joinedIds);
            excelRow.createCell(c++).setCellValue(patrolNames);
            excelRow.createCell(c++).setCellValue("Administrative Support");
            excelRow.createCell(c++).setCellValue("TRUE");
            excelRow.createCell(c).setCellValue("Linked to Befriending KPI");
        }
        autoSize(sheet, headers.length);
    }

    private static void writeEventSessionSheet(XSSFWorkbook wb, List<EventSession> sessions, CellStyle highlightStyle, RegistrationData registrationData) {
        // Rename sheet to match expected name
        XSSFSheet sheet = wb.createSheet("Event Sessions");
        String[] headers = new String[] {
                "composition_id",
                "number_of_event_sessions",
                "event_session_id1",
                "event_session_mode1",
                "event_session_start_date1",
                "event_session_end_date1",
                "event_session_duration1",
                "event_session_venue1",
                "event_session_capacity1",
                "event_session_patient_references1",
                "attended_indicator",
                "purpose_of_contact"
        };

        createHeaderRow(sheet, headers);
        // Keep a datetime style available for any future numeric date usage, but
        // for scenario-driven data we now store ISO+08:00 strings directly.
        CellStyle dateTimeStyleES = sheet.getWorkbook().createCellStyle();
        dateTimeStyleES.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));
        int r = 1;
        CellStyle dateTimeStyle = sheet.getWorkbook().createCellStyle();
        dateTimeStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));
        for (EventSession s : sessions) {
            String compId = StringUtils.sanitizeAlphaNum(s.getCompositionId());
            Row row = sheet.createRow(r++);
            int c = 0;
            row.createCell(c++).setCellValue(compId);
            row.createCell(c++).setCellValue(s.getNumberOfEventSessions());
            row.createCell(c++).setCellValue(nvl(s.getEventSessionId1()));
            row.createCell(c++).setCellValue(nvl(s.getEventSessionMode1()));
            // Store start/end as ISO 8601 offset text (e.g. 2025-08-24T09:00:00+08:00)
            row.createCell(c++).setCellValue(nvl(s.getEventSessionStartDate1()));
            row.createCell(c++).setCellValue(nvl(s.getEventSessionEndDate1()));
            row.createCell(c++).setCellValue(s.getEventSessionDuration1());
            row.createCell(c++).setCellValue(nvl(s.getEventSessionVenue1()));
            row.createCell(c++).setCellValue(s.getEventSessionCapacity1());
            String regJoined = String.join("##", registrationData.lookupRegistrationIds(nvl(s.getEventSessionPatientReferences1())));
            if (regJoined.isBlank()) regJoined = nvl(s.getEventSessionPatientReferences1());
            row.createCell(c++).setCellValue(regJoined);
            row.createCell(c++).setCellValue(s.isAttendedIndicator() ? "TRUE" : "FALSE");
            row.createCell(c++).setCellValue(nvl(s.getPurposeOfContact()));
            if (highlightStyle != null && !compId.isEmpty()
                    && AppState.getHighlightedEventSessionCompositionIds().contains(compId)) {
                applyHighlight(row, highlightStyle);
            }
        }

        autoSize(sheet, headers.length);
    }

    private static void writePractitionerSheet(XSSFWorkbook wb,
                                               List<Practitioner> practitioners,
                                               MasterData masterData,
                                               CellStyle highlightStyle) {
        XSSFSheet sheet = wb.createSheet("Practitioner (Master)");
        String[] headers = new String[] {
                "practitioner_id",
                "practitioner_identifier_value",
                "practitioner_identifier_system",
                "practitioner_manpower_position",
                "practitioner_volunteer_name",
                "practitioner_manpower_capacity",
                "practitioner_volunteer_age",
                "Working Remarks"
        };
        createHeaderRow(sheet, headers);
        int r = 1;
        if (practitioners != null && !practitioners.isEmpty()) {
            for (Practitioner p : practitioners) {
                Row row = sheet.createRow(r++);
                int c = 0;
                String pid = nvl(p.getPractitionerId());
                row.createCell(c++).setCellValue(pid);
                row.createCell(c++).setCellValue(nvl(p.getPractitionerIdentifierValue()));
                row.createCell(c++).setCellValue(nvl(p.getPractitionerIdentifierSystem()));
                row.createCell(c++).setCellValue(nvl(p.getPractitionerManpowerPosition()));
                row.createCell(c++).setCellValue(nvl(p.getPractitionerVolunteerName()));
                row.createCell(c++).setCellValue(p.getPractitionerManpowerCapacity());
                row.createCell(c++).setCellValue(p.getPractitionerVolunteerAge());
                row.createCell(c++).setCellValue(nvl(p.getWorkingRemarks()));
                if (highlightStyle != null && !pid.isEmpty()
                        && AppState.getHighlightedPractitionerIds().contains(pid)) {
                    applyHighlight(row, highlightStyle);
                }
            }
        } else {
            for (MasterDataService.Volunteer v : masterData.getVolunteers()) {
                Row row = sheet.createRow(r++);
                int c = 0;
                row.createCell(c++).setCellValue(nvl(v.volunteerId()));
                row.createCell(c++).setCellValue(NRICGeneratorUtil.generateFakeNRIC());
                row.createCell(c++).setCellValue(RandomDataUtil.randomPractitionerIdentifierSystem());
                row.createCell(c++).setCellValue(nvl(v.volunteerRole()));
                row.createCell(c++).setCellValue(nvl(v.volunteerName()));
                row.createCell(c++).setCellValue(0.8);
                row.createCell(c++).setCellValue(35);
                row.createCell(c++).setCellValue("Linked via AAC master");
                if (highlightStyle != null && AppState.getHighlightedPractitionerIds().contains(nvl(v.volunteerId()))) {
                    applyHighlight(row, highlightStyle);
                }
            }
        }
        autoSize(sheet, headers.length);
    }

    private static void writeEncounterSheet(XSSFWorkbook wb, List<com.aac.kpi.model.Encounter> list, CellStyle highlightStyle) {
        XSSFSheet sheet = wb.createSheet("Encounter (Master)");
        String[] headers = new String[] {
                "encounter_id",
                "encounter_status",
                "encounter_display",
                "encounter_start",
                "encounter_purpose",
                "encounter_contactedstaffname",
                "encounter_referredby",
                "encounter_patient_reference"
        };
        createHeaderRow(sheet, headers);
        CellStyle dateTimeStyleEN = sheet.getWorkbook().createCellStyle();
        dateTimeStyleEN.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));
        int r = 1;
        for (com.aac.kpi.model.Encounter e : list) {
            Row row = sheet.createRow(r++);
            int c = 0;
            String encounterId = nvl(e.getEncounterId());
            row.createCell(c++).setCellValue(encounterId);
            row.createCell(c++).setCellValue(nvl(e.getEncounterStatus()));
            row.createCell(c++).setCellValue(nvl(e.getEncounterDisplay()));
            // encounter_start should be stored as ISO offset text (e.g. 2025-09-09T10:30:01+08:00)
            String encStart = nvl(e.getEncounterStart());
            if (!encStart.isBlank()) {
                row.createCell(c++).setCellValue(toIsoOffset(encStart));
            } else {
                row.createCell(c++).setCellValue("");
            }
            row.createCell(c++).setCellValue(nvl(e.getEncounterPurpose()));
            row.createCell(c++).setCellValue(nvl(e.getEncounterContactedStaffName()));
            row.createCell(c++).setCellValue(nvl(e.getEncounterReferredBy()));
            row.createCell(c++).setCellValue(nvl(e.getEncounterPatientReference()));
            if (highlightStyle != null && !encounterId.isEmpty()
                    && AppState.getHighlightedEncounterIds().contains(encounterId)) {
                applyHighlight(row, highlightStyle);
            }
        }
        autoSize(sheet, headers.length);
    }

    private static void writeQuestionnaireSheet(XSSFWorkbook wb,
                                               List<com.aac.kpi.model.QuestionnaireResponse> list,
                                               CellStyle highlightStyle) {
        XSSFSheet sheet = wb.createSheet("QuestionnaireResponse (Master)");
        String[] headers = new String[] {
                "questionnaire_id",
                "questionnaire_status",
                "questionnaire_q1_answer",
                "questionnaire_q2_answer",
                "questionnaire_q3_answer",
                "questionnaire_q4_answer",
                "questionnaire_q5_answer",
                "questionnaire_q6_answer",
                "questionnaire_q7_answer",
                "questionnaire_q8_answer",
                "questionnaire_q9_answer",
                "questionnaire_q10_answer",
                "questionnaire_patient_reference"
        };
        createHeaderRow(sheet, headers);
        // Date style for Q1..Q9 answers
        CellStyle dateStyle = sheet.getWorkbook().createCellStyle();
        dateStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd"));
        int r = 1;
        for (com.aac.kpi.model.QuestionnaireResponse q : list) {
            Row row = sheet.createRow(r++);
            int c = 0;
            String questionnaireId = nvl(q.getQuestionnaireId());
            row.createCell(c++).setCellValue(questionnaireId);
            row.createCell(c++).setCellValue(nvl(q.getQuestionnaireStatus()));
            setDateCell(row, c++, q.getQ1(), dateStyle);
            row.createCell(c++).setCellValue(nvl(q.getQ2()));
            setDateCell(row, c++, q.getQ3(), dateStyle);
            row.createCell(c++).setCellValue(nvl(q.getQ4()));
            setDateCell(row, c++, q.getQ5(), dateStyle);
            row.createCell(c++).setCellValue(nvl(q.getQ6()));
            setDateCell(row, c++, q.getQ7(), dateStyle);
            row.createCell(c++).setCellValue(nvl(q.getQ8()));
            setDateCell(row, c++, q.getQ9(), dateStyle);
            row.createCell(c++).setCellValue(nvl(q.getQ10()));
            row.createCell(c++).setCellValue(nvl(q.getQuestionnairePatientReference()));
            if (highlightStyle != null && !questionnaireId.isEmpty()
                    && AppState.getHighlightedQuestionnaireIds().contains(questionnaireId)) {
                applyHighlight(row, highlightStyle);
            }
        }
        autoSize(sheet, headers.length);
    }

    private static void writeCommonSheet(XSSFWorkbook wb, List<com.aac.kpi.model.CommonRow> list) {
        XSSFSheet sheet = wb.createSheet("Common");
        String[] headers = new String[] {
                "composition_id", "version_id", "last_updated", "meta_code", "reporting_month",
                "total_operating_days", "total_clients", "status", "author_value", "author_display",
                "patient_reference", "encounter_references", "questionnaire_reference", "attended_event_references"
        };
        createHeaderRow(sheet, headers);
        int r = 1;
        for (com.aac.kpi.model.CommonRow c : list) {
            Row row = sheet.createRow(r++);
            int i = 0;
            row.createCell(i++).setCellValue(nvl(c.getCompositionId()));
            row.createCell(i++).setCellValue(c.getVersionId());
            row.createCell(i++).setCellValue(toIsoOffset(reportDateWithOverride(nvl(c.getLastUpdated()))));
            row.createCell(i++).setCellValue(nvl(c.getMetaCode()));
            row.createCell(i++).setCellValue(reportingMonthWithOverride(nvl(c.getReportingMonth())));
            row.createCell(i++).setCellValue(c.getTotalOperatingDays());
            row.createCell(i++).setCellValue(c.getTotalClients());
            row.createCell(i++).setCellValue(nvl(c.getStatus()));
            row.createCell(i++).setCellValue(nvl(c.getAuthorValue()));
            row.createCell(i++).setCellValue(nvl(c.getAuthorDisplay()));
            row.createCell(i++).setCellValue(nvl(c.getPatientReference()));
            row.createCell(i++).setCellValue(nvl(c.getEncounterReferences()));
            row.createCell(i++).setCellValue(nvl(c.getQuestionnaireReference()));
            row.createCell(i++).setCellValue(nvl(c.getAttendedEventReferences()));
        }
        autoSize(sheet, headers.length);
    }

    private static void writeCombinedCommonSheet(XSSFWorkbook wb,
            List<Patient> patients,
            List<EventSession> sessions,
            List<com.aac.kpi.model.Practitioner> practitioners,
            List<com.aac.kpi.model.Encounter> encounters,
            List<com.aac.kpi.model.QuestionnaireResponse> questionnaires,
            List<com.aac.kpi.model.CommonRow> residentRows,
            MasterData masterData,
            CellStyle highlightStyle,
            RegistrationData registrationData) {
        XSSFSheet sheet = wb.createSheet("Common");

        // Styles
        CellStyle baseBold = wb.createCellStyle();
        Font bold = wb.createFont();
        bold.setBold(true);
        baseBold.setFont(bold);
        // Table Header Row (Title) - Green #00B050
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.cloneStyleFrom(baseBold);
        ((org.apache.poi.xssf.usermodel.XSSFCellStyle) titleStyle)
                .setFillForegroundColor(
                        new org.apache.poi.xssf.usermodel.XSSFColor(new java.awt.Color(0, 176, 80), null));
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        // Column Header Row - Light Blue #BDD7EE
        CellStyle columnHeaderStyle = wb.createCellStyle();
        columnHeaderStyle.cloneStyleFrom(baseBold);
        ((org.apache.poi.xssf.usermodel.XSSFCellStyle) columnHeaderStyle)
                .setFillForegroundColor(
                        new org.apache.poi.xssf.usermodel.XSSFColor(new java.awt.Color(189, 215, 238), null));
        columnHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int r = 0;
        // Section 1: aac_report
        r = writeSectionTitle(sheet, r, "aac_report", titleStyle);
        r = writeAacReportSection(sheet, r, patients, sessions, practitioners, columnHeaderStyle, masterData);

        // Section 2: resident_report (from residentRows/CommonRow)
        r = writeSectionTitle(sheet, r, "resident_report", titleStyle);
        r = writeResidentReportSection(sheet, r, residentRows, columnHeaderStyle, highlightStyle);

        // Section 2.5: volunteer_attendance_report (between resident and event)
        r = writeSectionTitle(sheet, r, "volunteer_attendance_report", titleStyle);
        r = writeVolunteerAttendanceReportSection(sheet, r, patients, practitioners, masterData, columnHeaderStyle);

        // Section 3: event_report
        r = writeSectionTitle(sheet, r, "event_report", titleStyle);
        r = writeEventReportSection(sheet, r, patients, sessions, practitioners, columnHeaderStyle, masterData, registrationData);

        // Section 4: organization_report
        r = writeSectionTitle(sheet, r, "organization_report", titleStyle);
        r = writeOrganizationReportSection(sheet, r, patients, sessions, practitioners, columnHeaderStyle, masterData);

        // Section 5: location_report
        r = writeSectionTitle(sheet, r, "location_report", titleStyle);
        r = writeLocationReportSection(sheet, r, sessions, columnHeaderStyle, masterData);

        // Autosize columns (cover extended resident_report columns)
        for (int c = 0; c < 40; c++)
            sheet.autoSizeColumn(c);
    }

    private static void reorderSheetsForKpiTool(XSSFWorkbook wb) {
        // JSON exporter relies on fixed sheet indexes beyond the Common tab.
        List<String> desiredOrder = List.of(
                "Common",
                "Event Sessions",
                "Event Sessions NRIC",
                "Patient (Master)",
                "Practitioner (Master)",
                "Encounter (Master)",
                "QuestionnaireResponse (Master)");
        int position = 0;
        for (String name : desiredOrder) {
            int idx = wb.getSheetIndex(name);
            if (idx < 0)
                continue;
            wb.setSheetOrder(name, position++);
        }
    }

    private static void applyCfsValidation(Sheet sheet, int startRow, int endRow, int columnIndex) {
        if (endRow < startRow)
            return;
        DataValidationHelper helper = sheet.getDataValidationHelper();
        String[] options = new String[] { "1-3", "4-5", "4", "5", "6", "7", "8", "9" };
        DataValidationConstraint constraint = helper.createExplicitListConstraint(options);
        CellRangeAddressList regions = new CellRangeAddressList(startRow, endRow, columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, regions);
        validation.setSuppressDropDownArrow(false);
        validation.setShowErrorBox(true);
        validation.createPromptBox("CFS selection", "Choose the grouped range (1-3, 4-5) or a single value.");
        validation.setShowPromptBox(true);
        sheet.addValidationData(validation);
    }

    private static void applySocialRiskValidation(Sheet sheet, int startRow, int endRow, int columnIndex) {
        if (endRow < startRow)
            return;
        DataValidationHelper helper = sheet.getDataValidationHelper();
        String[] options = new String[] { "1", ">1" };
        DataValidationConstraint constraint = helper.createExplicitListConstraint(options);
        CellRangeAddressList regions = new CellRangeAddressList(startRow, endRow, columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, regions);
        validation.setSuppressDropDownArrow(false);
        validation.setShowErrorBox(true);
        validation.createPromptBox("Social risk selection", "Pick either 1 or >1 for the social risk factor column.");
        validation.setShowPromptBox(true);
        sheet.addValidationData(validation);
    }

    private static int writeSectionTitle(Sheet sheet, int rowIndex, String title, CellStyle style) {
        Row row = sheet.createRow(rowIndex++);
        Cell c = row.createCell(0);
        c.setCellValue(title);
        c.setCellStyle(style);
        return rowIndex;
    }

    private static int writeAacReportSection(Sheet sheet, int rowIndex,
            List<Patient> patients,
            List<EventSession> sessions,
            List<com.aac.kpi.model.Practitioner> practitioners,
            CellStyle headerStyle,
            MasterData masterData) {
        String[] headers = { "S.No", "composition_id", "version_id", "last_updated", "meta_code",
                "extension_reporting_month", "extension_total_operating_days", "extension_total_clients",
                "status", "date", "author_value", "author_display", "practitioner_references" };
        createHeaderRow(sheet, headers, rowIndex++, headerStyle);

        Map<String, Long> clientsByAac = new HashMap<>();
        for (Patient p : patients)
            clientsByAac.merge(nvl(p.getAac()), 1L, Long::sum);
        Map<String, LocalDateTime> latestByAac = new HashMap<>();
        Map<String, Patient> pIndex = new HashMap<>();
        for (Patient p : patients)
            pIndex.put(p.getPatientId(), p);
        for (EventSession s : sessions) {
            Patient p = pIndex.getOrDefault(s.getEventSessionPatientReferences1(), null);
            if (p == null)
                continue;
            String aac = nvl(p.getAac());
            LocalDateTime dt = parseDateTime(s.getEventSessionStartDate1());
            if (dt != null)
                latestByAac.merge(aac, dt, (a, b) -> a.isAfter(b) ? a : b);
        }
        List<String> practitionerIds = practitioners.stream()
                .map(com.aac.kpi.model.Practitioner::getPractitionerId)
                .filter(Objects::nonNull).toList();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        int sno = 1;
        for (MasterDataService.AacCenter center : masterData.getAacCenters()) {
            String aac = center.aacCenterId();
            Row row = sheet.createRow(rowIndex++);
            int i = 0;
            row.createCell(i++).setCellValue("aac_report_" + sno++);
            row.createCell(i++).setCellValue(RandomDataUtil.uuid32().toUpperCase());
            row.createCell(i++).setCellValue(1);
            row.createCell(i++).setCellValue(nowIsoOffset("+08:00"));
            row.createCell(i++).setCellValue(RandomDataUtil.uuid32().substring(0, 20));
            LocalDateTime dt = latestByAac.getOrDefault(aac, LocalDateTime.now());
            row.createCell(i++).setCellValue(reportingMonthWithOverride(dt.format(monthFmt)));
            row.createCell(i++).setCellValue(240);
            row.createCell(i++).setCellValue(clientsByAac.getOrDefault(aac, 0L).intValue());
            row.createCell(i++).setCellValue("final");
            row.createCell(i++).setCellValue(toIsoOffset(reportDateWithOverride(nowIsoOffset("+08:00"))));
            row.createCell(i++).setCellValue(aac);
            row.createCell(i++).setCellValue(center.aacCenterName());
            List<String> references = selectPractitionerSubset(
                    practitionerIds,
                    sno,
                    Math.max(1, Math.min(3, practitionerIds.size()))
            );
            row.createCell(i).setCellValue(String.join("##", references));
        }
        return rowIndex; // no blank line between sections
    }

    private static int writeResidentReportSection(Sheet sheet, int rowIndex,
            List<com.aac.kpi.model.CommonRow> residents,
            CellStyle headerStyle,
            CellStyle highlightStyle) {
        String[] headers = { "S. No", "composition_id", "version_id", "last_updated", "meta_code",
                "extension_reporting_month", "status", "date", "author_value", "author_display",
                "resident_volunteer_status", "cst_date", "cfs", "social_risk_factor_score", "aap_recommendation",
                "social_support_recommendation", "aac_opt_out_status", "aap_opt_out_status",
                "screening_declaration_date",
                "befriending_opt_out_status", "buddying_opt_out_status", "resident_befriending_programme_period_start",
                "resident_befriending_programme_period_end", "resident_buddying_programme_period_start",
                "resident_buddying_programme_period_end", "irms_referral_raised_date", "irms_referral_accepted_date",
                "asg_referral_raised_by", "asg_referral_accepted_by", "patient_reference", "encounter_references",
                "questionnaire_reference" };
        createHeaderRow(sheet, headers, rowIndex++, headerStyle);
        int dataStartRow = rowIndex;
        int sno = 1;
        // Prepare date style for yyyy-MM-dd (Excel date type) and datetime style
        CellStyle dateStyle = sheet.getWorkbook().createCellStyle();
        dateStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd"));
        CellStyle dateTimeStyle = sheet.getWorkbook().createCellStyle();
        dateTimeStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));
        for (com.aac.kpi.model.CommonRow c : residents) {
            if (c.getPatientReference() == null || c.getPatientReference().isBlank())
                continue; // only resident rows
            Row row = sheet.createRow(rowIndex++);
            int i = 0;
            row.createCell(i++).setCellValue("resident_report_" + sno++);
            row.createCell(i++).setCellValue(nvl(c.getCompositionId()));
            row.createCell(i++).setCellValue(c.getVersionId());
            row.createCell(i++).setCellValue(nowIsoOffset("+08:00"));
            row.createCell(i++).setCellValue(nvl(c.getMetaCode()));
            row.createCell(i++).setCellValue(reportingMonthWithOverride(nvl(c.getReportingMonth())));
            row.createCell(i++).setCellValue(nvl(c.getStatus()));
            // 'date' column: ISO 8601 with +08:00 (text), e.g. 2025-05-09T06:19:37+08:00
            String last = reportDateWithOverride(nvl(c.getLastUpdated()));
            if (last.isBlank()) last = reportDateWithOverride(nowStamp());
            row.createCell(i++).setCellValue(toIsoOffset(last));
            row.createCell(i++).setCellValue(nvl(c.getAuthorValue()));
            row.createCell(i++).setCellValue(nvl(c.getAuthorDisplay()));
            row.createCell(i++).setCellValue(nvl(c.getResidentVolunteerStatus()));
            setDateCell(row, i++, c.getCstDate(), dateStyle);
            String cfsText = c.getCfsLabel();
            if (cfsText == null || cfsText.isBlank())
                cfsText = CfsUtil.formatCfs(c.getCfs());
            row.createCell(i++).setCellValue(cfsText);
            String srValue = nvl(c.getSocialRiskLabel());
            if (srValue.isBlank())
                srValue = String.valueOf(c.getSocialRiskFactorScore());
            row.createCell(i++).setCellValue(srValue);
            row.createCell(i++).setCellValue(nvl(c.getAapRecommendation()));
            row.createCell(i++).setCellValue(nvl(c.getSocialSupportRecommendation()));
            row.createCell(i++).setCellValue(nvl(c.getAacOptOutStatus()));
            row.createCell(i++).setCellValue(nvl(c.getAapOptOutStatus()));
            setDateCell(row, i++, c.getScreeningDeclarationDate(), dateStyle);
            row.createCell(i++).setCellValue(nvl(c.getBefriendingOptOutStatus()));
            row.createCell(i++).setCellValue(nvl(c.getBuddyingOptOutStatus()));
            setDateCell(row, i++, c.getResidentBefriendingProgrammePeriodStart(), dateStyle);
            setDateCell(row, i++, c.getResidentBefriendingProgrammePeriodEnd(), dateStyle);
            setDateCell(row, i++, c.getResidentBuddyingProgrammePeriodStart(), dateStyle);
            setDateCell(row, i++, c.getResidentBuddyingProgrammePeriodEnd(), dateStyle);
            setDateCell(row, i++, c.getIrmsReferralRaisedDate(), dateStyle);
            setDateCell(row, i++, c.getIrmsReferralAcceptedDate(), dateStyle);
            row.createCell(i++).setCellValue(nvl(c.getAsgReferralRaisedBy()));
            row.createCell(i++).setCellValue(nvl(c.getAsgReferralAcceptedBy()));
            String patientRef = nvl(c.getPatientReference());
            row.createCell(i++).setCellValue(patientRef);
            row.createCell(i++).setCellValue(nvl(c.getEncounterReferences()));
            row.createCell(i).setCellValue(nvl(c.getQuestionnaireReference()));
            if (highlightStyle != null && !patientRef.isEmpty()
                    && AppState.getHighlightedPatientIds().contains(patientRef)) {
                applyHighlight(row, highlightStyle);
            }
        }
        int dataEndRow = rowIndex - 1;
        applyCfsValidation(sheet, dataStartRow, dataEndRow, 12);
        applySocialRiskValidation(sheet, dataStartRow, dataEndRow, 13);
        return rowIndex;
    }

    private static int writeEventReportSection(Sheet sheet, int rowIndex,
            List<Patient> patients,
            List<EventSession> sessions,
            List<com.aac.kpi.model.Practitioner> practitioners,
            CellStyle headerStyle,
            MasterData masterData,
            RegistrationData registrationData) {
        // Columns as requested
        // Build dynamic headers: add is_attended_session_patientN only up to max refs
        // across events
        // We'll compute the groups first to derive maxRefs
        // Index patients by ID and AAC/KPI metadata
        Map<String, Patient> patientIndex = new HashMap<>();
        Map<String, Patient> patientIndexSan = new HashMap<>();
        for (Patient p : patients) {
            if (p.getPatientId() != null) {
                patientIndex.put(p.getPatientId(), p);
                patientIndexSan.put(StringUtils.sanitizeAlphaNum(p.getPatientId()), p);
            }
        }

        // Group sessions by event_id
        Map<String, List<EventSession>> byEventId = new HashMap<>();
        for (EventSession s : sessions) {
            String ev = nvl(s.getEventSessionId1());
            byEventId.computeIfAbsent(ev, k -> new ArrayList<>()).add(s);
        }

        int maxRefs = 0;
        for (EventSession s : sessions) {
            if (!s.isAttendedIndicator())
                continue;
            String raw = nvl(s.getEventSessionPatientReferences1());
            if (raw.isBlank()) continue;
            LinkedHashSet<String> refs = new LinkedHashSet<>();
            for (String part : raw.split("##")) {
                String clean = StringUtils.sanitizeAlphaNum(part);
                if (clean.isBlank()) continue;
                List<String> regs = registrationData.lookupRegistrationIds(clean);
                if (regs.isEmpty()) refs.add(clean); else refs.addAll(regs);
            }
            maxRefs = Math.max(maxRefs, refs.size());
        }

        List<String> headerList = new ArrayList<>(List.of(
                "S. No", "composition_id", "version_id", "last_updated", "meta_code",
                "extension_reporting_month", "status", "date", "author_value", "author_display",
                "event_id", "event_name", "event_type", "event_domain", "event_target_attendees", "event_category",
                "aap_provider", "minimum_required_sessions", "event_is_gui", "gui_partner",
                "number_of_event_sessions", "patient_references", "total_patient_references"));
        for (int n = 1; n <= Math.max(1, maxRefs); n++) {
            headerList.add("is_attended_session_patient" + n);
        }
        headerList.add("Working Remarks");
        createHeaderRow(sheet, headerList.toArray(new String[0]), rowIndex++, headerStyle);
        // Working Remarks body cell style (Light Yellow #FFF2CC)
        CellStyle workingRemarksStyle = sheet.getWorkbook().createCellStyle();
        ((org.apache.poi.xssf.usermodel.XSSFCellStyle) workingRemarksStyle)
                .setFillForegroundColor(
                        new org.apache.poi.xssf.usermodel.XSSFColor(new java.awt.Color(255, 242, 204), null));
        workingRemarksStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Practitioner fallback (no linkage available in data)
        String authorValue = "";
        String authorDisplay = "";
        if (practitioners != null && !practitioners.isEmpty()) {
            com.aac.kpi.model.Practitioner pr = practitioners.get(0);
            authorValue = nvl(pr.getPractitionerId());
            String name = nvl(pr.getPractitionerVolunteerName());
            authorDisplay = name.isBlank() ? authorValue : name;
        }

        int sno = 1;
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter stampFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Map.Entry<String, List<EventSession>> e : byEventId.entrySet()) {
            String eventIdRaw = nvl(e.getKey());
            if (eventIdRaw.isBlank())
                continue;
            List<EventSession> list = e.getValue();

            // Derive earliest start and name
            LocalDateTime earliest = null;
            LocalDateTime latest = null;
            int capacitySum = 0;
            for (EventSession s : list) {
                LocalDateTime st = parseDateTime(s.getEventSessionStartDate1());
                LocalDateTime en = parseDateTime(nvl(s.getEventSessionEndDate1()));
                if (en == null)
                    en = st;
                if (st != null)
                    earliest = (earliest == null || st.isBefore(earliest)) ? st : earliest;
                if (en != null)
                    latest = (latest == null || en.isAfter(latest)) ? en : latest;
                capacitySum += Math.max(0, s.getEventSessionCapacity1());
            }

            // Collect attending registration references (unique) from sessions where
            // attendedIndicator = true, and track patient IDs for KPI lookups
            LinkedHashSet<String> attendeeRegs = new LinkedHashSet<>();
            LinkedHashSet<String> attendeePatientIds = new LinkedHashSet<>();
            for (EventSession s : list) {
                if (!s.isAttendedIndicator())
                    continue;
                String raw = nvl(s.getEventSessionPatientReferences1());
                if (raw.isBlank()) continue;
                for (String part : raw.split("##")) {
                    String clean = StringUtils.sanitizeAlphaNum(part);
                    if (clean.isBlank()) continue;
                    attendeePatientIds.add(clean);
                    List<String> regs = registrationData.lookupRegistrationIds(clean);
                    if (regs.isEmpty()) {
                        attendeeRegs.add(clean);
                    } else {
                        attendeeRegs.addAll(regs);
                    }
                }
            }

            // Compute event_type by majority of attendees' KPI type (if available)
            String eventType = "";
            if (!attendeePatientIds.isEmpty()) {
                Map<String, Integer> counts = new HashMap<>();
                for (String pid : attendeePatientIds) {
                    Patient p = patientIndexSan.get(pid);
                    String t = p != null ? nvl(p.getKpiType()) : "";
                    if (!t.isBlank())
                        counts.merge(t.trim(), 1, Integer::sum);
                }
                if (!counts.isEmpty()) {
                    eventType = counts.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
                }
            }
            // Normalize eventType to title-case expected values if close match
            String lt = eventType.toLowerCase();
            if (lt.contains("befriend"))
                eventType = "Befriending";
            else if (lt.contains("robust"))
                eventType = "Robust";
            else if (lt.contains("bud"))
                eventType = "Budding";

            // event_category derived from type (overrideable via UI)
            String eventCategory = switch (eventType) {
                case "Robust" -> "Physical";
                case "Budding", "Befriending" -> "Social";
                default -> "";
            };
            String overrideCategory = AppState.getEventReportLabel();
            if (!overrideCategory.isBlank())
                eventCategory = overrideCategory;

            // aap_provider & GUI fields (no clear mapping; use AAC from first attendee if
            // available)
            String aac = "";
            for (String pid : attendeePatientIds) {
                Patient p = patientIndexSan.get(pid);
                if (p != null) {
                    aac = nvl(p.getAac());
                    break;
                }
            }
            String aapProvider = aac;
            String eventIsGui = "FALSE"; // default
            String guiPartner = "";

            String eventAuthorValue = StringUtils.sanitizeAlphaNum(aac);
            String eventAuthorDisplay = authorDisplay;
            if (!eventAuthorValue.isBlank()) {
                String digits = aac.replaceAll("[^0-9]", "");
                if (!digits.isBlank()) {
                    eventAuthorDisplay = "Active Ageing Centre " + digits;
                }
            } else {
                eventAuthorValue = StringUtils.sanitizeAlphaNum(authorValue);
            }

            // minimum_required_sessions per rules
            int minReq = switch (eventType) {
                case "Robust" -> 1;
                case "Budding" -> 12;
                case "Befriending" -> 52;
                default -> 0;
            };

            // number_of_event_sessions = count rows for this event_id
            int sessionsCount = list.size();

            String eventName = eventIdRaw.replaceAll("[0-9-]", "");
            if (eventName.isBlank() && !list.isEmpty()) {
                String venue = list.get(0).getEventSessionVenue1();
                eventName = nvl(venue);
            }

            for (EventSession session : list) {
                LinkedHashSet<String> sessionRefs = new LinkedHashSet<>();
                if (session.isAttendedIndicator()) {
                    String raw = nvl(session.getEventSessionPatientReferences1());
                    if (!raw.isBlank()) {
                        for (String part : raw.split("##")) {
                            String clean = StringUtils.sanitizeAlphaNum(part);
                            if (clean.isBlank()) continue;
                            List<String> regs = registrationData.lookupRegistrationIds(clean);
                            if (regs.isEmpty()) sessionRefs.add(clean); else sessionRefs.addAll(regs);
                        }
                    }
                }
                String joinedRefs = String.join("##", sessionRefs);
                int totalRefs = sessionRefs.size();

                Row row = sheet.createRow(rowIndex++);
                int i = 0;
                row.createCell(i++).setCellValue("event_report_" + sno++);
                String compFromSession = StringUtils.sanitizeAlphaNum(nvl(session.getCompositionId()));
                row.createCell(i++).setCellValue(compFromSession);
                row.createCell(i++).setCellValue(1);
                row.createCell(i++).setCellValue(nowIsoOffset("+08:00"));
                row.createCell(i++).setCellValue(RandomDataUtil.uuid32().substring(0, 20));
                row.createCell(i++).setCellValue(reportingMonthWithOverride(earliest != null ? earliest.format(monthFmt) : ""));
                row.createCell(i++).setCellValue("completed");
                {
                    String dtStr = earliest != null ? earliest.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
                    dtStr = reportDateWithOverride(dtStr);
                    row.createCell(i++).setCellValue(dtStr.isEmpty() ? "" : toIsoOffset(dtStr));
                }
                row.createCell(i++).setCellValue(eventAuthorValue);
                row.createCell(i++).setCellValue(eventAuthorDisplay);
                row.createCell(i++).setCellValue(StringUtils.sanitizeAlphaNum(eventIdRaw));
                row.createCell(i++).setCellValue(eventName);
                row.createCell(i++)
                        .setCellValue(eventType == null || eventType.isBlank() ? "Physical activity" : eventType);
                row.createCell(i++).setCellValue("Community Well-Being");
                String targetAttendees = "AAC " + (eventType.isBlank() ? "Robust" : eventType);
                row.createCell(i++).setCellValue(targetAttendees);
                row.createCell(i++).setCellValue(eventCategory);
                row.createCell(i++).setCellValue(StringUtils.sanitizeAlphaNum(aapProvider));
                row.createCell(i++).setCellValue(minReq);
                row.createCell(i++).setCellValue(eventIsGui);
                row.createCell(i++).setCellValue(guiPartner);
                row.createCell(i++).setCellValue(sessionsCount);
                row.createCell(i++).setCellValue(joinedRefs);
                row.createCell(i++).setCellValue(totalRefs);

                for (int idx = 0; idx < Math.max(1, maxRefs); idx++) {
                    boolean present = idx < totalRefs;
                    row.createCell(i++).setCellValue(present ? "TRUE" : "");
                }
                Cell wr = row.createCell(i);
                wr.setCellValue("");
                wr.setCellStyle(workingRemarksStyle);
            }
        }
        return rowIndex;
    }

    private static int writeVolunteerAttendanceReportSection(Sheet sheet, int rowIndex,
            List<Patient> patients,
            List<com.aac.kpi.model.Practitioner> practitioners,
            MasterData masterData,
            CellStyle headerStyle) {
        int requested = com.aac.kpi.service.AppState.getVolunteerPractitionerCount();
        if (requested <= 0)
            requested = 1;

        java.util.List<String> headerList = new java.util.ArrayList<>(java.util.List.of(
                "S.No", "composition_id", "version_id", "last_updated", "meta_code",
                "extension_reporting_month", "status", "date", "author_value", "author_display",
                "practitioner_references", "number_of_practitioners"));
        for (int n = 1; n <= requested; n++) {
            headerList.add("volunteered_activity_name_practitioner" + n);
            headerList.add("volunteered_activity_date_practitioner" + n);
        }
        createHeaderRow(sheet, headerList.toArray(new String[0]), rowIndex++, headerStyle);

        CellStyle dateStyle = sheet.getWorkbook().createCellStyle();
        dateStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd"));
        CellStyle dateTimeStyle = sheet.getWorkbook().createCellStyle();
        dateTimeStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));

        List<MasterDataService.AacCenter> centers = masterData.getAacCenters();
        java.util.List<String> practitionerIds = practitioners == null ? java.util.List.of()
                : practitioners.stream()
                        .map(com.aac.kpi.model.Practitioner::getPractitionerId)
                        .filter(id -> id != null && !id.isBlank())
                        .toList();
        int sno = 1;
        for (MasterDataService.AacCenter center : centers) {
            java.util.List<String> refs;
            if (practitionerIds.isEmpty()) {
                refs = java.util.List.of();
            } else {
                refs = selectPractitionerSubset(practitionerIds, sno, requested);
            }
            rowIndex = writeAttendanceRow(sheet, rowIndex, dateStyle, dateTimeStyle, sno++,
                    center, refs, patients, requested);
        }
        return rowIndex;
    }

    private static int writeAttendanceRow(Sheet sheet,
                                          int rowIndex,
                                          CellStyle dateStyle,
                                          CellStyle dateTimeStyle,
                                          int sno,
                                          MasterDataService.AacCenter center,
                                          List<String> practitionerRefs,
                                          List<Patient> patients,
                                          int activityColumns) {
        Row row = sheet.createRow(rowIndex++);
        int i = 0;
        row.createCell(i++).setCellValue("volunteer_attendance_report_" + sno);
        row.createCell(i++).setCellValue(RandomDataUtil.uuid32().toUpperCase());
        row.createCell(i++).setCellValue(1);
        row.createCell(i++).setCellValue(nowIsoOffset("+08:00"));
        row.createCell(i++).setCellValue(RandomDataUtil.uuid32().substring(0, 20));
        row.createCell(i++).setCellValue(reportingMonthWithOverride(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))));
        row.createCell(i++).setCellValue("final");
        row.createCell(i++).setCellValue(toIsoOffset(reportDateWithOverride(nowIsoOffset("+08:00"))));

        String aac = center != null ? center.aacCenterId() : (patients.isEmpty() ? "AAC" : nvl(patients.get(0).getAac()));
        String centerName = center != null ? center.aacCenterName() : "Active Ageing Centre";
        row.createCell(i++).setCellValue(aac);
        row.createCell(i++).setCellValue("Active Ageing Centre " + centerName.replaceAll("[^0-9]", ""));

        List<String> sanitized = practitionerRefs.stream()
                .filter(ref -> ref != null && !ref.isBlank())
                .toList();
        String joined = String.join("##", sanitized);
        row.createCell(i++).setCellValue(joined);
        row.createCell(i++).setCellValue(sanitized.size());

        for (int n = 0; n < activityColumns; n++) {
            if (n < sanitized.size()) {
                row.createCell(i++).setCellValue("Painting class");
                setDateCell(row, i++, "2024-02-11", dateStyle);
            } else {
                row.createCell(i++).setCellValue("");
                row.createCell(i++).setCellValue("");
            }
        }

        return rowIndex;
    }

    private static int writeOrganizationReportSection(Sheet sheet, int rowIndex,
            List<Patient> patients,
            List<EventSession> sessions,
            List<com.aac.kpi.model.Practitioner> practitioners,
            CellStyle headerStyle,
            MasterData masterData) {
        // Per request: columns for organization_report in Common sheet
        String[] headers = { "S. No", "id", "version_id", "last_updated", "meta_code",
                "start", "end", "aac_center_ids", "uen", "active", "organization_type_code",
                "organization_type_display", "name" };
        createHeaderRow(sheet, headers, rowIndex++, headerStyle);

        Map<String, Patient> pIndex = new HashMap<>();
        for (Patient p : patients) {
            pIndex.put(p.getPatientId(), p);
        }
        Map<String, List<MasterDataService.AacCenter>> centersByOrg = masterData.getAacCenters().stream()
                .collect(Collectors.groupingBy(MasterDataService.AacCenter::organizationId));

        Map<String, LocalDateTime> earliestStartByAac = new HashMap<>();
        Map<String, LocalDateTime> latestEndByAac = new HashMap<>();
        for (EventSession s : sessions) {
            Patient p = pIndex.get(s.getEventSessionPatientReferences1());
            if (p == null)
                continue;
            String aac = nvl(p.getAac());
            LocalDateTime startDt = parseDateTime(s.getEventSessionStartDate1());
            LocalDateTime endDt = parseDateTime(nvl(s.getEventSessionEndDate1()));
            if (endDt == null)
                endDt = startDt;
            if (startDt != null)
                earliestStartByAac.merge(aac, startDt, (a, b) -> a.isBefore(b) ? a : b);
            if (endDt != null)
                latestEndByAac.merge(aac, endDt, (a, b) -> a.isAfter(b) ? a : b);
        }

        int sno = 1;
        // For organization_report, start and end should be dates (yyyy-MM-dd)
        CellStyle dateStyleOrg = sheet.getWorkbook().createCellStyle();
        dateStyleOrg.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd"));
        for (MasterDataService.Organization org : masterData.getOrganizations()) {
            Row row = sheet.createRow(rowIndex++);
            int i = 0;
            row.createCell(i++).setCellValue("organization_report_" + sno++);
            row.createCell(i++).setCellValue(org.organizationId());
            row.createCell(i++).setCellValue(1);
            row.createCell(i++).setCellValue(nowIsoOffset("+08:00"));
            row.createCell(i++).setCellValue(RandomDataUtil.uuid32().substring(0, 20));
            LocalDateTime sdt = null;
            LocalDateTime edt = null;
            List<MasterDataService.AacCenter> related = centersByOrg.getOrDefault(org.organizationId(), List.of());
            for (MasterDataService.AacCenter relatedCenter : related) {
                LocalDateTime start = earliestStartByAac.get(relatedCenter.aacCenterId());
                LocalDateTime end = latestEndByAac.get(relatedCenter.aacCenterId());
                if (start != null && (sdt == null || start.isBefore(sdt)))
                    sdt = start;
                if (end != null && (edt == null || end.isAfter(edt)))
                    edt = end;
            }
            setDateCell(row, i++, dateOrDefault(sdt).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), dateStyleOrg);
            setDateCell(row, i++, dateOrDefault(edt).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), dateStyleOrg);
            String aacList = related.stream().map(MasterDataService.AacCenter::aacCenterId)
                    .collect(Collectors.joining("##"));
            row.createCell(i++).setCellValue(aacList);
            row.createCell(i++).setCellValue(RandomDataUtil.randomUen());
            row.createCell(i++).setCellValue("TRUE");
            if (org != null) {
                row.createCell(i++).setCellValue(org.organizationType());
                row.createCell(i++).setCellValue("AAC");
                String centerNames = related.stream()
                        .map(MasterDataService.AacCenter::aacCenterName)
                        .collect(Collectors.joining("##"));
                row.createCell(i++).setCellValue(centerNames);
            } else {
                row.createCell(i++).setCellValue("");
                row.createCell(i++).setCellValue("");
                row.createCell(i++).setCellValue("");
            }
        }
        return rowIndex;
    }

    private static int writeLocationReportSection(Sheet sheet, int rowIndex,
            List<EventSession> sessions,
            CellStyle headerStyle,
            MasterData masterData) {
        String[] headers = { "S. No", "id", "version_id", "last_updated", "meta_code",
                "start", "end", "postal_code", "reference" };
        createHeaderRow(sheet, headers, rowIndex++, headerStyle);

        CellStyle dateStyleLoc = sheet.getWorkbook().createCellStyle();
        dateStyleLoc.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd"));
        String urlPrefix = "https://pophealth.healthdpx.com/Organisation/";
        int sno = 1;
        for (MasterDataService.Location location : masterData.getLocations()) {
            Row row = sheet.createRow(rowIndex++);
            int i = 0;
            row.createCell(i++).setCellValue("location_report_" + sno++);
            row.createCell(i++).setCellValue(RandomDataUtil.uuid32().toUpperCase());
            row.createCell(i++).setCellValue(1);
            row.createCell(i++).setCellValue(nowIsoOffset("+08:00"));
            row.createCell(i++).setCellValue(RandomDataUtil.uuid32().substring(0, 20));
            setDateCell(row, i++, LocalDate.of(2025, 4, 1), dateStyleLoc);
            setDateCell(row, i++, LocalDate.of(2026, 3, 31), dateStyleLoc);
            row.createCell(i++).setCellValue(location.postalCode());
            row.createCell(i).setCellValue(urlPrefix + location.organizationId());
        }
        return rowIndex;
    }

    private static String randomOrganizationReference() {
        return "2025" + RandomDataUtil.uuid32().substring(0, 5).toUpperCase();
    }

    private static LocalDate dateOrDefault(LocalDateTime dt) {
        if (dt != null)
            return dt.toLocalDate();
        return LocalDate.of(2025, 4, 1);
    }

    private static List<String> selectPractitionerSubset(List<String> practitionerIds, int rowNumber, int limit) {
        if (practitionerIds.isEmpty())
            return Collections.emptyList();
        int size = Math.min(limit, practitionerIds.size());
        List<String> subset = new ArrayList<>(size);
        for (int idx = 0; idx < size; idx++) {
            subset.add(practitionerIds.get((rowNumber + idx) % practitionerIds.size()));
        }
        return subset;
    }

    private static void writeAacReportSheet(XSSFWorkbook wb, List<Patient> patients, List<EventSession> sessions,
            List<com.aac.kpi.model.Practitioner> practitioners) {
        XSSFSheet sheet = wb.createSheet("aac_report");
        String[] headers = new String[] {
                "composition_id", "version_id", "last_updated", "meta_code",
                "extension_reporting_month", "extension_total_operating_days", "extension_total_clients",
                "status", "date", "author_value", "author_display", "practitioner_references"
        };
        createHeaderRow(sheet, headers);

        Map<String, Long> clientsByAac = new HashMap<>();
        for (Patient p : patients)
            clientsByAac.merge(nvl(p.getAac()), 1L, Long::sum);

        Map<String, LocalDateTime> latestByAac = new HashMap<>();
        Map<String, Patient> patientIndex = new HashMap<>();
        for (Patient p : patients)
            patientIndex.put(p.getPatientId(), p);
        for (EventSession s : sessions) {
            Patient p = patientIndex.get(s.getEventSessionPatientReferences1());
            if (p == null)
                continue;
            String aac = nvl(p.getAac());
            LocalDateTime dt = parseDateTime(s.getEventSessionStartDate1());
            if (dt != null)
                latestByAac.merge(aac, dt, (a, b) -> a.isAfter(b) ? a : b);
        }

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        int r = 1;
        CellStyle dateTimeStyle = sheet.getWorkbook().createCellStyle();
        dateTimeStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));
        for (Map.Entry<String, Long> e : clientsByAac.entrySet()) {
            String aac = e.getKey();
            int rowNumber = r;
            Row row = sheet.createRow(r++);
            int i = 0;
            row.createCell(i++).setCellValue(RandomDataUtil.uuid32().toUpperCase());
            row.createCell(i++).setCellValue(1);
            row.createCell(i++).setCellValue(nowIsoOffset("+08:00"));
            row.createCell(i++).setCellValue(RandomDataUtil.uuid32().substring(0, 20));
            LocalDateTime ldt = latestByAac.getOrDefault(aac, LocalDateTime.now());
            row.createCell(i++).setCellValue(reportingMonthWithOverride(ldt.format(monthFmt)));
            row.createCell(i++).setCellValue(240);
            row.createCell(i++).setCellValue(e.getValue().intValue());
            row.createCell(i++).setCellValue("final");
            setDateTimeCell(row, i++, reportDateWithOverride(nowStamp()), dateTimeStyle);
            row.createCell(i++).setCellValue(aac);
            row.createCell(i++).setCellValue("Active Ageing Centre " + aac.replaceAll("[^0-9]", ""));
            row.createCell(i).setCellValue(String.join("##", practitioners.stream()
                    .map(com.aac.kpi.model.Practitioner::getPractitionerId)
                    .filter(Objects::nonNull).toList()));
        }
        autoSize(sheet, headers.length);
    }

    private static void writeEventSessionsNricSheet(XSSFWorkbook wb, List<Patient> patients, RegistrationData registrationData) {
        XSSFSheet sheet = wb.createSheet("Event Sessions NRIC");
        java.util.List<String> headerList = new java.util.ArrayList<>();
        headerList.add("patient_identifier_value");
        headerList.add("number_of_attended_indicator");
        int maxCols = Math.max(1, registrationData.maxRegistrationPerPatient());
        for (int n = 1; n <= maxCols; n++) {
            headerList.add("registration_id" + n);
            headerList.add("registration_value" + n);
        }
        String[] headers = headerList.toArray(new String[0]);
        createHeaderRow(sheet, headers);

        int r = 1;
        for (Patient p : patients) {
            Row row = sheet.createRow(r++);
            int c = 0;
            String identifier = nvl(p.getPatientIdentifierValue());
            if (identifier.isBlank())
                identifier = nvl(p.getPatientId());
            String idKey = StringUtils.sanitizeAlphaNum(identifier);
            java.util.List<String> regIds = registrationData.lookupRegistrationIds(idKey);
            int count = regIds.size();

            row.createCell(c++).setCellValue(identifier);
            row.createCell(c++).setCellValue(count);
            for (int idx = 0; idx < maxCols; idx++) {
                if (idx < regIds.size()) {
                    row.createCell(c++).setCellValue(regIds.get(idx));
                    row.createCell(c++).setCellValue(RandomDataUtil.randomInt(0, 1) == 1 ? "TRUE" : "FALSE");
                } else {
                    row.createCell(c++).setCellValue("");
                    row.createCell(c++).setCellValue("");
                }
            }
        }
        autoSize(sheet, headers.length);
    }

    private static void writeVolunteerAttendanceReportSheet(XSSFWorkbook wb, List<Patient> patients,
            List<EventSession> sessions,
            List<com.aac.kpi.model.Practitioner> practitioners) {
        XSSFSheet sheet = wb.createSheet("volunteer_attendance_report");
        String[] headers = new String[] {
                "composition_id", "version_id", "last_updated", "meta_code",
                "extension_reporting_month", "status", "date", "author_value", "author_display",
                "practitioner_references", "number_of_practitioners"
        };
        createHeaderRow(sheet, headers);
        Row row = sheet.createRow(1);
        int i = 0;
        CellStyle dateTimeStyle = sheet.getWorkbook().createCellStyle();
        dateTimeStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));
        row.createCell(i++).setCellValue(RandomDataUtil.uuid32().toUpperCase());
        row.createCell(i++).setCellValue(1);
        row.createCell(i++).setCellValue(nowIsoOffset("+08:00"));
        row.createCell(i++).setCellValue(RandomDataUtil.uuid32().substring(0, 20));
        row.createCell(i++).setCellValue(reportingMonthWithOverride(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))));
        row.createCell(i++).setCellValue("final");
        setDateTimeCell(row, i++, reportDateWithOverride(nowStamp()), dateTimeStyle);
        String aac = patients.isEmpty() ? "AAC" : nvl(patients.get(0).getAac());
        row.createCell(i++).setCellValue(aac);
        row.createCell(i++).setCellValue("Active Ageing Centre " + aac.replaceAll("[^0-9]", ""));
        String joined = String.join("##", practitioners.stream().map(com.aac.kpi.model.Practitioner::getPractitionerId)
                .filter(Objects::nonNull).toList());
        row.createCell(i++).setCellValue(joined);
        row.createCell(i).setCellValue(practitioners.size());
        autoSize(sheet, headers.length);
    }

    private static void writeEventReportSheet(XSSFWorkbook wb, List<Patient> patients, List<EventSession> sessions,
            List<com.aac.kpi.model.Practitioner> practitioners) {
        XSSFSheet sheet = wb.createSheet("event_report");
        String[] headers = new String[] {
                "composition_id", "version_id", "last_updated", "meta_code", "extension_reporting_month", "status",
                "date",
                "author_value", "author_display", "event_id", "event_name"
        };
        createHeaderRow(sheet, headers);
        CellStyle dateTimeStyle = sheet.getWorkbook().createCellStyle();
        dateTimeStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));
        Map<String, Patient> patientIndex = new HashMap<>();
        for (Patient p : patients)
            patientIndex.put(p.getPatientId(), p);
        int r = 1;
        for (EventSession s : sessions) {
            Row row = sheet.createRow(r++);
            int i = 0;
            row.createCell(i++).setCellValue(RandomDataUtil.uuid32().toUpperCase());
            row.createCell(i++).setCellValue(1);
            row.createCell(i++).setCellValue(nowIsoOffset("+08:00"));
            row.createCell(i++).setCellValue(RandomDataUtil.uuid32().substring(0, 20));
            LocalDateTime dt = parseDateTime(s.getEventSessionStartDate1());
            row.createCell(i++).setCellValue(
                    reportingMonthWithOverride((dt != null ? dt : LocalDateTime.now()).format(DateTimeFormatter.ofPattern("yyyy-MM"))));
            row.createCell(i++).setCellValue("final");
            setDateTimeCell(row, i++, reportDateWithOverride(nowStamp()), dateTimeStyle);
            Patient p = patientIndex.get(s.getEventSessionPatientReferences1());
            String aac = p != null ? nvl(p.getAac()) : "AAC";
            row.createCell(i++).setCellValue(aac);
            row.createCell(i++).setCellValue("Active Ageing Centre " + aac.replaceAll("[^0-9]", ""));
            row.createCell(i++).setCellValue(nvl(s.getEventSessionId1()));
            String name = nvl(s.getEventSessionId1()).replaceAll("[0-9-]", "");
            if (name.isEmpty())
                name = nvl(s.getEventSessionVenue1());
            row.createCell(i).setCellValue(name);
        }
        autoSize(sheet, headers.length);
    }

    private static void writeOrganizationReportSheet(XSSFWorkbook wb, List<Patient> patients,
            List<EventSession> sessions,
            List<com.aac.kpi.model.Practitioner> practitioners) {
        XSSFSheet sheet = wb.createSheet("organization_report");
        String[] headers = new String[] {
                "organization_id", "organization_name", "total_sessions_held", "total_volunteers", "total_participants"
        };
        createHeaderRow(sheet, headers);
        Map<String, Long> participants = new HashMap<>();
        Map<String, Long> sessionsCount = new HashMap<>();
        Map<String, Patient> pIndex = new HashMap<>();
        for (Patient p : patients) {
            pIndex.put(p.getPatientId(), p);
            participants.merge(nvl(p.getAac()), 1L, Long::sum);
        }
        for (EventSession s : sessions) {
            Patient p = pIndex.get(s.getEventSessionPatientReferences1());
            if (p == null)
                continue;
            sessionsCount.merge(nvl(p.getAac()), 1L, Long::sum);
        }
        int r = 1;
        for (String aac : participants.keySet()) {
            Row row = sheet.createRow(r++);
            int i = 0;
            row.createCell(i++).setCellValue(aac);
            row.createCell(i++).setCellValue("Active Ageing Centre " + aac.replaceAll("[^0-9]", ""));
            row.createCell(i++).setCellValue(sessionsCount.getOrDefault(aac, 0L));
            row.createCell(i++).setCellValue(practitioners.size());
            row.createCell(i).setCellValue(participants.getOrDefault(aac, 0L));
        }
        autoSize(sheet, headers.length);
    }

    private static void writeLocationReportSheet(XSSFWorkbook wb, List<EventSession> sessions) {
        XSSFSheet sheet = wb.createSheet("location_report");
        String[] headers = new String[] {
                "location_id", "location_name", "sessions_held", "total_attendance"
        };
        createHeaderRow(sheet, headers);
        Map<String, long[]> stats = new HashMap<>(); // [sessions, attendance]
        for (EventSession s : sessions) {
            String v = nvl(s.getEventSessionVenue1());
            long[] arr = stats.computeIfAbsent(v, k -> new long[2]);
            arr[0] += 1;
            if (s.isAttendedIndicator())
                arr[1] += 1;
        }
        int r = 1;
        for (Map.Entry<String, long[]> e : stats.entrySet()) {
            Row row = sheet.createRow(r++);
            String id = e.getKey().replaceAll("[^A-Za-z0-9]", "");
            row.createCell(0).setCellValue(id);
            row.createCell(1).setCellValue(e.getKey());
            row.createCell(2).setCellValue(e.getValue()[0]);
            row.createCell(3).setCellValue(e.getValue()[1]);
        }
        autoSize(sheet, headers.length);
    }

    private static LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank())
            return null;
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            try {
                return java.time.OffsetDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))
                        .toLocalDateTime();
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private static void createHeaderRow(Sheet sheet, String[] headers) {
        Row header = sheet.createRow(0);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);
        // Apply light blue fill to header row for all non-Common sheets
        try {
            ((org.apache.poi.xssf.usermodel.XSSFCellStyle) style)
                    .setFillForegroundColor(
                            new org.apache.poi.xssf.usermodel.XSSFColor(new java.awt.Color(189, 215, 238), null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        } catch (Exception ignored) {
            // If not XSSF, ignore fill color
        }
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    // Overload to place a styled header at an arbitrary row index
    private static void createHeaderRow(Sheet sheet, String[] headers, int rowIndex, CellStyle style) {
        Row header = sheet.createRow(rowIndex);
        CellStyle useStyle = style;
        if (useStyle == null) {
            useStyle = sheet.getWorkbook().createCellStyle();
            Font f = sheet.getWorkbook().createFont();
            f.setBold(true);
            useStyle.setFont(f);
        }
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(useStyle);
        }
    }

    private static void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++)
            sheet.autoSizeColumn(i);
    }

    private static CellStyle createHighlightStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        if (style instanceof XSSFCellStyle) {
            ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(new Color(255, 228, 225), null));
        } else {
            style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        }
        return style;
    }

    private static void applyHighlight(Row row, CellStyle template) {
        if (row == null || template == null) return;
        Sheet sheet = row.getSheet();
        if (sheet == null) return;
        Workbook wb = sheet.getWorkbook();
        for (Cell cell : row) {
            CellStyle base = cell.getCellStyle();
            CellStyle clone = wb.createCellStyle();
            if (base != null) {
                clone.cloneStyleFrom(base);
            }
            clone.setFillPattern(template.getFillPattern());
            if (template instanceof XSSFCellStyle && clone instanceof XSSFCellStyle) {
                XSSFColor color = ((XSSFCellStyle) template).getFillForegroundColorColor();
                ((XSSFCellStyle) clone).setFillForegroundColor(color);
            } else {
                clone.setFillForegroundColor(template.getFillForegroundColor());
            }
            cell.setCellStyle(clone);
        }
    }

    private static RegistrationData buildRegistrationData(List<Patient> patients, RegistrationConfig cfg, String overrideType) {
        RegistrationData data = new RegistrationData();
        for (Patient p : patients) {
            int count = resolveRegistrationCountForType(
                    (overrideType != null && !overrideType.isBlank()) ? overrideType : nvl(p.getKpiType()),
                    cfg);
            if (count <= 0)
                continue;
            List<String> ids = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ids.add(RandomDataUtil.uuid32().toUpperCase());
            }
            data.registerPatient(p, ids);
        }
        return data;
    }

    private static int resolveRegistrationCountForType(String kpiType, RegistrationConfig cfg) {
        String t = kpiType == null ? "" : kpiType.toLowerCase();
        if (t.contains("robust")) {
            return Math.min(Math.max(cfg.robust(), 2), 5);
        } else if (t.contains("frail")) {
            return Math.max(0, cfg.frail());
        } else if (t.contains("bud")) { // budding
            return Math.max(6, cfg.budding());
        } else if (t.contains("befriend")) {
            return Math.max(12, cfg.befriending());
        }
        return Math.max(1, cfg.robust());
    }

    private record RegistrationConfig(int robust, int frail, int budding, int befriending) {
        static RegistrationConfig fromAppState() {
            return new RegistrationConfig(
                    AppState.getRobustRegistrationCount() <= 0 ? 2 : AppState.getRobustRegistrationCount(),
                    AppState.getFrailRegistrationCount() < 0 ? 0 : AppState.getFrailRegistrationCount(),
                    AppState.getBuddingRegistrationCount() <= 0 ? 6 : AppState.getBuddingRegistrationCount(),
                    AppState.getBefriendingRegistrationCount() <= 0 ? 12 : AppState.getBefriendingRegistrationCount()
            );
        }
    }

    private static class RegistrationData {
        private final Map<String, List<String>> byKey = new HashMap<>();
        private int maxPerPatient = 0;

        void registerPatient(Patient p, List<String> registrationIds) {
            if (registrationIds == null || registrationIds.isEmpty() || p == null) return;
            List<String> copy = List.copyOf(registrationIds);
            maxPerPatient = Math.max(maxPerPatient, copy.size());
            String pid = nvl(p.getPatientId());
            String ident = nvl(p.getPatientIdentifierValue());
            String pidSan = StringUtils.sanitizeAlphaNum(pid);
            String identSan = StringUtils.sanitizeAlphaNum(ident);
            putIfPresent(pid, copy);
            putIfPresent(ident, copy);
            putIfPresent(pidSan, copy);
            putIfPresent(identSan, copy);
        }

        private void putIfPresent(String key, List<String> ids) {
            if (key == null || key.isBlank()) return;
            byKey.put(key, ids);
        }

        List<String> lookupRegistrationIds(String patientRef) {
            if (patientRef == null || patientRef.isBlank()) return java.util.List.of();
            String ref = patientRef.split("##")[0].trim();
            String san = StringUtils.sanitizeAlphaNum(ref);
            List<String> ids = byKey.get(ref);
            if (ids == null || ids.isEmpty()) ids = byKey.get(san);
            return ids == null ? java.util.List.of() : ids;
        }

        int maxRegistrationPerPatient() {
            return maxPerPatient;
        }
    }

    private static String reportingMonthWithOverride(String fallback) {
        String override = AppState.getReportingMonthOverride();
        if (override != null && !override.isBlank())
            return override.trim();
        return nvl(fallback);
    }

    private static String reportDateWithOverride(String fallback) {
        String override = AppState.getReportDateOverride();
        if (override != null && !override.isBlank())
            return override.trim();
        return nvl(fallback);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    public static String nowStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String nowIsoOffset(String offset) {
        java.time.OffsetDateTime odt = java.time.OffsetDateTime.now(java.time.ZoneOffset.of(offset));
        return odt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
    }

    private static final java.time.format.DateTimeFormatter ISO_OFFSET_NO_MS = java.time.format.DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private static void setDateCell(Row row, int idx, String value, CellStyle dateStyle) {
        String v = nvl(value);
        if (v.isEmpty()) {
            Cell cell = row.createCell(idx);
            cell.setCellValue("");
            if (dateStyle != null)
                cell.setCellStyle(dateStyle);
            return;
        }
        java.time.LocalDate d = parseLocalDateFlexible(v);
        if (d != null) {
            Cell cell = row.createCell(idx);
            cell.setCellValue(java.sql.Date.valueOf(d));
            if (dateStyle != null)
                cell.setCellStyle(dateStyle);
        } else {
            // Fallback: write the raw string; caller may adjust if needed
            row.createCell(idx).setCellValue(v);
        }
    }

    private static java.time.LocalDate parseLocalDateFlexible(String s) {
        if (s == null || s.isBlank())
            return null;
        try {
            return java.time.LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception ignored) {
        }
        try {
            java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(s,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
            return odt.toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return ldt.toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return ldt.toLocalDate();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void setDateCell(Row row, int idx, LocalDate date, CellStyle dateStyle) {
        if (date == null) {
            Cell cell = row.createCell(idx);
            cell.setCellValue("");
            if (dateStyle != null)
                cell.setCellStyle(dateStyle);
            return;
        }
        Cell cell = row.createCell(idx);
        cell.setCellValue(java.sql.Date.valueOf(date));
        if (dateStyle != null)
            cell.setCellStyle(dateStyle);
    }

    private static void setDateTimeCell(Row row, int idx, String value, CellStyle dateTimeStyle) {
        String v = nvl(value);
        if (v.isEmpty()) {
            row.createCell(idx).setCellValue("");
            return;
        }
        java.time.LocalDateTime dt = parseDateTime(v);
        if (dt == null) {
            try {
                // Try parsing ISO offset
                java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(v,
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
                dt = odt.toLocalDateTime();
            } catch (Exception ignored) {
            }
        }
        if (dt != null) {
            java.util.Date util = java.util.Date.from(dt.atZone(java.time.ZoneId.systemDefault()).toInstant());
            Cell cell = row.createCell(idx);
            cell.setCellValue(util);
            cell.setCellStyle(dateTimeStyle);
        } else {
            row.createCell(idx).setCellValue(v);
        }
    }

    private static String toIsoOffset(String s) {
        if (s == null || s.isBlank())
            return "";
        java.time.ZoneOffset off = java.time.ZoneOffset.of("+08:00");
        // Try parse as OffsetDateTime (ISO)
        try {
            java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(s,
                    java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return odt.withOffsetSameInstant(off).format(ISO_OFFSET_NO_MS);
        } catch (Exception ignored) {
        }
        // Try parse LocalDateTime with seconds
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return ldt.atOffset(off).format(ISO_OFFSET_NO_MS);
        } catch (Exception ignored) {
        }
        // Try parse LocalDateTime without seconds
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return ldt.atOffset(off).format(ISO_OFFSET_NO_MS);
        } catch (Exception ignored) {
        }
        // Try parse LocalDate (set midnight)
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(s,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return d.atStartOfDay().atOffset(off).format(ISO_OFFSET_NO_MS);
        } catch (Exception ignored) {
        }
        return s; // leave as-is if unrecognized
    }
}
