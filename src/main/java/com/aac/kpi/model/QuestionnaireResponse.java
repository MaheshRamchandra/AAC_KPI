package com.aac.kpi.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class QuestionnaireResponse {
    private final StringProperty questionnaireId = new SimpleStringProperty();
    private final StringProperty questionnaireStatus = new SimpleStringProperty("completed");
    private final StringProperty q1 = new SimpleStringProperty();
    private final StringProperty q2 = new SimpleStringProperty();
    private final StringProperty q3 = new SimpleStringProperty();
    private final StringProperty q4 = new SimpleStringProperty();
    private final StringProperty q5 = new SimpleStringProperty();
    private final StringProperty q6 = new SimpleStringProperty();
    private final StringProperty q7 = new SimpleStringProperty();
    private final StringProperty q8 = new SimpleStringProperty();
    private final StringProperty q9 = new SimpleStringProperty();
    private final StringProperty q10 = new SimpleStringProperty();
    private final StringProperty questionnairePatientReference = new SimpleStringProperty();

    public String getQuestionnaireId() { return questionnaireId.get(); }
    public void setQuestionnaireId(String v) { questionnaireId.set(v); }
    public StringProperty questionnaireIdProperty() { return questionnaireId; }

    public String getQuestionnaireStatus() { return questionnaireStatus.get(); }
    public void setQuestionnaireStatus(String v) { questionnaireStatus.set(v); }
    public StringProperty questionnaireStatusProperty() { return questionnaireStatus; }

    public String getQ1() { return q1.get(); } public void setQ1(String v) { q1.set(v);} public StringProperty q1Property(){return q1;}
    public String getQ2() { return q2.get(); } public void setQ2(String v) { q2.set(v);} public StringProperty q2Property(){return q2;}
    public String getQ3() { return q3.get(); } public void setQ3(String v) { q3.set(v);} public StringProperty q3Property(){return q3;}
    public String getQ4() { return q4.get(); } public void setQ4(String v) { q4.set(v);} public StringProperty q4Property(){return q4;}
    public String getQ5() { return q5.get(); } public void setQ5(String v) { q5.set(v);} public StringProperty q5Property(){return q5;}
    public String getQ6() { return q6.get(); } public void setQ6(String v) { q6.set(v);} public StringProperty q6Property(){return q6;}
    public String getQ7() { return q7.get(); } public void setQ7(String v) { q7.set(v);} public StringProperty q7Property(){return q7;}
    public String getQ8() { return q8.get(); } public void setQ8(String v) { q8.set(v);} public StringProperty q8Property(){return q8;}
    public String getQ9() { return q9.get(); } public void setQ9(String v) { q9.set(v);} public StringProperty q9Property(){return q9;}
    public String getQ10() { return q10.get(); } public void setQ10(String v) { q10.set(v);} public StringProperty q10Property(){return q10;}

    public String getQuestionnairePatientReference() { return questionnairePatientReference.get(); }
    public void setQuestionnairePatientReference(String v) { questionnairePatientReference.set(v); }
    public StringProperty questionnairePatientReferenceProperty() { return questionnairePatientReference; }
}
