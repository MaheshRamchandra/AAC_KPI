package com.aac.kpi.model;

import javafx.beans.property.*;

public class CommonRow {
    private final StringProperty compositionId = new SimpleStringProperty();
    private final IntegerProperty versionId = new SimpleIntegerProperty(1);
    private final StringProperty lastUpdated = new SimpleStringProperty();
    private final StringProperty metaCode = new SimpleStringProperty();
    private final StringProperty reportingMonth = new SimpleStringProperty();
    private final IntegerProperty totalOperatingDays = new SimpleIntegerProperty(20);
    private final IntegerProperty totalClients = new SimpleIntegerProperty(0);
    private final StringProperty status = new SimpleStringProperty("final");
    private final StringProperty authorValue = new SimpleStringProperty();
    private final StringProperty authorDisplay = new SimpleStringProperty();
    private final StringProperty patientReference = new SimpleStringProperty();
    private final StringProperty encounterReferences = new SimpleStringProperty();
    private final StringProperty questionnaireReference = new SimpleStringProperty();
    private final StringProperty attendedEventReferences = new SimpleStringProperty();

    // Resident report extended fields
    private final StringProperty residentVolunteerStatus = new SimpleStringProperty();
    private final StringProperty cstDate = new SimpleStringProperty();
    private final IntegerProperty cfs = new SimpleIntegerProperty();
    private final StringProperty cfsLabel = new SimpleStringProperty();
    private final IntegerProperty socialRiskFactorScore = new SimpleIntegerProperty();
    private final StringProperty socialRiskLabel = new SimpleStringProperty();
    private final StringProperty aapRecommendation = new SimpleStringProperty();
    private final StringProperty socialSupportRecommendation = new SimpleStringProperty();
    private final StringProperty aacOptOutStatus = new SimpleStringProperty();
    private final StringProperty aapOptOutStatus = new SimpleStringProperty();
    private final StringProperty screeningDeclarationDate = new SimpleStringProperty();
    private final StringProperty befriendingOptOutStatus = new SimpleStringProperty();
    private final StringProperty buddyingOptOutStatus = new SimpleStringProperty();
    private final StringProperty residentBefriendingProgrammePeriodStart = new SimpleStringProperty();
    private final StringProperty residentBefriendingProgrammePeriodEnd = new SimpleStringProperty();
    private final StringProperty residentBuddyingProgrammePeriodStart = new SimpleStringProperty();
    private final StringProperty residentBuddyingProgrammePeriodEnd = new SimpleStringProperty();
    private final StringProperty irmsReferralRaisedDate = new SimpleStringProperty();
    private final StringProperty irmsReferralAcceptedDate = new SimpleStringProperty();
    private final StringProperty asgReferralRaisedBy = new SimpleStringProperty();
    private final StringProperty asgReferralAcceptedBy = new SimpleStringProperty();

    public String getCompositionId() { return compositionId.get(); }
    public void setCompositionId(String v) { compositionId.set(v); }
    public StringProperty compositionIdProperty() { return compositionId; }

    public int getVersionId() { return versionId.get(); }
    public void setVersionId(int v) { versionId.set(v); }
    public IntegerProperty versionIdProperty() { return versionId; }

    public String getLastUpdated() { return lastUpdated.get(); }
    public void setLastUpdated(String v) { lastUpdated.set(v); }
    public StringProperty lastUpdatedProperty() { return lastUpdated; }

    public String getMetaCode() { return metaCode.get(); }
    public void setMetaCode(String v) { metaCode.set(v); }
    public StringProperty metaCodeProperty() { return metaCode; }

    public String getReportingMonth() { return reportingMonth.get(); }
    public void setReportingMonth(String v) { reportingMonth.set(v); }
    public StringProperty reportingMonthProperty() { return reportingMonth; }

    public int getTotalOperatingDays() { return totalOperatingDays.get(); }
    public void setTotalOperatingDays(int v) { totalOperatingDays.set(v); }
    public IntegerProperty totalOperatingDaysProperty() { return totalOperatingDays; }

    public int getTotalClients() { return totalClients.get(); }
    public void setTotalClients(int v) { totalClients.set(v); }
    public IntegerProperty totalClientsProperty() { return totalClients; }

    public String getStatus() { return status.get(); }
    public void setStatus(String v) { status.set(v); }
    public StringProperty statusProperty() { return status; }

    public String getAuthorValue() { return authorValue.get(); }
    public void setAuthorValue(String v) { authorValue.set(v); }
    public StringProperty authorValueProperty() { return authorValue; }

    public String getAuthorDisplay() { return authorDisplay.get(); }
    public void setAuthorDisplay(String v) { authorDisplay.set(v); }
    public StringProperty authorDisplayProperty() { return authorDisplay; }

    public String getPatientReference() { return patientReference.get(); }
    public void setPatientReference(String v) { patientReference.set(v); }
    public StringProperty patientReferenceProperty() { return patientReference; }

    public String getEncounterReferences() { return encounterReferences.get(); }
    public void setEncounterReferences(String v) { encounterReferences.set(v); }
    public StringProperty encounterReferencesProperty() { return encounterReferences; }

    public String getQuestionnaireReference() { return questionnaireReference.get(); }
    public void setQuestionnaireReference(String v) { questionnaireReference.set(v); }
    public StringProperty questionnaireReferenceProperty() { return questionnaireReference; }

    public String getAttendedEventReferences() { return attendedEventReferences.get(); }
    public void setAttendedEventReferences(String v) { attendedEventReferences.set(v); }
    public StringProperty attendedEventReferencesProperty() { return attendedEventReferences; }

