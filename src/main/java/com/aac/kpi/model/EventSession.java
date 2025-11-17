package com.aac.kpi.model;

import javafx.beans.property.*;

public class EventSession {
    private final StringProperty compositionId = new SimpleStringProperty();
    private final IntegerProperty numberOfEventSessions = new SimpleIntegerProperty(1);
    private final StringProperty eventSessionId1 = new SimpleStringProperty();
    private final StringProperty eventSessionMode1 = new SimpleStringProperty();
    private final StringProperty eventSessionStartDate1 = new SimpleStringProperty();
    private final StringProperty eventSessionEndDate1 = new SimpleStringProperty();
    private final IntegerProperty eventSessionDuration1 = new SimpleIntegerProperty();
    private final StringProperty eventSessionVenue1 = new SimpleStringProperty();
    private final IntegerProperty eventSessionCapacity1 = new SimpleIntegerProperty();
    private final StringProperty eventSessionPatientReferences1 = new SimpleStringProperty();
    private final javafx.beans.property.BooleanProperty attendedIndicator = new javafx.beans.property.SimpleBooleanProperty(true);
    private final StringProperty purposeOfContact = new SimpleStringProperty("");

    public String getCompositionId() { return compositionId.get(); }
    public void setCompositionId(String v) { compositionId.set(v); }
    public StringProperty compositionIdProperty() { return compositionId; }

    public int getNumberOfEventSessions() { return numberOfEventSessions.get(); }
    public void setNumberOfEventSessions(int v) { numberOfEventSessions.set(v); }
    public IntegerProperty numberOfEventSessionsProperty() { return numberOfEventSessions; }

    public String getEventSessionId1() { return eventSessionId1.get(); }
    public void setEventSessionId1(String v) { eventSessionId1.set(v); }
    public StringProperty eventSessionId1Property() { return eventSessionId1; }

    public String getEventSessionMode1() { return eventSessionMode1.get(); }
    public void setEventSessionMode1(String v) { eventSessionMode1.set(v); }
    public StringProperty eventSessionMode1Property() { return eventSessionMode1; }

    public String getEventSessionStartDate1() { return eventSessionStartDate1.get(); }
    public void setEventSessionStartDate1(String v) { eventSessionStartDate1.set(v); }
    public StringProperty eventSessionStartDate1Property() { return eventSessionStartDate1; }

    public String getEventSessionEndDate1() { return eventSessionEndDate1.get(); }
    public void setEventSessionEndDate1(String v) { eventSessionEndDate1.set(v); }
    public StringProperty eventSessionEndDate1Property() { return eventSessionEndDate1; }

    public int getEventSessionDuration1() { return eventSessionDuration1.get(); }
    public void setEventSessionDuration1(int v) { eventSessionDuration1.set(v); }
    public IntegerProperty eventSessionDuration1Property() { return eventSessionDuration1; }

    public String getEventSessionVenue1() { return eventSessionVenue1.get(); }
    public void setEventSessionVenue1(String v) { eventSessionVenue1.set(v); }
    public StringProperty eventSessionVenue1Property() { return eventSessionVenue1; }

    public int getEventSessionCapacity1() { return eventSessionCapacity1.get(); }
    public void setEventSessionCapacity1(int v) { eventSessionCapacity1.set(v); }
    public IntegerProperty eventSessionCapacity1Property() { return eventSessionCapacity1; }

    public String getEventSessionPatientReferences1() { return eventSessionPatientReferences1.get(); }
    public void setEventSessionPatientReferences1(String v) { eventSessionPatientReferences1.set(v); }
    public StringProperty eventSessionPatientReferences1Property() { return eventSessionPatientReferences1; }

    public boolean isAttendedIndicator() { return attendedIndicator.get(); }
    public void setAttendedIndicator(boolean b) { attendedIndicator.set(b); }
    public javafx.beans.property.BooleanProperty attendedIndicatorProperty() { return attendedIndicator; }

    public String getPurposeOfContact() { return purposeOfContact.get(); }
    public void setPurposeOfContact(String v) { purposeOfContact.set(v); }
    public StringProperty purposeOfContactProperty() { return purposeOfContact; }
}
