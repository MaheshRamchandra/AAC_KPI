package com.aac.kpi.converter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;

public class MainJson {
    public static String generateAacReportsJson(HashMap<String, HashMap<String, String>> aacReports,
                                                HashMap<String, Practitioner> practitioners,
                                                String outputFolder) throws IOException, ParseException {

        StringBuilder fullPayload = new StringBuilder();
        File folder = new File(outputFolder,"aac_reports");
        folder.mkdirs();

        for (int i = 0; i < aacReports.size(); i++) {
            String currentReportName = "aac_report_" + (i + 1);
            System.out.println(currentReportName);
            JsonObject aacReportObject = Skeleton.generateAacReportObject(aacReports.get(currentReportName), practitioners);
            Gson gson = new Gson();
            String aacReportJson = toJsonWithoutNulls(aacReportObject, gson);

            writeJsonToFile(folder.getAbsolutePath(), currentReportName, aacReportJson);
            fullPayload.append("\n" + aacReportJson);
        }

        return fullPayload.toString();
    }

    public static String generateResidentReportsJson(HashMap<String, HashMap<String, String>> residentReports,
                                                     HashMap<String, Patient> patientsMap,
                                                     HashMap<String, Encounter> encountersMap,
                                                     HashMap<String, Questionnaire> questionnairesMap,
                                                     String outputFolder) throws IOException, ParseException {

        StringBuilder fullPayload = new StringBuilder();
        File folder = new File(outputFolder,"resident_reports");
        folder.mkdirs();

        for (int i = 0; i < residentReports.size(); i++) {
            String currentReportName = "resident_report_" + (i + 1);
            System.out.println(currentReportName);
            JsonObject residentReportObject = Skeleton.generateResidentReportObject(residentReports.get(currentReportName),
                                                                                    patientsMap,
                                                                                    encountersMap,
                                                                                    questionnairesMap);
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            String residentReportJson = toJsonWithoutNulls(residentReportObject, gson);

            writeJsonToFile(folder.getAbsolutePath(), currentReportName, residentReportJson);
            fullPayload.append("\n" + residentReportJson);
        }

        return fullPayload.toString();
    }

    public static String generateVolunteerAttendanceReportsJson(HashMap<String, HashMap<String, String>> volunteerAttendanceReports,
                                                                HashMap<String, Practitioner> practitioners,
                                                                String outputFolder) throws IOException, ParseException {
        StringBuilder fullPayload = new StringBuilder();
        File folder = new File(outputFolder,"volunteer_attendance_reports");
        folder.mkdirs();

        for (int i = 0; i < volunteerAttendanceReports.size(); i++) {
            String currentReportName = "volunteer_attendance_report_" + (i + 1);
            System.out.println(currentReportName);
            JsonObject volunteerAttendanceReportObject = Skeleton.generateVolunteerAttendanceReportObject(volunteerAttendanceReports.get(currentReportName),
                                                                                                          practitioners);
            Gson gson = new Gson();
            String volunteerAttendanceReportJson = toJsonWithoutNulls(volunteerAttendanceReportObject, gson);

            writeJsonToFile(folder.getAbsolutePath(), currentReportName, volunteerAttendanceReportJson);
            fullPayload.append("\n" + volunteerAttendanceReportJson);
        }

        return fullPayload.toString();
    }

    public static String generateEventReportsJson(HashMap<String, HashMap<String, String>> eventReports,
                                                  HashMap<String, HashMap<String, String>> eventSessionsNricMap,
                                                  HashMap<String, Patient> patientsByNric,
                                                  HashMap<String, Event> events,
                                                  String outputFolder) throws IOException, ParseException {

        StringBuilder fullPayload = new StringBuilder();
        File folder = new File(outputFolder,"event_reports");
        folder.mkdirs();

        for (int i = 0; i < eventReports.size(); i++) {
            String currentReportName = "event_report_" + (i + 1);
            System.out.println(currentReportName);
            JsonObject eventReportObject = Skeleton.generateEventReportObject(eventReports.get(currentReportName),
                                                                              eventSessionsNricMap,
                                                                              patientsByNric,
                                                                              events);
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            String eventReportJson = toJsonWithoutNulls(eventReportObject, gson);

            writeJsonToFile(folder.getAbsolutePath(), currentReportName, eventReportJson);
            fullPayload.append("\n" + eventReportJson);
        }

        return fullPayload.toString();
    }

    public static String generateOrganizationReportsJson(HashMap<String, HashMap<String, String>> organizationReports,
                                                         String outputFolder) throws IOException, ParseException {

        StringBuilder fullPayload = new StringBuilder();
        File folder = new File(outputFolder,"organization_reports");
        folder.mkdirs();

        for (int i = 0; i < organizationReports.size(); i++) {
            String currentReportName = "organization_report_" + (i + 1);
            System.out.println(currentReportName);
            JsonObject organizationReportObject = Skeleton.generateOrganizationReportObject(organizationReports.get(currentReportName));

            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            String organizationReportJson = toJsonWithoutNulls(organizationReportObject, gson);

            writeJsonToFile(folder.getAbsolutePath(), currentReportName, organizationReportJson);
            fullPayload.append("\n" + organizationReportJson);
        }

        return fullPayload.toString();
    }

    public static String generateLocationReportsJson(HashMap<String, HashMap<String, String>> locationReports,
                                                     String outputFolder) throws IOException, ParseException {

        StringBuilder fullPayload = new StringBuilder();
        File folder = new File(outputFolder,"location_reports");
        folder.mkdirs();

        for (int i = 0; i < locationReports.size(); i++) {
            String currentReportName = "location_report_" + (i + 1);
            System.out.println(currentReportName);
            JsonObject locationReportObject = Skeleton.generateLocationReportObject(locationReports.get(currentReportName));

            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            String locationReportJson = toJsonWithoutNulls(locationReportObject, gson);

            writeJsonToFile(folder.getAbsolutePath(), currentReportName, locationReportJson);
            fullPayload.append("\n" + locationReportJson);
        }

        return fullPayload.toString();
    }

    public static void writeJsonToFile(String outputFolder, String fileName, String txtToWrite) throws IOException {
        File file = new File(outputFolder + "/" + fileName + ".json");

        if (!file.exists()) {
            file.createNewFile();
        }

        BufferedWriter output = new BufferedWriter(new FileWriter(file));
        output.write(txtToWrite);
        output.close();
    }

    private static String toJsonWithoutNulls(JsonObject source, Gson gson) {
        JsonElement cleaned = stripNulls(source);
        return gson.toJson(cleaned);
    }

    private static JsonElement stripNulls(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            JsonObject src = element.getAsJsonObject();
            JsonObject dest = new JsonObject();
            for (var entry : src.entrySet()) {
                JsonElement cleaned = stripNulls(entry.getValue());
                if (cleaned != null && !cleaned.isJsonNull()) {
                    dest.add(entry.getKey(), cleaned);
                }
            }
            return dest;
        }
        if (element.isJsonArray()) {
            JsonArray dest = new JsonArray();
            for (JsonElement item : element.getAsJsonArray()) {
                JsonElement cleaned = stripNulls(item);
                if (cleaned != null && !cleaned.isJsonNull()) {
                    dest.add(cleaned);
                }
            }
            return dest;
        }
        return element;
    }
}