    public String getResidentVolunteerStatus() { return residentVolunteerStatus.get(); }
    public void setResidentVolunteerStatus(String v) { residentVolunteerStatus.set(v); }
    public StringProperty residentVolunteerStatusProperty() { return residentVolunteerStatus; }

    public String getCstDate() { return cstDate.get(); }
    public void setCstDate(String v) { cstDate.set(v); }
    public StringProperty cstDateProperty() { return cstDate; }

    public int getCfs() { return cfs.get(); }
    public void setCfs(int v) { cfs.set(v); }
    public IntegerProperty cfsProperty() { return cfs; }

    public String getCfsLabel() { return cfsLabel.get(); }
    public void setCfsLabel(String v) { cfsLabel.set(v); }
    public StringProperty cfsLabelProperty() { return cfsLabel; }

    public int getSocialRiskFactorScore() { return socialRiskFactorScore.get(); }
    public void setSocialRiskFactorScore(int v) { socialRiskFactorScore.set(v); }
    public IntegerProperty socialRiskFactorScoreProperty() { return socialRiskFactorScore; }

    public String getSocialRiskLabel() { return socialRiskLabel.get(); }
    public void setSocialRiskLabel(String v) { socialRiskLabel.set(v); }
    public StringProperty socialRiskLabelProperty() { return socialRiskLabel; }

    public String getAapRecommendation() { return aapRecommendation.get(); }
    public void setAapRecommendation(String v) { aapRecommendation.set(v); }
    public StringProperty aapRecommendationProperty() { return aapRecommendation; }

    public String getSocialSupportRecommendation() { return socialSupportRecommendation.get(); }
    public void setSocialSupportRecommendation(String v) { socialSupportRecommendation.set(v); }
    public StringProperty socialSupportRecommendationProperty() { return socialSupportRecommendation; }

    public String getAacOptOutStatus() { return aacOptOutStatus.get(); }
    public void setAacOptOutStatus(String v) { aacOptOutStatus.set(v); }
    public StringProperty aacOptOutStatusProperty() { return aacOptOutStatus; }

    public String getAapOptOutStatus() { return aapOptOutStatus.get(); }
    public void setAapOptOutStatus(String v) { aapOptOutStatus.set(v); }
    public StringProperty aapOptOutStatusProperty() { return aapOptOutStatus; }

    public String getScreeningDeclarationDate() { return screeningDeclarationDate.get(); }
    public void setScreeningDeclarationDate(String v) { screeningDeclarationDate.set(v); }
    public StringProperty screeningDeclarationDateProperty() { return screeningDeclarationDate; }

    public String getBefriendingOptOutStatus() { return befriendingOptOutStatus.get(); }
    public void setBefriendingOptOutStatus(String v) { befriendingOptOutStatus.set(v); }
    public StringProperty befriendingOptOutStatusProperty() { return befriendingOptOutStatus; }

    public String getBuddyingOptOutStatus() { return buddyingOptOutStatus.get(); }
    public void setBuddyingOptOutStatus(String v) { buddyingOptOutStatus.set(v); }
    public StringProperty buddyingOptOutStatusProperty() { return buddyingOptOutStatus; }

    public String getResidentBefriendingProgrammePeriodStart() { return residentBefriendingProgrammePeriodStart.get(); }
    public void setResidentBefriendingProgrammePeriodStart(String v) { residentBefriendingProgrammePeriodStart.set(v); }
    public StringProperty residentBefriendingProgrammePeriodStartProperty() { return residentBefriendingProgrammePeriodStart; }

    public String getResidentBefriendingProgrammePeriodEnd() { return residentBefriendingProgrammePeriodEnd.get(); }
    public void setResidentBefriendingProgrammePeriodEnd(String v) { residentBefriendingProgrammePeriodEnd.set(v); }
    public StringProperty residentBefriendingProgrammePeriodEndProperty() { return residentBefriendingProgrammePeriodEnd; }

    public String getResidentBuddyingProgrammePeriodStart() { return residentBuddyingProgrammePeriodStart.get(); }
    public void setResidentBuddyingProgrammePeriodStart(String v) { residentBuddyingProgrammePeriodStart.set(v); }
    public StringProperty residentBuddyingProgrammePeriodStartProperty() { return residentBuddyingProgrammePeriodStart; }

    public String getResidentBuddyingProgrammePeriodEnd() { return residentBuddyingProgrammePeriodEnd.get(); }
    public void setResidentBuddyingProgrammePeriodEnd(String v) { residentBuddyingProgrammePeriodEnd.set(v); }
    public StringProperty residentBuddyingProgrammePeriodEndProperty() { return residentBuddyingProgrammePeriodEnd; }

    public String getIrmsReferralRaisedDate() { return irmsReferralRaisedDate.get(); }
    public void setIrmsReferralRaisedDate(String v) { irmsReferralRaisedDate.set(v); }
    public StringProperty irmsReferralRaisedDateProperty() { return irmsReferralRaisedDate; }

    public String getIrmsReferralAcceptedDate() { return irmsReferralAcceptedDate.get(); }
    public void setIrmsReferralAcceptedDate(String v) { irmsReferralAcceptedDate.set(v); }
    public StringProperty irmsReferralAcceptedDateProperty() { return irmsReferralAcceptedDate; }

    public String getAsgReferralRaisedBy() { return asgReferralRaisedBy.get(); }
    public void setAsgReferralRaisedBy(String v) { asgReferralRaisedBy.set(v); }
    public StringProperty asgReferralRaisedByProperty() { return asgReferralRaisedBy; }

    public String getAsgReferralAcceptedBy() { return asgReferralAcceptedBy.get(); }
    public void setAsgReferralAcceptedBy(String v) { asgReferralAcceptedBy.set(v); }
    public StringProperty asgReferralAcceptedByProperty() { return asgReferralAcceptedBy; }
}
