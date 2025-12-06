package com.aac.kpi.converter;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.aac.kpi.converter.DateTime.convertToDate;
import static com.aac.kpi.converter.ValueUtil.splitRefs;
import static com.aac.kpi.converter.ValueUtil.stripDecimal;
import static com.aac.kpi.converter.ValueUtil.toInteger;

public class Skeleton {

    // In MainJson, iterate through map of AAC reports and pass each AAC report to this function.
    public static JsonObject generateAacReportObject(HashMap<String, String> aacReportMap,
                                                     HashMap<String, Practitioner> practitioners) {
        JsonObject resource = new JsonObject();
        resource.addProperty("resourceType", "Composition");
        resource.addProperty("id", aacReportMap.get("composition_id"));

        JsonObject meta = new JsonObject();
        meta.addProperty("versionId", stripDecimal(aacReportMap.get("version_id")));
        meta.addProperty("lastUpdated", aacReportMap.get("last_updated"));

        JsonArray profileArray = new JsonArray();
        profileArray.add("http://ihis.sg/StructureDefinition/Composition-put-asg-aac-report");
        meta.add("profile", profileArray);

        JsonArray tagArray = new JsonArray();
        JsonObject tagObject = new JsonObject();
        tagObject.addProperty("system", "http://ihis.sg/coding/correlationid");
        tagObject.addProperty("code", aacReportMap.get("meta_code"));
        tagArray.add(tagObject);

        meta.add("tag", tagArray);
        resource.add("meta", meta);

        // Contained section.
        JsonArray containedArray = new JsonArray();
        List<String> practitionerKeys = splitRefs(aacReportMap.get("practitioner_references"));
        List<String> resolvedPractitionerIds = new java.util.ArrayList<>();

        for (String practitionerId : practitionerKeys) {
            Practitioner practitioner = practitioners.get(practitionerId);
            if (practitioner == null) {
                continue;
            }
            JsonObject practitionerObject = practitioner.generatePractitionerObject();
            containedArray.add(practitionerObject);
            resolvedPractitionerIds.add(practitionerId);
        }
        resource.add("contained", containedArray);

        // Extension section.
        JsonArray extensionArray = new JsonArray();
        JsonObject asgReportingMonthObject = new JsonObject();
        asgReportingMonthObject.addProperty("url", "http://ihis.sg/extension/asg-reporting-month");
        asgReportingMonthObject.addProperty("valueDate", aacReportMap.get("extension_reporting_month"));

        JsonObject asgTotalOperatingDaysObject = new JsonObject();
        asgTotalOperatingDaysObject.addProperty("url", "http://ihis.sg/extension/asg-total-operating-days");
        asgTotalOperatingDaysObject.addProperty("valueInteger", toInteger(aacReportMap.get("extension_total_operating_days")));

        JsonObject asgTotalClients = new JsonObject();
        asgTotalClients.addProperty("url", "http://ihis.sg/extension/asg-total-clients");
        asgTotalClients.addProperty("valueInteger", toInteger(aacReportMap.get("extension_total_clients")));

        extensionArray.add(asgReportingMonthObject);
        extensionArray.add(asgTotalOperatingDaysObject);
        extensionArray.add(asgTotalClients);
        resource.add("extension", extensionArray);

        resource.addProperty("status", aacReportMap.get("status"));

        // Type section.
        JsonObject type = new JsonObject();
        JsonArray codingArray = new JsonArray();
        JsonObject codingObject = new JsonObject();
        codingObject.addProperty("system", "http://ihis.sg/CodeSystem/asg-document-type");
        codingObject.addProperty("code", "aac-report");
        codingObject.addProperty("display", "AAC Report");
        codingArray.add(codingObject);
        type.add("coding", codingArray);
        resource.add("type", type);

        resource.addProperty("date", aacReportMap.get("date"));

        // Author section.
        JsonArray authorArray = new JsonArray();
        JsonObject authorObject = new JsonObject();
        JsonObject identifierObject = new JsonObject();
        identifierObject.addProperty("system", "http://ihis.sg/identifier/aac-center-id");
        identifierObject.addProperty("value", aacReportMap.get("author_value"));
        authorObject.add("identifier", identifierObject);
        authorObject.addProperty("display", aacReportMap.get("author_display"));
        authorArray.add(authorObject);
        resource.add("author", authorArray);

        resource.addProperty("title", "AAC Report Submission");

        // Section.
        JsonArray sectionArray = new JsonArray();
        JsonObject outerCodeObject = new JsonObject();
        JsonObject innerCodeObject = new JsonObject();
        codingArray = new JsonArray();
        codingObject = new JsonObject();
        codingObject.addProperty("system", "http://ihis.sg/CodeSystem/asg-aac-report-section");
        codingObject.addProperty("code", "section-aac-manpower");
        codingArray.add(codingObject);
        innerCodeObject.add("coding", codingArray);
        outerCodeObject.add("code", innerCodeObject);

        JsonArray entryArray = new JsonArray();
        for (String practitionerId : resolvedPractitionerIds) {
            JsonObject referenceObject = new JsonObject();
            referenceObject.addProperty("reference", "#" + practitionerId);
            entryArray.add(referenceObject);
        }

        outerCodeObject.add("entry", entryArray);
        sectionArray.add(outerCodeObject);
        resource.add("section", sectionArray);

        return resource;
    }

