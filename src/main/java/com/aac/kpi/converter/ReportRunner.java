package com.aac.kpi.converter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.aac.kpi.converter.ReportConstants.AAC_TYPE;
import static com.aac.kpi.converter.ReportConstants.ENCOUNTER_TYPE;
import static com.aac.kpi.converter.ReportConstants.EVENT_SESSIONS_NRIC_TYPE;
import static com.aac.kpi.converter.ReportConstants.EVENT_SESSIONS_TYPE;
import static com.aac.kpi.converter.ReportConstants.EVENT_TYPE;
import static com.aac.kpi.converter.ReportConstants.FILLER_ROWS;
import static com.aac.kpi.converter.ReportConstants.LOCATION_TYPE;
import static com.aac.kpi.converter.ReportConstants.ORGANIZATION_TYPE;
import static com.aac.kpi.converter.ReportConstants.PATIENT_TYPE;
import static com.aac.kpi.converter.ReportConstants.PRACTITIONER_TYPE;
import static com.aac.kpi.converter.ReportConstants.QUESTIONNAIRE_TYPE;
import static com.aac.kpi.converter.ReportConstants.RESIDENT_TYPE;
import static com.aac.kpi.converter.ReportConstants.START_ROW;
import static com.aac.kpi.converter.ReportConstants.VOLUNTEER_ATTENDANCE_TYPE;

/**
 * Central place where report generation orchestration lives so it can be reused
 * by both the CLI entry point and the JavaFX UI.
 */
public class ReportRunner {

