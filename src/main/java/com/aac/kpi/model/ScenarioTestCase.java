package com.aac.kpi.model;

public class ScenarioTestCase {
    private String id;
    private String workItemType;
    private String title;
    private String testStep;
    private String stepAction;
    private String stepExpected;
    private String numberOfSeniors;
    private String cfs;
    private String modeOfEvent;
    private String aapSessionDate;
    private String numberOfAapAttendance;
    private String withinBoundary;
    private String purposeOfContact;
    private String dateOfContact;
    private String age;
    private String remarks;

    public ScenarioTestCase() {
    }

    public ScenarioTestCase(String id, String workItemType, String title, String testStep, String stepAction,
                            String stepExpected, String numberOfSeniors, String cfs, String modeOfEvent,
                            String aapSessionDate, String numberOfAapAttendance, String withinBoundary,
                            String purposeOfContact, String dateOfContact, String age, String remarks) {
        this.id = id;
        this.workItemType = workItemType;
        this.title = title;
        this.testStep = testStep;
        this.stepAction = stepAction;
        this.stepExpected = stepExpected;
        this.numberOfSeniors = numberOfSeniors;
        this.cfs = cfs;
        this.modeOfEvent = modeOfEvent;
        this.aapSessionDate = aapSessionDate;
        this.numberOfAapAttendance = numberOfAapAttendance;
        this.withinBoundary = withinBoundary;
        this.purposeOfContact = purposeOfContact;
        this.dateOfContact = dateOfContact;
        this.age = age;
        this.remarks = remarks;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkItemType() {
        return workItemType;
    }

    public void setWorkItemType(String workItemType) {
        this.workItemType = workItemType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTestStep() {
        return testStep;
    }

    public void setTestStep(String testStep) {
        this.testStep = testStep;
    }

    public String getStepAction() {
        return stepAction;
    }

    public void setStepAction(String stepAction) {
        this.stepAction = stepAction;
    }

    public String getStepExpected() {
        return stepExpected;
    }

    public void setStepExpected(String stepExpected) {
        this.stepExpected = stepExpected;
    }

    public String getNumberOfSeniors() {
        return numberOfSeniors;
    }

    public void setNumberOfSeniors(String numberOfSeniors) {
        this.numberOfSeniors = numberOfSeniors;
    }

    public String getCfs() {
        return cfs;
    }

    public void setCfs(String cfs) {
        this.cfs = cfs;
    }

    public String getModeOfEvent() {
        return modeOfEvent;
    }

    public void setModeOfEvent(String modeOfEvent) {
        this.modeOfEvent = modeOfEvent;
    }

    public String getAapSessionDate() {
        return aapSessionDate;
    }

    public void setAapSessionDate(String aapSessionDate) {
        this.aapSessionDate = aapSessionDate;
    }

    public String getNumberOfAapAttendance() {
        return numberOfAapAttendance;
    }

    public void setNumberOfAapAttendance(String numberOfAapAttendance) {
        this.numberOfAapAttendance = numberOfAapAttendance;
    }

    public String getWithinBoundary() {
        return withinBoundary;
    }

    public void setWithinBoundary(String withinBoundary) {
        this.withinBoundary = withinBoundary;
    }

    public String getPurposeOfContact() {
        return purposeOfContact;
    }

    public void setPurposeOfContact(String purposeOfContact) {
        this.purposeOfContact = purposeOfContact;
    }

    public String getDateOfContact() {
        return dateOfContact;
    }

    public void setDateOfContact(String dateOfContact) {
        this.dateOfContact = dateOfContact;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