    public static JsonObject generateResidentReportObject(HashMap<String, String> residentReportMap,
                                                          HashMap<String, Patient> patients,
                                                          HashMap<String, Encounter> encounters,
                                                          HashMap<String, Questionnaire> questionnaires) {
        JsonObject resource = new JsonObject();
        resource.addProperty("resourceType", "Composition");
        resource.addProperty("id", residentReportMap.get("composition_id"));
        //printHashMap(residentReportMap);

        JsonObject meta = new JsonObject();
        meta.addProperty("versionId", stripDecimal(residentReportMap.get("version_id")));
        meta.addProperty("lastUpdated", residentReportMap.get("last_updated"));

        JsonArray profileArray = new JsonArray();
        profileArray.add("http://ihis.sg/StructureDefinition/Composition-put-asg-resident-report");
        meta.add("profile", profileArray);

        JsonArray tagArray = new JsonArray();
        JsonObject tagObject = new JsonObject();
        tagObject.addProperty("system", "http://ihis.sg/coding/correlationid");
        tagObject.addProperty("code", residentReportMap.get("meta_code"));
        tagArray.add(tagObject);

        meta.add("tag", tagArray);
        resource.add("meta", meta);

        // Contained section.
        JsonArray containedArray = new JsonArray();
        String patientId = residentReportMap.get("patient_reference"); // Each resident report should only have 1 patient.
        Patient patient = patients.get(patientId);
        if (patient != null) {
            JsonObject patientObject = patient.generatePatientObject();
            containedArray.add(patientObject);
        }

        List<String> encounterKeys = splitRefs(residentReportMap.get("encounter_references"));
        for (String encounterId : encounterKeys) {
            Encounter encounter = encounters.get(encounterId);
            if (encounter == null) {
                continue;
            }
            JsonObject encounterObject = encounter.generateEncounterObject();
            containedArray.add(encounterObject);
        }

        String questionnaireId = residentReportMap.get("questionnaire_reference"); // Assume the resident in each resident report only has 1 questionnaire.
        Questionnaire questionnaire = questionnaires.get(questionnaireId);
        if (questionnaire != null) {
            JsonObject questionnaireObject = questionnaire.generateQuestionnaireObject();
            containedArray.add(questionnaireObject);
        }
        resource.add("contained", containedArray);

        //Extension section.
        JsonArray extensionArray = new JsonArray();
        JsonObject asgReportingMonthObject = new JsonObject();
        asgReportingMonthObject.addProperty("url", "http://ihis.sg/extension/asg-reporting-month");
        asgReportingMonthObject.addProperty("valueDate", residentReportMap.get("extension_reporting_month"));
        extensionArray.add(asgReportingMonthObject);
        resource.add("extension", extensionArray);

        resource.addProperty("status", residentReportMap.get("status"));

        // Type section.
        JsonObject type = new JsonObject();
        JsonArray codingArray = new JsonArray();
        JsonObject codingObject = new JsonObject();
        codingObject.addProperty("system", "http://ihis.sg/CodeSystem/asg-document-type");
        codingObject.addProperty("code", "resident-report");
        codingObject.addProperty("display", "Resident Report");
        codingArray.add(codingObject);
        type.add("coding", codingArray);
        resource.add("type", type);

        resource.addProperty("date", residentReportMap.get("date"));

        // Author section.
        JsonArray authorArray = new JsonArray();
        JsonObject authorObject = new JsonObject();
        JsonObject identifierObject = new JsonObject();
        identifierObject.addProperty("system", "http://ihis.sg/identifier/aac-center-id");
        identifierObject.addProperty("value", residentReportMap.get("author_value"));
        authorObject.add("identifier", identifierObject);
        authorObject.addProperty("display", residentReportMap.get("author_display"));
        authorArray.add(authorObject);
        resource.add("author", authorArray);

        resource.addProperty("title", "Resident Report Submission");

        // Section.
        JsonArray sectionArray = new JsonArray();
        JsonObject outerCodeObject = new JsonObject();
        JsonObject innerCodeObject = new JsonObject();
        codingArray = new JsonArray();
        codingObject = new JsonObject();
        codingObject.addProperty("system", "http://ihis.sg/CodeSystem/asg-resident-report-section");
        codingObject.addProperty("code", "section-resident-profile");
        codingArray.add(codingObject);
        innerCodeObject.add("coding", codingArray);
        outerCodeObject.add("code", innerCodeObject);

        JsonArray sectionExtensionArray = new JsonArray();

        JsonObject asgResidentVolunteerStatusObject = new JsonObject();
        asgResidentVolunteerStatusObject.addProperty("url", "http://ihis.sg/extension/asg-resident-volunteer-status");
        asgResidentVolunteerStatusObject.addProperty("valueBoolean", Boolean.parseBoolean(residentReportMap.get("resident_volunteer_status")));

        JsonObject asgCstDateObject = new JsonObject();
        asgCstDateObject.addProperty("url", "http://ihis.sg/extension/asg-cst-date");
        asgCstDateObject.addProperty("valueDate", convertToDate(residentReportMap.get("cst_date")));

        JsonObject asgCfsObject = new JsonObject();
        asgCfsObject.addProperty("url", "http://ihis.sg/extension/asg-clinical-frailty-score");
        asgCfsObject.addProperty("valueString", residentReportMap.get("cfs"));

        JsonObject asgSocialRiskFactorScoreObject = new JsonObject();
        asgSocialRiskFactorScoreObject.addProperty("url", "http://ihis.sg/extension/asg-social-risk-factor-score");
        asgSocialRiskFactorScoreObject.addProperty("valueString", stripDecimal(residentReportMap.get("social_risk_factor_score")));

        JsonObject asgAapRecommendation = new JsonObject();
        asgAapRecommendation.addProperty("url", "http://ihis.sg/extension/asg-aap-recommendation");
        asgAapRecommendation.addProperty("valueString", residentReportMap.get("aap_recommendation"));

        JsonObject asgSocialSupportRecommendationObject = new JsonObject();
        asgSocialSupportRecommendationObject.addProperty("url", "http://ihis.sg/extension/asg-social-support-recommendation");
        asgSocialSupportRecommendationObject.addProperty("valueString", residentReportMap.get("social_support_recommendation"));

        JsonObject asgAacOptOutStatusObject = new JsonObject();
        asgAacOptOutStatusObject.addProperty("url", "http://ihis.sg/extension/asg-aac-opt-out-status");
        asgAacOptOutStatusObject.addProperty("valueBoolean", Boolean.parseBoolean(residentReportMap.get("aac_opt_out_status")));

        JsonObject asgAapOptOutStatusObject = new JsonObject();
        asgAapOptOutStatusObject.addProperty("url", "http://ihis.sg/extension/asg-aap-opt-out-status");
        asgAapOptOutStatusObject.addProperty("valueBoolean", Boolean.parseBoolean(residentReportMap.get("aap_opt_out_status")));

        JsonObject asgScreeningDeclarationDateObject = new JsonObject();
        asgScreeningDeclarationDateObject.addProperty("url", "http://ihis.sg/extension/asg-screening-declaration-date");
        asgScreeningDeclarationDateObject.addProperty("valueDate", convertToDate(residentReportMap.get("screening_declaration_date")));

        JsonObject asgBefriendingOptOutStatusObject = new JsonObject();
        asgBefriendingOptOutStatusObject.addProperty("url", "http://ihis.sg/extension/asg-befriending-programme-opt-out-status");
        asgBefriendingOptOutStatusObject.addProperty("valueBoolean", Boolean.parseBoolean(residentReportMap.get("befriending_opt_out_status")));

        JsonObject asgBuddyingOptOutStatusObject = new JsonObject();
        asgBuddyingOptOutStatusObject.addProperty("url", "http://ihis.sg/extension/asg-buddying-programme-opt-out-status");
        asgBuddyingOptOutStatusObject.addProperty("valueBoolean", Boolean.parseBoolean(residentReportMap.get("buddying_opt_out_status")));

        JsonObject asgBefriendingPeriodObject = new JsonObject();
        asgBefriendingPeriodObject.addProperty("url", "http://ihis.sg/extension/asg-resident-befriending-programme-period");
        JsonObject befriendingValuePeriodObject = new JsonObject();
        befriendingValuePeriodObject.addProperty("start", convertToDate(residentReportMap.get("resident_befriending_programme_period_start")));
        befriendingValuePeriodObject.addProperty("end", convertToDate(residentReportMap.get("resident_befriending_programme_period_end")));
        asgBefriendingPeriodObject.add("valuePeriod", befriendingValuePeriodObject);

        JsonObject asgBuddyingPeriodObject = new JsonObject();
        asgBuddyingPeriodObject.addProperty("url", "http://ihis.sg/extension/asg-resident-buddying-programme-period");
        JsonObject buddyingValuePeriodObject = new JsonObject();
        buddyingValuePeriodObject.addProperty("start", convertToDate(residentReportMap.get("resident_buddying_programme_period_start")));
        buddyingValuePeriodObject.addProperty("end", convertToDate(residentReportMap.get("resident_buddying_programme_period_end")));
        asgBuddyingPeriodObject.add("valuePeriod", buddyingValuePeriodObject);

        JsonObject asgReferralRaisedDateObject = new JsonObject();
        asgReferralRaisedDateObject.addProperty("url", "http://ihis.sg/extension/asg-irms-referral-raised-date");
        asgReferralRaisedDateObject.addProperty("valueDate", convertToDate(residentReportMap.get("irms_referral_raised_date")));

        JsonObject asgReferralAcceptedDateObject = new JsonObject();
        asgReferralAcceptedDateObject.addProperty("url", "http://ihis.sg/extension/asg-irms-referral-accepted-date");
        asgReferralAcceptedDateObject.addProperty("valueDate", convertToDate(residentReportMap.get("irms_referral_accepted_date")));

        JsonObject asgReferralRaisedByObject = new JsonObject();
        asgReferralRaisedByObject.addProperty("url", "http://ihis.sg/extension/asg-referral-raised-by");
        asgReferralRaisedByObject.addProperty("valueString", residentReportMap.get("asg_referral_raised_by"));

        JsonObject asgReferralAcceptedByObject = new JsonObject();
        asgReferralAcceptedByObject.addProperty("url", "http://ihis.sg/extension/asg-referral-accepted-by");
        asgReferralAcceptedByObject.addProperty("valueString", residentReportMap.get("asg_referral_accepted_by"));

        sectionExtensionArray.add(asgResidentVolunteerStatusObject);
        sectionExtensionArray.add(asgCstDateObject);
        sectionExtensionArray.add(asgCfsObject);
        sectionExtensionArray.add(asgSocialRiskFactorScoreObject);
        sectionExtensionArray.add(asgAapRecommendation);
        sectionExtensionArray.add(asgSocialSupportRecommendationObject);
        sectionExtensionArray.add(asgAacOptOutStatusObject);
        sectionExtensionArray.add(asgAapOptOutStatusObject);
        sectionExtensionArray.add(asgScreeningDeclarationDateObject);
        sectionExtensionArray.add(asgBefriendingOptOutStatusObject);
        sectionExtensionArray.add(asgBuddyingOptOutStatusObject);
        sectionExtensionArray.add(asgBefriendingPeriodObject);
        sectionExtensionArray.add(asgBuddyingPeriodObject);
        sectionExtensionArray.add(asgReferralRaisedDateObject);
        sectionExtensionArray.add(asgReferralAcceptedDateObject);
        sectionExtensionArray.add(asgReferralRaisedByObject);
        sectionExtensionArray.add(asgReferralAcceptedByObject);
        outerCodeObject.add("extension", sectionExtensionArray);

        // Patient reference.
        if (patient != null) {
            JsonArray patientEntryArray = new JsonArray();
            JsonObject patientEntryObject = new JsonObject();
            patientEntryObject.addProperty("reference", "#" + residentReportMap.get("patient_reference"));
            patientEntryArray.add(patientEntryObject);
            outerCodeObject.add("entry", patientEntryArray);
            sectionArray.add(outerCodeObject);
        }

        // Encounter references.
        outerCodeObject = new JsonObject();
        innerCodeObject = new JsonObject();
        codingArray = new JsonArray();
        codingObject = new JsonObject();
        codingObject.addProperty("system", "http://ihis.sg/CodeSystem/asg-resident-report-section");
        codingObject.addProperty("code", "section-contact-log");
        codingArray.add(codingObject);
        innerCodeObject.add("coding", codingArray);
        outerCodeObject.add("code", innerCodeObject);

        JsonArray encounterEntryArray = new JsonArray();
        for (String encounterId : encounterKeys) {
            JsonObject referenceObject = new JsonObject();
            referenceObject.addProperty("reference", "#" + encounterId);
            encounterEntryArray.add(referenceObject);
        }
        outerCodeObject.add("entry", encounterEntryArray);
        sectionArray.add(outerCodeObject);

        //Questionnaire reference.
        outerCodeObject = new JsonObject();
        innerCodeObject = new JsonObject();
        codingArray = new JsonArray();
        codingObject = new JsonObject();
        codingObject.addProperty("system", "http://ihis.sg/CodeSystem/asg-resident-report-section");
        codingObject.addProperty("code", "section-resident-satisfaction-statistics");
        codingArray.add(codingObject);
        innerCodeObject.add("coding", codingArray);
        outerCodeObject.add("code", innerCodeObject);
        JsonArray questionnaireEntryArray = new JsonArray();
        JsonObject questionnaireEntryObject = new JsonObject();
        questionnaireEntryObject.addProperty("reference", "#" + residentReportMap.get("questionnaire_reference"));
        questionnaireEntryArray.add(questionnaireEntryObject);
        outerCodeObject.add("entry", questionnaireEntryArray);
        sectionArray.add(outerCodeObject);

        resource.add("section", sectionArray);

        return resource;
    }