    public void generateReports(ReportConfig config) throws Exception {
        Path inputPath = Path.of(config.inputPath());
        if (!Files.isRegularFile(inputPath)) {
            throw new IllegalArgumentException("Input Excel file not found: " + inputPath.toAbsolutePath());
        }

        Files.createDirectories(Path.of(config.outputFolder()));

        ExcelOperations excelOperations = new ExcelOperations(inputPath.toString());
        HashMap<String, HashMap<String, String>> aacReportsMap =
                excelOperations.getReportsMap(AAC_TYPE, config.aacReports(), START_ROW);

        int residentReportsStartRow = START_ROW + config.aacReports() + FILLER_ROWS;
        HashMap<String, HashMap<String, String>> residentReportsMap =
                excelOperations.getReportsMap(RESIDENT_TYPE, config.residentReports(), residentReportsStartRow);

        int volunteerAttendanceReportsStartRow = residentReportsStartRow + config.residentReports() + FILLER_ROWS;
        HashMap<String, HashMap<String, String>> volunteerAttendanceReportsMap =
                excelOperations.getReportsMap(VOLUNTEER_ATTENDANCE_TYPE, config.volunteerAttendanceReports(), volunteerAttendanceReportsStartRow);

        int eventReportsStartRow = volunteerAttendanceReportsStartRow + config.volunteerAttendanceReports() + FILLER_ROWS;
        HashMap<String, HashMap<String, String>> eventReportsMap =
                excelOperations.getReportsMap(EVENT_TYPE, config.eventReports(), eventReportsStartRow);

        int organizationReportsStartRow = eventReportsStartRow + config.eventReports() + FILLER_ROWS;
        HashMap<String, HashMap<String, String>> organizationReportsMap =
                excelOperations.getReportsMap(ORGANIZATION_TYPE, config.organizationReports(), organizationReportsStartRow);

        int locationReportsStartRow = organizationReportsStartRow + config.organizationReports() + FILLER_ROWS;
        HashMap<String, HashMap<String, String>> locationReportsMap =
                excelOperations.getReportsMap(LOCATION_TYPE, config.locationReports(), locationReportsStartRow);

        HashMap<String, HashMap<String, String>> eventSessionsMap = excelOperations.getMasterDataMap(EVENT_SESSIONS_TYPE);
        HashMap<String, Event> events = new HashMap<>();
        for (Map.Entry<String, HashMap<String, String>> eventEntry : eventSessionsMap.entrySet()) {
            String eventCompositionId = eventEntry.getKey();
            HashMap<String, String> eventDetails = eventEntry.getValue();
            Event event = new Event(eventDetails);
            events.put(eventCompositionId, event);
        }

        HashMap<String, Integer> globalPatientSessionMapping = new HashMap<>();
        for (Map.Entry<String, Event> eventEntry : events.entrySet()) {
            Event event = eventEntry.getValue();
            HashMap<String, Integer> eventPatientSessionMapping = event.getPatientSessionMapping();
            for (Map.Entry<String, Integer> patientEntry : eventPatientSessionMapping.entrySet()) {
                String patientId = patientEntry.getKey();
                Integer numSessionsAttended = patientEntry.getValue();

                if (globalPatientSessionMapping.get(patientId) == null) {
                    globalPatientSessionMapping.put(patientId, numSessionsAttended);
                } else {
                    Integer currentCount = globalPatientSessionMapping.get(patientId);
                    globalPatientSessionMapping.put(patientId, currentCount + numSessionsAttended);
                }
            }
        }

        HashMap<String, HashMap<String, String>> eventSessionsNricMap = excelOperations.getMasterDataMap(EVENT_SESSIONS_NRIC_TYPE);

        HashMap<String, HashMap<String, String>> patientsMap = excelOperations.getMasterDataMap(PATIENT_TYPE);
        HashMap<String, Patient> patients = new HashMap<>();
        HashMap<String, Patient> patientsByNric = new HashMap<>(); // For event reports only
        for (Map.Entry<String, HashMap<String, String>> patientEntry : patientsMap.entrySet()) {
            String patientId = patientEntry.getKey();
            String patientNric = patientEntry.getValue().get("patient_identifier_value");
            HashMap<String, String> patientDetails = patientEntry.getValue();

            if (globalPatientSessionMapping.get(patientId) == null) {
                patientDetails.put("number_of_sessions_attended", "0");
            } else {
                patientDetails.put("number_of_sessions_attended", String.valueOf(globalPatientSessionMapping.get(patientId)));
            }

            Patient patient = new Patient(patientDetails);
            patients.put(patientId, patient);
            patientsByNric.put(patientNric, patient);
        }

        HashMap<String, HashMap<String, String>> practitionersMap = excelOperations.getMasterDataMap(PRACTITIONER_TYPE);
        HashMap<String, Practitioner> practitioners = new HashMap<>();
        for (Map.Entry<String, HashMap<String, String>> practitionerEntry : practitionersMap.entrySet()) {
            String practitionerId = practitionerEntry.getKey();
            HashMap<String, String> practitionerDetails = practitionerEntry.getValue();
            Practitioner practitioner = new Practitioner(practitionerDetails);
            practitioners.put(practitionerId, practitioner);
        }

        HashMap<String, HashMap<String, String>> encountersMap = excelOperations.getMasterDataMap(ENCOUNTER_TYPE);
        HashMap<String, Encounter> encounters = new HashMap<>();
        for (Map.Entry<String, HashMap<String, String>> encounterEntry : encountersMap.entrySet()) {
            String encounterId = encounterEntry.getKey();
            HashMap<String, String> encounterDetails = encounterEntry.getValue();
            Encounter encounter = new Encounter(encounterDetails);
            encounters.put(encounterId, encounter);
        }

        HashMap<String, HashMap<String, String>> questionnairesMap = excelOperations.getMasterDataMap(QUESTIONNAIRE_TYPE);
        HashMap<String, Questionnaire> questionnaires = new HashMap<>();
        for (Map.Entry<String, HashMap<String, String>> questionnaireEntry : questionnairesMap.entrySet()) {
            String questionnaireId = questionnaireEntry.getKey();
            HashMap<String, String> questionnaireDetails = questionnaireEntry.getValue();
            Questionnaire questionnaire = new Questionnaire(questionnaireDetails);
            questionnaires.put(questionnaireId, questionnaire);
        }

        MainJson.generateAacReportsJson(aacReportsMap, practitioners, config.outputFolder());
        MainJson.generateResidentReportsJson(residentReportsMap, patients, encounters, questionnaires, config.outputFolder());
        MainJson.generateVolunteerAttendanceReportsJson(volunteerAttendanceReportsMap, practitioners, config.outputFolder());
        MainJson.generateEventReportsJson(eventReportsMap, eventSessionsNricMap, patientsByNric, events, config.outputFolder());
        MainJson.generateOrganizationReportsJson(organizationReportsMap, config.outputFolder());
        MainJson.generateLocationReportsJson(locationReportsMap, config.outputFolder());
    }
}
