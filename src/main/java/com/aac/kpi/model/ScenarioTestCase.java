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
    private String contactLogs;
    private String kpiType;
    private String totalRegistrations;
    private String attendedIndicator;
    private String encounterStart;
    private String patientBirthdate;
    private String reportingMonth;
    private String reportDate;
    private String socialRiskFactorScore;
    private String buddyingProgrammePeriodStart;
    private String buddyingProgrammePeriodEnd;
    private String befriendingProgrammePeriodStart;
    private String befriendingProgrammePeriodEnd;
    private java.util.Map<String, String> extraFields = new java.util.LinkedHashMap<>();
    private java.util.List<ColumnOverride> columnOverrides = new java.util.ArrayList<>();

    public ScenarioTestCase() {
    }

    public ScenarioTestCase(String id, String workItemType, String title, String testStep, String stepAction,
                            String stepExpected, String numberOfSeniors, String cfs, String modeOfEvent,
                            String aapSessionDate, String numberOfAapAttendance, String withinBoundary,
                            String purposeOfContact, String dateOfContact, String age, String remarks,
                            String contactLogs) {
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
        this.contactLogs = contactLogs;
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

    public String getContactLogs() {
        return contactLogs;
    }

    public void setContactLogs(String contactLogs) {
        this.contactLogs = contactLogs;
    }

    public String getKpiType() {
        return kpiType;
    }

    public void setKpiType(String kpiType) {
        this.kpiType = kpiType;
    }

    public String getTotalRegistrations() {
        return totalRegistrations;
    }

    public void setTotalRegistrations(String totalRegistrations) {
        this.totalRegistrations = totalRegistrations;
    }

    public String getAttendedIndicator() {
        return attendedIndicator;
    }

    public void setAttendedIndicator(String attendedIndicator) {
        this.attendedIndicator = attendedIndicator;
    }

    public String getEncounterStart() {
        return encounterStart;
    }

    public void setEncounterStart(String encounterStart) {
        this.encounterStart = encounterStart;
    }

    public String getPatientBirthdate() {
        return patientBirthdate;
    }

    public void setPatientBirthdate(String patientBirthdate) {
        this.patientBirthdate = patientBirthdate;
    }

    public String getReportingMonth() {
        return reportingMonth;
    }

    public void setReportingMonth(String reportingMonth) {
        this.reportingMonth = reportingMonth;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getSocialRiskFactorScore() {
        return socialRiskFactorScore;
    }

    public void setSocialRiskFactorScore(String socialRiskFactorScore) {
        this.socialRiskFactorScore = socialRiskFactorScore;
    }

    public String getBuddyingProgrammePeriodStart() {
        return buddyingProgrammePeriodStart;
    }

    public void setBuddyingProgrammePeriodStart(String buddyingProgrammePeriodStart) {
        this.buddyingProgrammePeriodStart = buddyingProgrammePeriodStart;
    }

    public String getBuddyingProgrammePeriodEnd() {
        return buddyingProgrammePeriodEnd;
    }

    public void setBuddyingProgrammePeriodEnd(String buddyingProgrammePeriodEnd) {
        this.buddyingProgrammePeriodEnd = buddyingProgrammePeriodEnd;
    }

    public String getBefriendingProgrammePeriodStart() {
        return befriendingProgrammePeriodStart;
    }

    public void setBefriendingProgrammePeriodStart(String befriendingProgrammePeriodStart) {
        this.befriendingProgrammePeriodStart = befriendingProgrammePeriodStart;
    }

    public String getBefriendingProgrammePeriodEnd() {
        return befriendingProgrammePeriodEnd;
    }

    public void setBefriendingProgrammePeriodEnd(String befriendingProgrammePeriodEnd) {
        this.befriendingProgrammePeriodEnd = befriendingProgrammePeriodEnd;
    }

    public java.util.Map<String, String> getExtraFields() {
        return extraFields;
    }

    public void setExtraFields(java.util.Map<String, String> extraFields) {
        this.extraFields = extraFields == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(extraFields);
    }

    public void putExtraField(String key, String value) {
        if (extraFields == null) {
            extraFields = new java.util.LinkedHashMap<>();
        }
        extraFields.put(key, value);
    }

    public java.util.List<ColumnOverride> getColumnOverrides() { return columnOverrides; }
    public void setColumnOverrides(java.util.List<ColumnOverride> overrides) {
        this.columnOverrides = overrides == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(overrides);
    }
    public void addColumnOverride(ColumnOverride override) {
        if (columnOverrides == null) columnOverrides = new java.util.ArrayList<>();
        columnOverrides.add(override);
    }

    public static final class ColumnOverride {
        private String sheet;
        private String column;
        private String value;

        public ColumnOverride() {}
        public ColumnOverride(String sheet, String column, String value) {
            this.sheet = sheet;
            this.column = column;
            this.value = value;
        }

        public String getSheet() { return sheet; }
        public void setSheet(String sheet) { this.sheet = sheet; }

        public String getColumn() { return column; }
        public void setColumn(String column) { this.column = column; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        @Override
        public String toString() {
            return (sheet == null ? "" : sheet) + "." + (column == null ? "" : column) + "=" + (value == null ? "" : value);
        }
    }
}