    public static JsonObject generateVolunteerAttendanceReportObject(HashMap<String, String> volunteerAttendanceReportMap,
                                                                     HashMap<String, Practitioner> practitioners) {
        JsonObject resource = new JsonObject();
        resource.addProperty("resourceType", "Composition");
        resource.addProperty("id", volunteerAttendanceReportMap.get("composition_id"));

        JsonObject meta = new JsonObject();
        meta.addProperty("versionId", stripDecimal(volunteerAttendanceReportMap.get("version_id")));
        meta.addProperty("lastUpdated", volunteerAttendanceReportMap.get("last_updated"));

        JsonArray profileArray = new JsonArray();
        profileArray.add("http://ihis.sg/StructureDefinition/Composition-put-asg-volunteer-attendance-report");
        meta.add("profile", profileArray);

        JsonArray tagArray = new JsonArray();
        JsonObject tagObject = new JsonObject();
        tagObject.addProperty("system", "http://ihis.sg/coding/correlationid");
        tagObject.addProperty("code", volunteerAttendanceReportMap.get("meta_code"));
        tagArray.add(tagObject);

        meta.add("tag", tagArray);
        resource.add("meta", meta);

        // Contained section.
        JsonArray containedArray = new JsonArray();
        List<String> practitionerKeys = splitRefs(volunteerAttendanceReportMap.get("practitioner_references"));

        int practitionerCount = 0;
        List<String> resolvedPractitionerIds = new java.util.ArrayList<>();
        for (String practitionerId : practitionerKeys) {
            Practitioner practitioner = practitioners.get(practitionerId);
            if (practitioner == null) {
                continue;
            }
            JsonObject practitionerObject = practitioner.generatePractitionerVolunteerDetailsObject();

            // Get extension member and append the non-static per volunteer attendance report fields.
            JsonArray extensionArray = practitionerObject.getAsJsonArray("extension");
            JsonObject asgActivityVolunteeredObject = new JsonObject();
            asgActivityVolunteeredObject.addProperty("url", "http://ihis.sg/extension/asg-activity-volunteered");

            JsonArray asgActivityVolunteeredArray = new JsonArray();
            JsonObject asgActivityVolunteeredNameObject = new JsonObject();
            asgActivityVolunteeredNameObject.addProperty("url", "http://ihis.sg/extension/asg-volunteered-activity-name");
            String volunteeredActivityName = volunteerAttendanceReportMap.get("volunteered_activity_name_practitioner" + (practitionerCount + 1));
            asgActivityVolunteeredNameObject.addProperty("valueString", volunteeredActivityName);
            asgActivityVolunteeredArray.add(asgActivityVolunteeredNameObject);

            JsonObject asgActivityVolunteeredDateObject = new JsonObject();
            asgActivityVolunteeredDateObject.addProperty("url", "http://ihis.sg/extension/asg-volunteered-activity-date");
            String volunteeredActivityDate = convertToDate(volunteerAttendanceReportMap.get("volunteered_activity_date_practitioner" + (practitionerCount + 1)));
            asgActivityVolunteeredDateObject.addProperty("valueDate", volunteeredActivityDate);
            asgActivityVolunteeredArray.add(asgActivityVolunteeredDateObject);

            asgActivityVolunteeredObject.add("extension", asgActivityVolunteeredArray);
            extensionArray.add(asgActivityVolunteeredObject);

            containedArray.add(practitionerObject);

            if (volunteeredActivityDate != null) {
                practitioner.addVolunteeredActivity(volunteeredActivityName, LocalDate.parse(volunteeredActivityDate));
            }
            resolvedPractitionerIds.add(practitionerId);

            practitionerCount++;
        }
        resource.add("contained", containedArray);

        // Extension section.
        JsonArray extensionArray = new JsonArray();
        JsonObject asgReportingMonthObject = new JsonObject();
        asgReportingMonthObject.addProperty("url", "http://ihis.sg/extension/asg-reporting-month");
        asgReportingMonthObject.addProperty("valueDate", volunteerAttendanceReportMap.get("extension_reporting_month"));
        extensionArray.add(asgReportingMonthObject);
        resource.add("extension", extensionArray);

        resource.addProperty("status", volunteerAttendanceReportMap.get("status"));

        // Type section.
        JsonObject type = new JsonObject();
        JsonArray codingArray = new JsonArray();
        JsonObject codingObject = new JsonObject();
        codingObject.addProperty("system", "http://ihis.sg/CodeSystem/asg-document-type");
        codingObject.addProperty("code", "volunteer-attendance-report");
        codingObject.addProperty("display", "Volunteer Attendance Report");
        codingArray.add(codingObject);
        type.add("coding", codingArray);
        resource.add("type", type);

        resource.addProperty("date", volunteerAttendanceReportMap.get("date"));

        // Author section.
        JsonArray authorArray = new JsonArray();
        JsonObject authorObject = new JsonObject();
        JsonObject identifierObject = new JsonObject();
        identifierObject.addProperty("system", "http://ihis.sg/identifier/aac-center-id");
        identifierObject.addProperty("value", volunteerAttendanceReportMap.get("author_value"));
        authorObject.add("identifier", identifierObject);
        authorObject.addProperty("display", volunteerAttendanceReportMap.get("author_display"));
        authorArray.add(authorObject);
        resource.add("author", authorArray);

        resource.addProperty("title", "Volunteer Attendance Report Submission");

        // Section.
        JsonArray sectionArray = new JsonArray();
        JsonObject outerCodeObject = new JsonObject();
        JsonObject innerCodeObject = new JsonObject();
        codingArray = new JsonArray();
        codingObject = new JsonObject();
        codingObject.addProperty("system", "http://ihis.sg/CodeSystem/asg-volunteer-attendance-report-section");
        codingObject.addProperty("code", "section-aac-volunteer-attendance");
        codingArray.add(codingObject);
        innerCodeObject.add("coding", codingArray);
        outerCodeObject.add("code", innerCodeObject);

        JsonArray entryArray = new JsonArray();
        for (String practitionerId : resolvedPractitionerIds) {
            JsonObject referenceObject = new JsonObject();
            referenceObject.addProperty("reference", "#" + practitionerId);
            entryArray.add(referenceObject);
        }

        outerCodeObject.add("entry", entryArray);
        sectionArray.add(outerCodeObject);
        resource.add("section", sectionArray);

        return resource;
    }

