package com.aac.kpi.converter;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.apache.commons.math3.util.Pair;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.aac.kpi.converter.ValueUtil.toFloat;
import static com.aac.kpi.converter.ValueUtil.toInteger;

public class Practitioner {

    String practitioner_id;
    String practitioner_identifier_value;
    String practitioner_identifier_system;
    String practitioner_manpower_position;
    String practitioner_volunteer_name;
    Float practitioner_manpower_capacity;
    Integer practitioner_volunteer_age;
    private final ArrayList<Pair<String, LocalDate>> activitiesVolunteeredFor = new ArrayList<Pair<String, LocalDate>>();;

    public Practitioner(HashMap<String, String> practitioner) {
        this.practitioner_id = practitioner.get("practitioner_id");
        this.practitioner_identifier_value = practitioner.get("practitioner_identifier_value");
        this.practitioner_identifier_system = practitioner.get("practitioner_identifier_system");
        this.practitioner_manpower_position = practitioner.get("practitioner_manpower_position");
        this.practitioner_volunteer_name = practitioner.get("practitioner_volunteer_name");
        this.practitioner_manpower_capacity = toFloat(practitioner.get("practitioner_manpower_capacity"));
        this.practitioner_volunteer_age = toInteger(practitioner.get("practitioner_volunteer_age"));
    }

    public JsonObject generatePractitionerObject() {
        JsonObject practitionerObject = new JsonObject();

        practitionerObject.addProperty("resourceType", "Practitioner");
        practitionerObject.addProperty("id", this.practitioner_id);

        JsonArray identifierArray = new JsonArray();
        JsonObject identifierObject = new JsonObject();
        identifierObject.addProperty("system", this.practitioner_identifier_system);
        identifierObject.addProperty("value", this.practitioner_identifier_value);
        identifierArray.add(identifierObject);
        practitionerObject.add("identifier", identifierArray);

        JsonArray extensionArray = new JsonArray();
        JsonObject asgManpowerPositionObject = new JsonObject();
        asgManpowerPositionObject.addProperty("url", "http://ihis.sg/extension/asg-manpower-position");
        asgManpowerPositionObject.addProperty("valueString", practitioner_manpower_position);

        JsonObject asgVolunteerName = new JsonObject();
        asgVolunteerName.addProperty("url", "http://ihis.sg/extension/asg-volunteer-name");
        asgVolunteerName.addProperty("valueString", practitioner_volunteer_name);

        JsonObject asgManpowerCapacity = new JsonObject();
        asgManpowerCapacity.addProperty("url", "http://ihis.sg/extension/asg-manpower-capacity");
        asgManpowerCapacity.addProperty("valueDecimal", practitioner_manpower_capacity);

        JsonObject asgVolunteerAge = new JsonObject();
        asgVolunteerAge.addProperty("url", "http://ihis.sg/extension/asg-volunteer-age");
        asgVolunteerAge.addProperty("valueInteger", practitioner_volunteer_age);

        JsonObject asgManpowerRemarks = new JsonObject();
        asgManpowerRemarks.addProperty("url", "http://ihis.sg/extension/asg-manpower-remarks");
        asgManpowerRemarks.addProperty("valueString", "Remarks for volunteer");

        extensionArray.add(asgManpowerPositionObject);
        extensionArray.add(asgVolunteerName);
        extensionArray.add(asgManpowerCapacity);
        extensionArray.add(asgVolunteerAge);
        extensionArray.add(asgManpowerRemarks);
        practitionerObject.add("extension", extensionArray);

        return practitionerObject;
    }

    public JsonObject generatePractitionerVolunteerDetailsObject() {
        JsonObject practitionerObject = new JsonObject();

        practitionerObject.addProperty("resourceType", "Practitioner");
        practitionerObject.addProperty("id", this.practitioner_id);

        JsonArray identifierArray = new JsonArray();
        JsonObject identifierObject = new JsonObject();
        identifierObject.addProperty("system", this.practitioner_identifier_system);
        identifierObject.addProperty("value", this.practitioner_identifier_value);
        identifierArray.add(identifierObject);
        practitionerObject.add("identifier", identifierArray);

        JsonArray extensionArray = new JsonArray();
        JsonObject asgVolunteerName = new JsonObject();
        asgVolunteerName.addProperty("url", "http://ihis.sg/extension/asg-volunteer-name");
        asgVolunteerName.addProperty("valueString", practitioner_volunteer_name);

        JsonObject asgVolunteerAge = new JsonObject();
        asgVolunteerAge.addProperty("url", "http://ihis.sg/extension/asg-volunteer-age");
        asgVolunteerAge.addProperty("valueInteger", practitioner_volunteer_age);

        extensionArray.add(asgVolunteerName);
        extensionArray.add(asgVolunteerAge);
        practitionerObject.add("extension", extensionArray);

        return practitionerObject;
    }

    public void addVolunteeredActivity(String activityVolunteered, LocalDate volunteeredDate) {
        Pair<String, LocalDate> activityDetails = new Pair<String, LocalDate>(activityVolunteered, volunteeredDate);
        this.activitiesVolunteeredFor.add(activityDetails);
    }

    public List<Pair<String, LocalDate>> getVolunteeredActivitiesList() {
        return this.activitiesVolunteeredFor;
    }
}
