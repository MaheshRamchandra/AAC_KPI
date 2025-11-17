package com.aac.kpi.model;

import javafx.beans.property.*;

public class Patient {
    private final StringProperty patientId = new SimpleStringProperty();
    private final StringProperty patientIdentifierValue = new SimpleStringProperty();
    private final StringProperty patientBirthdate = new SimpleStringProperty();
    private final StringProperty patientPostalCode = new SimpleStringProperty();
    private final StringProperty attendedEventReferences = new SimpleStringProperty("");
    private final StringProperty workingRemarks = new SimpleStringProperty("");
    private final IntegerProperty group = new SimpleIntegerProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty aac = new SimpleStringProperty();
    // KPI-related fields
    private final IntegerProperty cfs = new SimpleIntegerProperty(3);
    private final IntegerProperty socialRiskFactor = new SimpleIntegerProperty(0);
    private final StringProperty buddyingProgramStartDate = new SimpleStringProperty("");
    private final StringProperty buddyingProgramEndDate = new SimpleStringProperty("");
    private final StringProperty befriendingProgramStartDate = new SimpleStringProperty("");
    private final StringProperty befriendingProgramEndDate = new SimpleStringProperty("");
    private final StringProperty kpiType = new SimpleStringProperty("");
    private final StringProperty kpiGroup = new SimpleStringProperty("");

    public String getPatientId() { return patientId.get(); }
    public void setPatientId(String value) { patientId.set(value); }
    public StringProperty patientIdProperty() { return patientId; }

    public String getPatientIdentifierValue() { return patientIdentifierValue.get(); }
    public void setPatientIdentifierValue(String value) { patientIdentifierValue.set(value); }
    public StringProperty patientIdentifierValueProperty() { return patientIdentifierValue; }

    public String getPatientBirthdate() { return patientBirthdate.get(); }
    public void setPatientBirthdate(String value) { patientBirthdate.set(value); }
    public StringProperty patientBirthdateProperty() { return patientBirthdate; }

    public String getPatientPostalCode() { return patientPostalCode.get(); }
    public void setPatientPostalCode(String value) { patientPostalCode.set(value); }
    public StringProperty patientPostalCodeProperty() { return patientPostalCode; }

    public String getAttendedEventReferences() { return attendedEventReferences.get(); }
    public void setAttendedEventReferences(String value) { attendedEventReferences.set(value); }
    public StringProperty attendedEventReferencesProperty() { return attendedEventReferences; }

    public String getWorkingRemarks() { return workingRemarks.get(); }
    public void setWorkingRemarks(String value) { workingRemarks.set(value); }
    public StringProperty workingRemarksProperty() { return workingRemarks; }

    public int getGroup() { return group.get(); }
    public void setGroup(int value) { group.set(value); }
    public IntegerProperty groupProperty() { return group; }

    public String getType() { return type.get(); }
    public void setType(String value) { type.set(value); }
    public StringProperty typeProperty() { return type; }

    public String getAac() { return aac.get(); }
    public void setAac(String value) { aac.set(value); }
    public StringProperty aacProperty() { return aac; }

    public int getCfs() { return cfs.get(); }
    public void setCfs(int v) { cfs.set(v); }
    public IntegerProperty cfsProperty() { return cfs; }

    public int getSocialRiskFactor() { return socialRiskFactor.get(); }
    public void setSocialRiskFactor(int v) { socialRiskFactor.set(v); }
    public IntegerProperty socialRiskFactorProperty() { return socialRiskFactor; }

    public String getBuddyingProgramStartDate() { return buddyingProgramStartDate.get(); }
    public void setBuddyingProgramStartDate(String v) { buddyingProgramStartDate.set(v); }
    public StringProperty buddyingProgramStartDateProperty() { return buddyingProgramStartDate; }

    public String getBuddyingProgramEndDate() { return buddyingProgramEndDate.get(); }
    public void setBuddyingProgramEndDate(String v) { buddyingProgramEndDate.set(v); }
    public StringProperty buddyingProgramEndDateProperty() { return buddyingProgramEndDate; }

    public String getBefriendingProgramStartDate() { return befriendingProgramStartDate.get(); }
    public void setBefriendingProgramStartDate(String v) { befriendingProgramStartDate.set(v); }
    public StringProperty befriendingProgramStartDateProperty() { return befriendingProgramStartDate; }

    public String getBefriendingProgramEndDate() { return befriendingProgramEndDate.get(); }
    public void setBefriendingProgramEndDate(String v) { befriendingProgramEndDate.set(v); }
    public StringProperty befriendingProgramEndDateProperty() { return befriendingProgramEndDate; }

    public String getKpiType() { return kpiType.get(); }
    public void setKpiType(String v) { kpiType.set(v); }
    public StringProperty kpiTypeProperty() { return kpiType; }

    public String getKpiGroup() { return kpiGroup.get(); }
    public void setKpiGroup(String v) { kpiGroup.set(v); }
    public StringProperty kpiGroupProperty() { return kpiGroup; }
}
