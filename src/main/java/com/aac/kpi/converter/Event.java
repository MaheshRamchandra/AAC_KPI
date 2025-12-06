package com.aac.kpi.converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;

import static com.aac.kpi.converter.ValueUtil.splitRefs;
import static com.aac.kpi.converter.ValueUtil.toInteger;

public class Event {

    private final HashMap<String, String> event;
    Integer numEventSessions;
    private HashMap<String, Integer> patientSessionMapping;

    public Event(HashMap<String, String> event) {
        this.event = event;
        this.numEventSessions = toInteger(event.get("number_of_event_sessions"));
        this.patientSessionMapping = mapPatientSessionCount();
    }

    private HashMap<String, Integer> mapPatientSessionCount() {
        HashMap<String, Integer> patientSessionMapping = new HashMap<String, Integer>();

        if (numEventSessions == null) {
            return patientSessionMapping;
        }

        for (int i = 0; i < numEventSessions; i++) {
            List<String> patientKeys = splitRefs(this.event.get("event_session_patient_references" + (i + 1)));

            for (int j = 0; j < patientKeys.size(); j++) {
                if (patientSessionMapping.get(patientKeys.get(j)) == null) {
                    patientSessionMapping.put(patientKeys.get(j), 1);
                } else {
                    int currentCount = patientSessionMapping.get(patientKeys.get(j));
                    patientSessionMapping.put(patientKeys.get(j), currentCount + 1);
                }
            }
        }

        return patientSessionMapping;
    }

    public HashMap<String, Integer> getPatientSessionMapping() {
        return patientSessionMapping;
    }

    public JsonArray generateEventSessionsSection(HashMap<String, HashMap<String, String>> eventSessionsNricMap) {
        JsonArray eventArray = new JsonArray();

        if (numEventSessions == null || eventSessionsNricMap == null) {
            return eventArray;
        }

        for (int i = 0; i < numEventSessions; i++) {
            JsonObject eventSessionObject = new JsonObject();

            // Extension.
            JsonArray extensionArray = new JsonArray();
            JsonObject eventSessionIdObject = new JsonObject();
            eventSessionIdObject.addProperty("url", "http://ihis.sg/extension/asg-event-session-id");
            eventSessionIdObject.addProperty("valueString", this.event.get("event_session_id" + (i + 1)));

            JsonObject eventSessionModeObject = new JsonObject();
            eventSessionModeObject.addProperty("url", "http://ihis.sg/extension/asg-event-session-mode");
            eventSessionModeObject.addProperty("valueString", this.event.get("event_session_mode" + (i + 1)));

            JsonObject eventSessionStartDateObject = new JsonObject();
            eventSessionStartDateObject.addProperty("url", "http://ihis.sg/extension/asg-event-session-start-date");
            eventSessionStartDateObject.addProperty("valueDateTime", this.event.get("event_session_start_date" + (i + 1)));

            JsonObject eventSessionEndDateObject = new JsonObject();
            eventSessionEndDateObject.addProperty("url", "http://ihis.sg/extension/asg-event-session-end-date");
            eventSessionEndDateObject.addProperty("valueDateTime", this.event.get("event_session_end_date" + (i + 1)));

            JsonObject eventSessionDurationObject = new JsonObject();
            eventSessionDurationObject.addProperty("url", "http://ihis.sg/extension/asg-event-session-duration");
            eventSessionDurationObject.addProperty("valueInteger", toInteger(this.event.get("event_session_duration" + (i + 1))));

            JsonObject eventSessionVenueObject = new JsonObject();
            eventSessionVenueObject.addProperty("url", "http://ihis.sg/extension/asg-event-session-venue");
            eventSessionVenueObject.addProperty("valueString", this.event.get("event_session_venue" + (i + 1)));

            JsonObject eventSessionCapacityObject = new JsonObject();
            eventSessionCapacityObject.addProperty("url", "http://ihis.sg/extension/asg-event-session-capacity");
            eventSessionCapacityObject.addProperty("valueInteger", toInteger(event.get("event_session_capacity" + (i + 1))));

            extensionArray.add(eventSessionIdObject);
            extensionArray.add(eventSessionModeObject);
            extensionArray.add(eventSessionStartDateObject);
            extensionArray.add(eventSessionEndDateObject);
            extensionArray.add(eventSessionDurationObject);
            extensionArray.add(eventSessionVenueObject);
            extensionArray.add(eventSessionCapacityObject);
            eventSessionObject.add("extension", extensionArray);

            // Code.
            JsonObject codeObject = new JsonObject();
            JsonArray codingArray = new JsonArray();
            JsonObject codingObject = new JsonObject();
            codingObject.addProperty("system", "http://ihis.sg/CodeSystem/asg-event-report-section");
            codingObject.addProperty("code", "section-aac-event-session");
            codingArray.add(codingObject);
            codeObject.add("coding", codingArray);
            eventSessionObject.add("code", codeObject);

            // Entry.
            JsonArray entryArray = new JsonArray();
            List<String> patientKeys = splitRefs(this.event.get("event_session_patient_references" + (i + 1)));
            for (int j = 0; j < patientKeys.size(); j++) {
                String currentPatientNric = patientKeys.get(j);
                HashMap<String, String> patientRegistration = eventSessionsNricMap.get(currentPatientNric);
                if (patientRegistration == null) {
                    continue;
                }

                Integer numRegistrationId = toInteger(patientRegistration.get("number_of_attended_indicator"));
                if (numRegistrationId == null) {
                    continue;
                }

                for (int k = 0; k < numRegistrationId; k++) {
                    JsonObject referenceObject = new JsonObject();
                    referenceObject.addProperty("reference", "#" + patientRegistration.get("registration_id" + (k + 1)));
                    entryArray.add(referenceObject);
                }
            }
            eventSessionObject.add("entry", entryArray);
            eventArray.add(eventSessionObject);
        }

        return eventArray;
    }
}
