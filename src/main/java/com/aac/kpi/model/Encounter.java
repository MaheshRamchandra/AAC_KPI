package com.aac.kpi.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Encounter {
    private final StringProperty encounterId = new SimpleStringProperty();
    private final StringProperty encounterStatus = new SimpleStringProperty("finished");
    private final StringProperty encounterDisplay = new SimpleStringProperty();
    private final StringProperty encounterStart = new SimpleStringProperty();
    private final StringProperty encounterPurpose = new SimpleStringProperty();
    private final StringProperty encounterContactedStaffName = new SimpleStringProperty();
    private final StringProperty encounterReferredBy = new SimpleStringProperty();
    private final StringProperty encounterPatientReference = new SimpleStringProperty();

    public String getEncounterId() { return encounterId.get(); }
    public void setEncounterId(String v) { encounterId.set(v); }
    public StringProperty encounterIdProperty() { return encounterId; }

    public String getEncounterStatus() { return encounterStatus.get(); }
    public void setEncounterStatus(String v) { encounterStatus.set(v); }
    public StringProperty encounterStatusProperty() { return encounterStatus; }

    public String getEncounterDisplay() { return encounterDisplay.get(); }
    public void setEncounterDisplay(String v) { encounterDisplay.set(v); }
    public StringProperty encounterDisplayProperty() { return encounterDisplay; }

    public String getEncounterStart() { return encounterStart.get(); }
    public void setEncounterStart(String v) { encounterStart.set(v); }
    public StringProperty encounterStartProperty() { return encounterStart; }

    public String getEncounterPurpose() { return encounterPurpose.get(); }
    public void setEncounterPurpose(String v) { encounterPurpose.set(v); }
    public StringProperty encounterPurposeProperty() { return encounterPurpose; }

    public String getEncounterContactedStaffName() { return encounterContactedStaffName.get(); }
    public void setEncounterContactedStaffName(String v) { encounterContactedStaffName.set(v); }
    public StringProperty encounterContactedStaffNameProperty() { return encounterContactedStaffName; }

    public String getEncounterReferredBy() { return encounterReferredBy.get(); }
    public void setEncounterReferredBy(String v) { encounterReferredBy.set(v); }
    public StringProperty encounterReferredByProperty() { return encounterReferredBy; }

    public String getEncounterPatientReference() { return encounterPatientReference.get(); }
    public void setEncounterPatientReference(String v) { encounterPatientReference.set(v); }
    public StringProperty encounterPatientReferenceProperty() { return encounterPatientReference; }
}
