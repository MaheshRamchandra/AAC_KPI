package com.aac.kpi.controller;

import com.aac.kpi.model.*;
import com.aac.kpi.service.AppState;
import com.aac.kpi.service.CommonBuilderService;
import com.aac.kpi.service.ExcelReader;
import com.aac.kpi.service.ExcelWriter;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class CommonController {
    private ObservableList<CommonRow> commons;
    private ObservableList<Patient> patients;
    private ObservableList<EventSession> sessions;
    private ObservableList<Encounter> encounters;
    private ObservableList<Practitioner> practitioners;
    private ObservableList<QuestionnaireResponse> questionnaires;
    private Label statusLabel;
    private Runnable clearAllHandler;

    @FXML private TableView<CommonRow> table;
    @FXML private TableColumn<CommonRow, String> cComp;
    @FXML private TableColumn<CommonRow, Number> cVersion;
    @FXML private TableColumn<CommonRow, String> cUpdated;
    @FXML private TableColumn<CommonRow, String> cMeta;
    @FXML private TableColumn<CommonRow, String> cMonth;
    @FXML private TableColumn<CommonRow, Number> cDays;
    @FXML private TableColumn<CommonRow, Number> cClients;
    @FXML private TableColumn<CommonRow, String> cStatus;
    @FXML private TableColumn<CommonRow, String> cAuthorVal;
    @FXML private TableColumn<CommonRow, String> cAuthorDisp;
    @FXML private TableColumn<CommonRow, String> cPatientRef;
    @FXML private TableColumn<CommonRow, String> cEncRefs;
    @FXML private TableColumn<CommonRow, String> cQuestRef;
    @FXML private TableColumn<CommonRow, String> cAttendRefs;

    public void init(ObservableList<CommonRow> commons,
                     ObservableList<Patient> patients,
                     ObservableList<EventSession> sessions,
                     ObservableList<Encounter> encounters,
                     ObservableList<Practitioner> practitioners,
                     ObservableList<QuestionnaireResponse> questionnaires,
                     Label statusLabel) {
        this.commons = commons;
        this.patients = patients;
        this.sessions = sessions;
        this.encounters = encounters;
        this.practitioners = practitioners;
        this.questionnaires = questionnaires;
        this.statusLabel = statusLabel;
        table.setItems(this.commons);
    }

    @FXML private void initialize() {
        if (cComp != null) cComp.setCellValueFactory(new PropertyValueFactory<>("compositionId"));
        if (cVersion != null) cVersion.setCellValueFactory(new PropertyValueFactory<>("versionId"));
        if (cUpdated != null) cUpdated.setCellValueFactory(new PropertyValueFactory<>("lastUpdated"));
        if (cMeta != null) cMeta.setCellValueFactory(new PropertyValueFactory<>("metaCode"));
        if (cMonth != null) cMonth.setCellValueFactory(new PropertyValueFactory<>("reportingMonth"));
        if (cDays != null) cDays.setCellValueFactory(new PropertyValueFactory<>("totalOperatingDays"));
        if (cClients != null) cClients.setCellValueFactory(new PropertyValueFactory<>("totalClients"));
        if (cStatus != null) cStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        if (cAuthorVal != null) cAuthorVal.setCellValueFactory(new PropertyValueFactory<>("authorValue"));
        if (cAuthorDisp != null) cAuthorDisp.setCellValueFactory(new PropertyValueFactory<>("authorDisplay"));
        if (cPatientRef != null) cPatientRef.setCellValueFactory(new PropertyValueFactory<>("patientReference"));
        if (cEncRefs != null) cEncRefs.setCellValueFactory(new PropertyValueFactory<>("encounterReferences"));
        if (cQuestRef != null) cQuestRef.setCellValueFactory(new PropertyValueFactory<>("questionnaireReference"));
        if (cAttendRefs != null) cAttendRefs.setCellValueFactory(new PropertyValueFactory<>("attendedEventReferences"));
    }

    @FXML private void onBuild() {
        Optional<ReportingFieldsDialog.ReportingFields> reporting = promptForReportingFields();
        if (reporting.isEmpty()) return;
        Optional<CfsSelection> selection = promptForCfsSelection();
        Optional<SocialRiskSelection> riskSelection = promptForSocialRiskSelection();
        Optional<EventReportSelection> eventSelection = promptForEventReportSelection();
        AppState.setEventReportLabel(eventSelection.map(EventReportSelection::getValue).orElse(""));
        List<CommonRow> built = CommonBuilderService.build(patients, sessions, encounters, questionnaires, practitioners);
        reporting.ifPresent(fields -> applyReportingFields(built, fields));
        selection.ifPresent(sel -> applyCfsSelection(built, sel));
        riskSelection.ifPresent(sel -> applySocialRiskSelection(built, sel));
        commons.setAll(built);
        updateStatus();
    }

    @FXML private void onUploadExcel() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File f = fc.showOpenDialog(table.getScene() != null ? table.getScene().getWindow() : null);
        if (f == null) return;
        try {
            List<CommonRow> list = ExcelReader.readCommon(f);
            if (!list.isEmpty()) commons.setAll(list);
            updateStatus();
        } catch (Exception ex) { showAlert("Failed to load Common: " + ex.getMessage()); }
    }

    @FXML private void onAnalyze() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < commons.size(); i++) {
            CommonRow r = commons.get(i);
            if (r.getPatientReference()==null || r.getPatientReference().isBlank()) sb.append("Row ").append(i+1).append(": missing patient_reference\n");
            if (r.getAuthorValue()==null || r.getAuthorValue().isBlank()) sb.append("Row ").append(i+1).append(": missing author_value\n");
        }
        if (sb.length()==0) showInfo("Common OK"); else showInfo(sb.toString());
    }

    @FXML private void onExport() {
        try {
            Optional<ReportingFieldsDialog.ReportingFields> reporting = promptForReportingFields();
            if (reporting.isEmpty()) return;
            java.io.File dest = com.aac.kpi.service.AppState.getCurrentExcelFile();
            if (dest == null) {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
                fc.setInitialFileName("KPI_Data.xlsx");
                dest = fc.showSaveDialog(table.getScene().getWindow());
                if (dest == null) return;
            }
            // Ensure resident_report data exists: auto-build if empty
            if (commons.isEmpty()) {
                List<CommonRow> built = CommonBuilderService.build(patients, sessions, encounters, questionnaires, practitioners);
                commons.setAll(built);
            }
            reporting.ifPresent(fields -> applyReportingFields(commons, fields));
            // Prompt for number of practitioners for volunteer_attendance_report
            int total = practitioners != null ? practitioners.size() : 0;
            if (total > 0) {
                TextInputDialog dlg = new TextInputDialog(String.valueOf(Math.min(total, 10)));
                dlg.setTitle("Volunteer Attendance");
                dlg.setHeaderText(null);
                dlg.setContentText(String.format("Enter number of practitioners (max %d):", total));
                java.util.Optional<String> res = dlg.showAndWait();
                if (res.isEmpty()) return; // user cancelled
                int n;
                try { n = Integer.parseInt(res.get().trim()); } catch (Exception ex) { showAlert("Please enter a valid integer."); return; }
                if (n < 1 || n > total) {
                    showAlert("Total number of practitioners is " + total + ". Please choose a number between 1 and " + total + ".");
                    return;
                }
                com.aac.kpi.service.AppState.setVolunteerPractitionerCount(n);
            } else {
                com.aac.kpi.service.AppState.setVolunteerPractitionerCount(0);
            }
            File file = ExcelWriter.saveToExcel(patients, sessions, practitioners, encounters, questionnaires, commons, dest);
            showInfo("Exported to: " + file.getAbsolutePath());
            updateStatus();
        } catch (Exception ex) { showAlert("Export failed: " + ex.getMessage()); }
    }

    public void refreshTable() { if (table != null) table.refresh(); }

    // Allow MainController to fetch current items for export
    public java.util.List<CommonRow> getItems() { return table != null ? table.getItems() : java.util.List.of(); }

    private void updateStatus() {
        if (statusLabel != null) {
            statusLabel.setText(String.format("Common rows: %d | Patients %d | Sessions %d | Encounters %d | Questionnaires %d",
                    commons.size(), patients.size(), sessions.size(), encounters.size(), questionnaires.size()));
        }
    }

    private Optional<ReportingFieldsDialog.ReportingFields> promptForReportingFields() {
        return ReportingFieldsDialog.prompt(AppState.getReportingMonthOverride(), AppState.getReportDateOverride());
    }

    private void applyReportingFields(List<CommonRow> rows, ReportingFieldsDialog.ReportingFields fields) {
        if (rows == null || fields == null) return;
        for (CommonRow row : rows) {
            if (row == null) continue;
            row.setReportingMonth(fields.reportingMonth());
            row.setLastUpdated(fields.reportDate());
        }
        AppState.setReportingMonthOverride(fields.reportingMonth());
        AppState.setReportDateOverride(fields.reportDate());
        refreshTable();
    }

    private void showAlert(String msg) { Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK); a.setHeaderText(null); a.showAndWait(); }
    private void showInfo(String msg) { Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK); a.setHeaderText(null); a.showAndWait(); }

    private void applyCfsSelection(List<CommonRow> rows, CfsSelection selection) {
        for (CommonRow row : rows) {
            row.setCfs(selection.getValue());
            row.setCfsLabel(selection.getLabel());
        }
    }

    private void applySocialRiskSelection(List<CommonRow> rows, SocialRiskSelection selection) {
        for (CommonRow row : rows) {
            row.setSocialRiskLabel(selection.getLabel());
        }
    }

    private Optional<CfsSelection> promptForCfsSelection() {
        Dialog<CfsSelection> dialog = new Dialog<>();
        dialog.setTitle("Resident CFS selection");
        dialog.setHeaderText("Choose the baseline CFS group/value for the resident report:");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<CfsSelection> combo = new ComboBox<>();
        combo.getItems().addAll(CfsSelection.values());
        combo.getSelectionModel().select(CfsSelection.GROUP1_RANGE);

        Label note = new Label("Group 2 allows the 4-5 range or single 4/5 entries, while Group 3 lets you pick a definitive frailty score.");
        note.setWrapText(true);
        note.setMaxWidth(340);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("CFS option:"), 0, 0);
        grid.add(combo, 1, 0);
        grid.add(note, 0, 1, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? combo.getValue() : null);
        dialog.getDialogPane().setPrefWidth(420);
        return dialog.showAndWait();
    }

    public enum CfsSelection {
        GROUP1_RANGE("Group 1 — 1-3", "1-3", 3),
        GROUP2_RANGE("Group 2 — 4-5", "4-5", 4),
        GROUP2_4("Group 2 — 4", "4", 4),
        GROUP2_5("Group 2 — 5", "5", 5),
        GROUP3_6("Group 3 — 6", "6", 6),
        GROUP3_7("Group 3 — 7", "7", 7),
        GROUP3_8("Group 3 — 8", "8", 8),
        GROUP3_9("Group 3 — 9", "9", 9);

        private final String description;
        private final String label;
        private final int value;

        CfsSelection(String description, String label, int value) {
            this.description = description;
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }

        public int getValue() { return value; }

        @Override
        public String toString() { return description; }
    }

    private Optional<SocialRiskSelection> promptForSocialRiskSelection() {
        Dialog<SocialRiskSelection> dialog = new Dialog<>();
        dialog.setTitle("Social Risk Factor selection");
        dialog.setHeaderText("Choose whether the resident report should display 1 or >1 in the social risk column:");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<SocialRiskSelection> combo = new ComboBox<>();
        combo.getItems().addAll(SocialRiskSelection.values());
        combo.getSelectionModel().select(SocialRiskSelection.ONE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Social risk option:"), 0, 0);
        grid.add(combo, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? combo.getValue() : null);
        dialog.getDialogPane().setPrefWidth(360);
        return dialog.showAndWait();
    }

    private enum SocialRiskSelection {
        ONE("1", "1"),
        MORE_THAN_ONE(">1", ">1");

        private final String description;
        private final String label;

        SocialRiskSelection(String description, String label) {
            this.description = description;
            this.label = label;
        }

        public String getLabel() { return label; }

        @Override
        public String toString() { return description; }
    }

    private Optional<EventReportSelection> promptForEventReportSelection() {
        Dialog<EventReportSelection> dialog = new Dialog<>();
        dialog.setTitle("Event report label");
        dialog.setHeaderText("Pick the label that should appear under Event Report section:");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<EventReportSelection> combo = new ComboBox<>();
        combo.getItems().addAll(EventReportSelection.values());
        combo.getSelectionModel().select(EventReportSelection.PHYSICAL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Label:"), 0, 0);
        grid.add(combo, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? combo.getValue() : null);
        dialog.getDialogPane().setPrefWidth(420);
        return dialog.showAndWait();
    }

    private enum EventReportSelection {
        PHYSICAL("Physical"),
        OTHERS("Others"),
        VOLUNTEERISM("Volunteerism"),
        PHYSICAL_HEALTH("PHYSICAL HEALTH"),
        COGNITIVE_ALLCAPS("COGNITIVE"),
        ACTIVE("ACTIVE"),
        SHARP("SHARP"),
        LEARNING_ALLCAPS("LEARNING"),
        CONNECTED("CONNECTED"),
        TOUCHPOINT("TOUCHPOINT"),
        COGNITIVE("Cognitive"),
        LEARNING("Learning"),
        SOCIAL_COMMUNAL("SOCIAL - COMMUNAL DINING ONLY");

        private final String value;

        EventReportSelection(String value) { this.value = value; }
        public String getValue() { return value; }
        @Override public String toString() { return value; }
    }

    public void setClearAllHandler(Runnable handler) {
        this.clearAllHandler = handler;
    }

    @FXML
    private void onClearSheet() {
        commons.clear();
        AppState.setDirty(true);
        updateStatus();
        refreshTable();
    }

    @FXML
    private void onClearAll() {
        if (clearAllHandler != null) clearAllHandler.run();
    }
}