    public static JsonObject generateEventReportObject(HashMap<String, String> eventReportMap,
                                                       HashMap<String, HashMap<String, String>> eventSessionsNricMap,
                                                       HashMap<String, Patient> patients,
                                                       HashMap<String, Event> events) {
        JsonObject resource = new JsonObject();
        resource.addProperty("resourceType", "Composition");
        resource.addProperty("id", eventReportMap.get("composition_id"));

        JsonObject meta = new JsonObject();
        meta.addProperty("versionId", stripDecimal(eventReportMap.get("version_id")));
        meta.addProperty("lastUpdated", eventReportMap.get("last_updated"));

        JsonArray profileArray = new JsonArray();
        profileArray.add("http://ihis.sg/StructureDefinition/Composition-put-asg-event-report");
        meta.add("profile", profileArray);

        JsonArray tagArray = new JsonArray();
        JsonObject tagObject = new JsonObject();
        tagObject.addProperty("system", "http://ihis.sg/coding/correlationid");
        tagObject.addProperty("code", eventReportMap.get("meta_code"));
        tagArray.add(tagObject);

        meta.add("tag", tagArray);
        resource.add("meta", meta);

        // Contained section.
        JsonArray containedArray = new JsonArray();
        List<String> patientKeys = splitRefs(eventReportMap.get("patient_references"));

        int patientCount = 0;

        // For event report, patientId is NRIC and we are using patients map with NRIC key.
        for (String patientId : patientKeys) {
            Patient patient = patients.get(patientId);
            HashMap<String, String> registrationDetails = eventSessionsNricMap.get(patientId);
            if (patient == null || registrationDetails == null) {
                continue;
            }

            Integer registrationIndex = toInteger(registrationDetails.get("number_of_attended_indicator"));
            if (registrationIndex == null) {
                continue;
            }

            for (int i = 0; i < registrationIndex; i++) {
                JsonObject patientObject = patient.generatePatientEventDetailsObject(eventSessionsNricMap, i + 1);

                // Get extension member and append the non-static per volunteer attendance report fields.
                JsonArray extensionArray = patientObject.getAsJsonArray("extension");
                JsonObject asgIsAttendedSessionObject = new JsonObject();
                asgIsAttendedSessionObject.addProperty("url", "http://ihis.sg/extension/asg-is-attended-session");
                asgIsAttendedSessionObject.addProperty("valueBoolean", Boolean.parseBoolean(registrationDetails.get("registration_value" + (i + 1))));
                extensionArray.add(asgIsAttendedSessionObject);

                containedArray.add(patientObject);
            }

            patientCount++;
        }


        resource.add("contained", containedArray);

        // Extension section.
        JsonArray extensionArray = new JsonArray();

        JsonObject asgReportingMonthObject = new JsonObject();
        asgReportingMonthObject.addProperty("url", "http://ihis.sg/extension/asg-reporting-month");
        asgReportingMonthObject.addProperty("valueDate", eventReportMap.get("extension_reporting_month"));

        JsonObject asgEventIdObject = new JsonObject();
        asgEventIdObject.addProperty("url", "http://ihis.sg/extension/asg-event-id");
        asgEventIdObject.addProperty("valueString", eventReportMap.get("event_id"));

        JsonObject asgEventNameObject = new JsonObject();
        asgEventNameObject.addProperty("url", "http://ihis.sg/extension/asg-event-name");
        asgEventNameObject.addProperty("valueString", eventReportMap.get("event_name"));

        JsonObject asgEventTypeObject = new JsonObject();
        asgEventTypeObject.addProperty("url", "http://ihis.sg/extension/asg-event-type");
        asgEventTypeObject.addProperty("valueString", eventReportMap.get("event_type"));

        JsonObject asgEventDomainObject = new JsonObject();
        asgEventDomainObject.addProperty("url", "http://ihis.sg/extension/asg-event-domain");
        asgEventDomainObject.addProperty("valueString", eventReportMap.get("event_domain"));

        JsonObject asgEventTargetAttendeesObject = new JsonObject();
        asgEventTargetAttendeesObject.addProperty("url", "http://ihis.sg/extension/asg-event-target-attendees");
        asgEventTargetAttendeesObject.addProperty("valueString", eventReportMap.get("event_target_attendees"));

        JsonObject asgEventCategory = new JsonObject();
        asgEventCategory.addProperty("url", "http://ihis.sg/extension/asg-event-category");
        asgEventCategory.addProperty("valueString", eventReportMap.get("event_category"));

        JsonObject asgAapProvider = new JsonObject();
        asgAapProvider.addProperty("url", "http://ihis.sg/extension/asg-aap-provider");
        asgAapProvider.addProperty("valueString", eventReportMap.get("aap_provider"));

        JsonObject asgMinimumRequiredSessionsObject = new JsonObject();
        asgMinimumRequiredSessionsObject.addProperty("url", "http://ihis.sg/extension/asg-minimum-required-sessions");
        asgMinimumRequiredSessionsObject.addProperty("valueInteger", toInteger(eventReportMap.get("minimum_required_sessions")));

        JsonObject asgIsGuiObject = new JsonObject();
        asgIsGuiObject.addProperty("url", "http://ihis.sg/extension/asg-event-is-ground-up-initiative");
        asgIsGuiObject.addProperty("valueBoolean", Boolean.parseBoolean(eventReportMap.get("event_is_gui")));

        JsonObject asgGuiPartnerObject = new JsonObject();
        asgGuiPartnerObject.addProperty("url", "http://ihis.sg/extension/asg-ground-up-initiative-partner");
        asgGuiPartnerObject.addProperty("valueString", eventReportMap.get("gui_partner"));

        extensionArray.add(asgReportingMonthObject);
        extensionArray.add(asgEventIdObject);
        extensionArray.add(asgEventNameObject);
        extensionArray.add(asgEventTypeObject);
        extensionArray.add(asgEventDomainObject);
        extensionArray.add(asgEventTargetAttendeesObject);
        extensionArray.add(asgEventCategory);
        extensionArray.add(asgAapProvider);
        extensionArray.add(asgMinimumRequiredSessionsObject);
        extensionArray.add(asgIsGuiObject);
        extensionArray.add(asgGuiPartnerObject);
        resource.add("extension", extensionArray);

        resource.addProperty("status", eventReportMap.get("status"));

        // Type section.
        JsonObject type = new JsonObject();
        JsonArray codingArray = new JsonArray();
        JsonObject codingObject = new JsonObject();
        codingObject.addProperty("system", "http://ihis.sg/CodeSystem/asg-document-type");
        codingObject.addProperty("code", "event-report");
        codingObject.addProperty("display", "Event Report");
        codingArray.add(codingObject);
        type.add("coding", codingArray);
        resource.add("type", type);

        resource.addProperty("date", eventReportMap.get("date"));

        // Author section.
        JsonArray authorArray = new JsonArray();
        JsonObject authorObject = new JsonObject();
        JsonObject identifierObject = new JsonObject();
        identifierObject.addProperty("system", "http://ihis.sg/identifier/aac-center-id");
        identifierObject.addProperty("value", eventReportMap.get("author_value"));
        authorObject.add("identifier", identifierObject);
        authorObject.addProperty("display", eventReportMap.get("author_display"));
        authorArray.add(authorObject);
        resource.add("author", authorArray);

        resource.addProperty("title", "Event Report Submission");

        // Section.
        String eventKey = eventReportMap.get("composition_id");
        Event event = eventKey == null ? null : events.get(eventKey);
        JsonArray sectionArray = event != null ? event.generateEventSessionsSection(eventSessionsNricMap) : new JsonArray();
        resource.add("section", sectionArray);

        return resource;
    }

