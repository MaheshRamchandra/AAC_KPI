package com.aac.kpi.converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;

public class Encounter {
    JsonObject encounterObject = new JsonObject();

    String encounter_id;
    String encounter_status;
    String encounter_display;
    String encounter_start;
    String encounter_purpose;
    String encounter_contactedstaffname;
    String encounter_referredby;

    public Encounter(HashMap<String, String> encounter) {
        this.encounter_id = encounter.get("encounter_id");
        this.encounter_status = encounter.get("encounter_status");
        this.encounter_display = encounter.get("encounter_display");
        this.encounter_start = encounter.get("encounter_start");
        this.encounter_purpose = encounter.get("encounter_purpose");
        this.encounter_contactedstaffname = encounter.get("encounter_contactedstaffname");
        this.encounter_referredby = encounter.get("encounter_referredby");
    }

    public JsonObject generateEncounterObject() {
        encounterObject.addProperty("resourceType", "Encounter");
        encounterObject.addProperty("id", this.encounter_id);
        encounterObject.addProperty("status", this.encounter_status);

        JsonObject classObject = new JsonObject();
        classObject.addProperty("display", this.encounter_display);
        encounterObject.add("class", classObject);

        JsonObject periodObject = new JsonObject();
        periodObject.addProperty("start", this.encounter_start);
        encounterObject.add("period", periodObject);

        JsonArray extensionArray = new JsonArray();
        JsonObject asgPurposeOfContactObject = new JsonObject();
        asgPurposeOfContactObject.addProperty("url", "http://ihis.sg/extension/asg-purpose-of-contact");
        asgPurposeOfContactObject.addProperty("valueString", this.encounter_purpose);
        JsonObject asgContactedStaffNameObject = new JsonObject();
        asgContactedStaffNameObject.addProperty("url", "http://ihis.sg/extension/asg-contacted-staff-name");
        asgContactedStaffNameObject.addProperty("valueString", this.encounter_contactedstaffname);
        JsonObject asgReferredByObject = new JsonObject();
        asgReferredByObject.addProperty("url", "http://ihis.sg/extension/asg-referred-by");
        asgReferredByObject.addProperty("valueString", this.encounter_referredby);
        extensionArray.add(asgPurposeOfContactObject);
        extensionArray.add(asgContactedStaffNameObject);
        extensionArray.add(asgReferredByObject);
        encounterObject.add("extension", extensionArray);

        return encounterObject;
    }
}
