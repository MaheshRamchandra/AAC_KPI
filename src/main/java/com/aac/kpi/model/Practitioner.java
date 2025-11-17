package com.aac.kpi.model;

import javafx.beans.property.*;

public class Practitioner {
    private final StringProperty practitionerId = new SimpleStringProperty();
    private final StringProperty practitionerIdentifierValue = new SimpleStringProperty();
    private final StringProperty practitionerIdentifierSystem = new SimpleStringProperty();
    private final StringProperty practitionerManpowerPosition = new SimpleStringProperty();
    private final StringProperty practitionerVolunteerName = new SimpleStringProperty();
    private final DoubleProperty practitionerManpowerCapacity = new SimpleDoubleProperty();
    private final IntegerProperty practitionerVolunteerAge = new SimpleIntegerProperty();
    private final StringProperty workingRemarks = new SimpleStringProperty();

    public String getPractitionerId() { return practitionerId.get(); }
    public void setPractitionerId(String v) { practitionerId.set(v); }
    public StringProperty practitionerIdProperty() { return practitionerId; }

    public String getPractitionerIdentifierValue() { return practitionerIdentifierValue.get(); }
    public void setPractitionerIdentifierValue(String v) { practitionerIdentifierValue.set(v); }
    public StringProperty practitionerIdentifierValueProperty() { return practitionerIdentifierValue; }

    public String getPractitionerIdentifierSystem() { return practitionerIdentifierSystem.get(); }
    public void setPractitionerIdentifierSystem(String v) { practitionerIdentifierSystem.set(v); }
    public StringProperty practitionerIdentifierSystemProperty() { return practitionerIdentifierSystem; }

    public String getPractitionerManpowerPosition() { return practitionerManpowerPosition.get(); }
    public void setPractitionerManpowerPosition(String v) { practitionerManpowerPosition.set(v); }
    public StringProperty practitionerManpowerPositionProperty() { return practitionerManpowerPosition; }

    public String getPractitionerVolunteerName() { return practitionerVolunteerName.get(); }
    public void setPractitionerVolunteerName(String v) { practitionerVolunteerName.set(v); }
    public StringProperty practitionerVolunteerNameProperty() { return practitionerVolunteerName; }

    public double getPractitionerManpowerCapacity() { return practitionerManpowerCapacity.get(); }
    public void setPractitionerManpowerCapacity(double v) { practitionerManpowerCapacity.set(v); }
    public DoubleProperty practitionerManpowerCapacityProperty() { return practitionerManpowerCapacity; }

    public int getPractitionerVolunteerAge() { return practitionerVolunteerAge.get(); }
    public void setPractitionerVolunteerAge(int v) { practitionerVolunteerAge.set(v); }
    public IntegerProperty practitionerVolunteerAgeProperty() { return practitionerVolunteerAge; }

    public String getWorkingRemarks() { return workingRemarks.get(); }
    public void setWorkingRemarks(String v) { workingRemarks.set(v); }
    public StringProperty workingRemarksProperty() { return workingRemarks; }
}

