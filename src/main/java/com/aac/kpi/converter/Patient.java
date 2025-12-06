package com.aac.kpi.converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;

import static com.aac.kpi.converter.DateTime.convertToDate;
import static com.aac.kpi.converter.ValueUtil.normalizeNumberString;
import static com.aac.kpi.converter.ValueUtil.splitRefs;
import static com.aac.kpi.converter.ValueUtil.cleanPostalCode;
import static com.aac.kpi.converter.ValueUtil.toInteger;

public class Patient {

    String patient_id;
    String patient_identifier_value;
    String patient_name;
    String patient_telecom_system;
    String patient_telecom_value;
    String patient_gender;
    String patient_birthdate;
    Integer patient_age;
    String patient_postalcode;
    String patient_residentialstatuscode;
    String patient_residentialstatusdisplay;
    String patient_racecode;
    String patient_racedisplay;
    List<String> attended_event_references;
    Integer number_of_sessions_attended;

    public Patient(HashMap<String, String> patient) {
        this.patient_id = patient.get("patient_id");
        this.patient_identifier_value = patient.get("patient_identifier_value");
        this.patient_name = patient.get("patient_name");
        this.patient_telecom_system = patient.get("patient_telecom_system");
        this.patient_telecom_value = normalizeNumberString(patient.get("patient_telecom_value"));
        this.patient_gender = patient.get("patient_gender");
        this.patient_birthdate = convertToDate(patient.get("patient_birthdate"));
        this.patient_postalcode = patient.get("patient_postalcode");
        this.patient_residentialstatuscode = patient.get("patient_residentialstatuscode");
        this.patient_residentialstatusdisplay = patient.get("patient_residentialstatusdisplay");
        this.patient_racecode = patient.get("patient_racecode");
        this.patient_racedisplay = patient.get("patient_racedisplay");
        this.attended_event_references = splitRefs(patient.get("attended_event_references"));
        String birthDate = this.patient_birthdate;
        if (birthDate != null) {
            Period period = Period.between(LocalDate.parse(birthDate), LocalDate.now());
            this.patient_age = period.getYears();
        } else {
            this.patient_age = null;
        }
        this.number_of_sessions_attended = toInteger(patient.get("number_of_sessions_attended"));
    }

    public JsonObject generatePatientObject() {
        JsonObject patientObject = new JsonObject();

        patientObject.addProperty("resourceType", "Patient");
        patientObject.addProperty("id", this.patient_id);

        JsonArray identifierArray = new JsonArray();
        JsonObject identifierObject = new JsonObject();
        identifierObject.addProperty("system", "http://ihis.sg/identifier/nric");
        identifierObject.addProperty("value", this.patient_identifier_value);
        identifierArray.add(identifierObject);
        patientObject.add("identifier", identifierArray);

        JsonArray nameArray = new JsonArray();
        JsonObject nameObject = new JsonObject();
        nameObject.addProperty("text", this.patient_name);
        nameArray.add(nameObject);
        patientObject.add("name", nameArray);

        patientObject.addProperty("birthDate", convertToDate(patient_birthdate));

        JsonArray addressArray = new JsonArray();
        JsonObject addressObject = new JsonObject();
        addressObject.addProperty("type", "physical"); // This could be non-static.
        String postal = cleanPostalCode(patient_postalcode);
        if (postal != null) {
            addressObject.addProperty("postalCode", postal);
            addressArray.add(addressObject);
            patientObject.add("address", addressArray);
        }

        JsonArray extensionArray = new JsonArray();

        // Residential status object.
        JsonObject asgResidentialStatusObject = new JsonObject();
        asgResidentialStatusObject.addProperty("url", "http://ihis.sg/extension/asg-residential-status");
        JsonObject residentialValueCodeableConceptObject = new JsonObject();
        JsonArray residentialCodingArray = new JsonArray();
        JsonObject residentialCodingObject = new JsonObject();
        residentialCodingObject.addProperty("system", "http://ihis.sg/CodeSystem/asg-residential-status");
        residentialCodingObject.addProperty("code", patient_residentialstatuscode);
        residentialCodingObject.addProperty("display", patient_residentialstatusdisplay);
        residentialCodingArray.add(residentialCodingObject);
        residentialValueCodeableConceptObject.add("coding", residentialCodingArray);
        asgResidentialStatusObject.add("valueCodeableConcept", residentialValueCodeableConceptObject);
        extensionArray.add(asgResidentialStatusObject);

        // Race object.
        JsonObject raceObject = new JsonObject();
        raceObject.addProperty("url", "http://ihis.sg/extension/race");
        JsonObject raceValueCodeableConceptObject = new JsonObject();
        JsonArray raceCodingArray = new JsonArray();
        JsonObject raceCodingObject = new JsonObject();
        raceCodingObject.addProperty("system", "http://ihis.sg/CodeSystem/race");
        raceCodingObject.addProperty("code", patient_racecode);
        raceCodingObject.addProperty("display", patient_racedisplay);
        raceCodingArray.add(raceCodingObject);
        raceValueCodeableConceptObject.add("coding", raceCodingArray);
        raceObject.add("valueCodeableConcept", raceValueCodeableConceptObject);
        extensionArray.add(raceObject);

        patientObject.add("extension", extensionArray);

        return patientObject;
    }

    public JsonObject generatePatientEventDetailsObject(HashMap<String, HashMap<String, String>> eventSessionsNricMap, int registrationIndex) {
        JsonObject patientObject = new JsonObject();

        HashMap<String, String> registrationDetails = eventSessionsNricMap.get(this.patient_identifier_value);

        patientObject.addProperty("resourceType", "Patient");
        patientObject.addProperty("id", registrationDetails != null ? registrationDetails.get("registration_id" + registrationIndex) : null);

        JsonArray identifierArray = new JsonArray();
        JsonObject identifierObject = new JsonObject();
        identifierObject.addProperty("system", "http://ihis.sg/identifier/nric");
        identifierObject.addProperty("value", this.patient_identifier_value);
        identifierArray.add(identifierObject);
        patientObject.add("identifier", identifierArray);

        JsonArray nameArray = new JsonArray();
        JsonObject nameObject = new JsonObject();
        nameObject.addProperty("text", this.patient_name);
        nameArray.add(nameObject);
        patientObject.add("name", nameArray);

        JsonArray telecomArray = new JsonArray();
        JsonObject telecomObject = new JsonObject();
        telecomObject.addProperty("system", this.patient_telecom_system);
        telecomObject.addProperty("value", this.patient_telecom_value);
        telecomArray.add(telecomObject);
        patientObject.add("telecom", telecomArray);

        patientObject.addProperty("gender", this.patient_gender);

        JsonArray addressArray = new JsonArray();
        JsonObject addressObject = new JsonObject();
        addressObject.addProperty("type", "physical"); // This could be non-static.
        String postal = cleanPostalCode(patient_postalcode);
        if (postal != null) {
            addressObject.addProperty("postalCode", postal);
            addressArray.add(addressObject);
            patientObject.add("address", addressArray);
        }

        JsonArray extensionArray = new JsonArray();
        JsonObject asgPersonAgeObject = new JsonObject();
        asgPersonAgeObject.addProperty("url", "http://ihis.sg/extension/asg-person-age");
        asgPersonAgeObject.addProperty("valueInteger", this.patient_age);
        extensionArray.add(asgPersonAgeObject);

        patientObject.add("extension", extensionArray);

        return patientObject;
    }
}