    public static JsonObject generateOrganizationReportObject(HashMap<String, String> organizationReportMap) {
        JsonObject resource = new JsonObject();
        resource.addProperty("resourceType", "Organization");
        resource.addProperty("id", organizationReportMap.get("id"));

        JsonObject meta = new JsonObject();
        meta.addProperty("versionId", stripDecimal(organizationReportMap.get("version_id")));
        meta.addProperty("lastUpdated", organizationReportMap.get("last_updated"));
        meta.addProperty("source", "http://ihis.sg/programme-owner/age-well");

        JsonArray profileArray = new JsonArray();
        profileArray.add("http://ihis.sg/StructureDefinition/Organization-put-asg-aac");
        meta.add("profile", profileArray);

        JsonArray tagArray = new JsonArray();
        JsonObject tagObject = new JsonObject();
        tagObject.addProperty("system", "http://ihis.sg/coding/correlationid");
        tagObject.addProperty("code", organizationReportMap.get("meta_code"));
        tagArray.add(tagObject);

        meta.add("tag", tagArray);
        resource.add("meta", meta);

        // Extension section.
        JsonArray extensionArray = new JsonArray();
        JsonObject asgOrganizationPeriodObject = new JsonObject();
        asgOrganizationPeriodObject.addProperty("url", "http://ihis.sg/extension/organization-period");
        JsonObject valuePeriodObject = new JsonObject();
        valuePeriodObject.addProperty("start", convertToDate(organizationReportMap.get("start")));

        if (organizationReportMap.get("end") != null) {
            valuePeriodObject.addProperty("end", convertToDate(organizationReportMap.get("end")));
        }

        asgOrganizationPeriodObject.add("valuePeriod", valuePeriodObject);
        extensionArray.add(asgOrganizationPeriodObject);
        resource.add("extension", extensionArray);

        // Identifier section.
        JsonArray identifierArray = new JsonArray();
        JsonObject aacCenterIdObject = new JsonObject();
        aacCenterIdObject.addProperty("system", "http://ihis.sg/identifier/aac-center-id");
        aacCenterIdObject.addProperty("value", organizationReportMap.get("aac_center_id"));
        JsonObject uenObject = new JsonObject();
        uenObject.addProperty("system", "http://ihis.sg/identifier/uen");
        uenObject.addProperty("value", organizationReportMap.get("uen"));
        identifierArray.add(aacCenterIdObject);
        identifierArray.add(uenObject);
        resource.add("identifier", identifierArray);

        resource.addProperty("active", Boolean.parseBoolean(organizationReportMap.get("active")));

        // Type section.
        JsonArray typeArray = new JsonArray();
        JsonObject outerCodingObject = new JsonObject();
        JsonArray codingArray = new JsonArray();
        JsonObject innerCodingObject = new JsonObject();
        innerCodingObject.addProperty("system", "http://ihis.sg/CodeSystem/organization-type");
        innerCodingObject.addProperty("code", organizationReportMap.get("organization_type_code"));
        innerCodingObject.addProperty("display", organizationReportMap.get("organization_type_display"));
        codingArray.add(innerCodingObject);
        outerCodingObject.add("coding", codingArray);
        typeArray.add(outerCodingObject);
        resource.add("type", typeArray);

        resource.addProperty("name", organizationReportMap.get("name"));

        return resource;
    }

    public static JsonObject generateLocationReportObject(HashMap<String, String> locationReportMap) {
        JsonObject resource = new JsonObject();
        resource.addProperty("resourceType", "Location");
        resource.addProperty("id", locationReportMap.get("id"));

        JsonObject meta = new JsonObject();
        meta.addProperty("versionId", stripDecimal(locationReportMap.get("version_id")));
        meta.addProperty("lastUpdated", locationReportMap.get("last_updated"));
        meta.addProperty("source", "http://ihis.sg/programme-owner/age-well");

        JsonArray profileArray = new JsonArray();
        profileArray.add("http://ihis.sg/StructureDefinition/Location-put-asg-aac-service-boundary");
        meta.add("profile", profileArray);

        JsonArray tagArray = new JsonArray();
        JsonObject tagObject = new JsonObject();
        tagObject.addProperty("system", "http://ihis.sg/coding/correlationid");
        tagObject.addProperty("code", locationReportMap.get("meta_code"));
        tagArray.add(tagObject);

        meta.add("tag", tagArray);
        resource.add("meta", meta);

        // Extension section.
        JsonArray extensionArray = new JsonArray();
        JsonObject asgEffectivePeriodObject = new JsonObject();
        asgEffectivePeriodObject.addProperty("url", "http://ihis.sg/extension/effective-period");
        JsonObject valuePeriodObject = new JsonObject();
        valuePeriodObject.addProperty("start", convertToDate(locationReportMap.get("start")));

        if (locationReportMap.get("end") != null) {
            valuePeriodObject.addProperty("end", convertToDate(locationReportMap.get("end")));
        }

        asgEffectivePeriodObject.add("valuePeriod", valuePeriodObject);
        extensionArray.add(asgEffectivePeriodObject);
        resource.add("extension", extensionArray);

        JsonObject addressObject = new JsonObject();
        addressObject.addProperty("postalCode", stripDecimal(locationReportMap.get("postal_code")));
        resource.add("address", addressObject);

        JsonObject managingOrganizationObject = new JsonObject();
        managingOrganizationObject.addProperty("reference", locationReportMap.get("reference"));
        resource.add("managingOrganization", managingOrganizationObject);

        return resource;
    }
}
