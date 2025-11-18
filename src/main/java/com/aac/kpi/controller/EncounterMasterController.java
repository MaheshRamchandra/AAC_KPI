package com.aac.kpi.controller;

import com.aac.kpi.model.Encounter;
import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.Practitioner;
import com.aac.kpi.service.AppState;
import com.aac.kpi.service.ExcelReader;
import com.aac.kpi.service.ExcelWriter;
import com.aac.kpi.service.RandomDataUtil;
import com.aac.kpi.ui.TableHighlightSupport;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class EncounterMasterController {
    private ObservableList<Encounter> encounters;
    private ObservableList<Patient> patients;
    private ObservableList<EventSession> sessions;
    private ObservableList<Practitioner> practitioners;
    private Label statusLabel;
    private Runnable clearAllHandler;
    private final Set<Encounter> highlightedGeneratedEncounters = new HashSet<>();

    @FXML private TableView<Encounter> table;
    @FXML private TableColumn<Encounter, String> cId;
    @FXML private TableColumn<Encounter, String> cStatus;
    @FXML private TableColumn<Encounter, String> cDisplay;
    @FXML private TableColumn<Encounter, String> cStart;
    @FXML private TableColumn<Encounter, String> cPurpose;
    @FXML private TableColumn<Encounter, String> cStaff;
    @FXML private TableColumn<Encounter, String> cRef;
    @FXML private TableColumn<Encounter, String> cPatientRef;

    private static final String[] DISPLAYS = {"Home Visit","Video Call","Centre Visit","Phone Call"};
    private static final String[] PREFIXES = {"Mr","Mrs","Ms","Dr"};
    private static final String[] STAFF = {"Staff A","Staff B","Staff C","Staff D","Nurse E","Nurse F","Counsellor G"};
    private static final String[] REFERRED = {"GP","Family","Self-referral","Neighbour","Social Worker"};

    public void init(ObservableList<Encounter> encounters,
                     ObservableList<Patient> patients,
                     ObservableList<EventSession> sessions,
                     ObservableList<Practitioner> practitioners,
                     Label statusLabel) {
        this.encounters = encounters;
        this.patients = patients;
        this.sessions = sessions;
        this.practitioners = practitioners;
        this.statusLabel = statusLabel;
        table.setItems(this.encounters);
        table.setEditable(true);
        TableHighlightSupport.install(table, highlightedGeneratedEncounters);
    }

    @FXML private void initialize() {
        if (cId != null) cId.setCellValueFactory(new PropertyValueFactory<>("encounterId"));
        if (cStatus != null) cStatus.setCellValueFactory(new PropertyValueFactory<>("encounterStatus"));
        if (cDisplay != null) cDisplay.setCellValueFactory(new PropertyValueFactory<>("encounterDisplay"));
        if (cStart != null) cStart.setCellValueFactory(new PropertyValueFactory<>("encounterStart"));
        if (cPurpose != null) cPurpose.setCellValueFactory(new PropertyValueFactory<>("encounterPurpose"));
        if (cStaff != null) cStaff.setCellValueFactory(new PropertyValueFactory<>("encounterContactedStaffName"));
        if (cRef != null) cRef.setCellValueFactory(new PropertyValueFactory<>("encounterReferredBy"));
        if (cPatientRef != null) cPatientRef.setCellValueFactory(new PropertyValueFactory<>("encounterPatientReference"));

        if (cId != null) { cId.setCellFactory(TextFieldTableCell.forTableColumn()); cId.setOnEditCommit(e -> e.getRowValue().setEncounterId(e.getNewValue())); }
        if (cStatus != null) { cStatus.setCellFactory(TextFieldTableCell.forTableColumn()); cStatus.setOnEditCommit(e -> e.getRowValue().setEncounterStatus(e.getNewValue())); }
        if (cDisplay != null) { cDisplay.setCellFactory(TextFieldTableCell.forTableColumn()); cDisplay.setOnEditCommit(e -> e.getRowValue().setEncounterDisplay(e.getNewValue())); }
        if (cStart != null) { cStart.setCellFactory(TextFieldTableCell.forTableColumn()); cStart.setOnEditCommit(e -> e.getRowValue().setEncounterStart(toIsoOffset(e.getNewValue()))); }
        if (cPurpose != null) { cPurpose.setCellFactory(TextFieldTableCell.forTableColumn()); cPurpose.setOnEditCommit(e -> e.getRowValue().setEncounterPurpose(e.getNewValue())); }
        if (cStaff != null) { cStaff.setCellFactory(TextFieldTableCell.forTableColumn()); cStaff.setOnEditCommit(e -> e.getRowValue().setEncounterContactedStaffName(e.getNewValue())); }
        if (cRef != null) { cRef.setCellFactory(TextFieldTableCell.forTableColumn()); cRef.setOnEditCommit(e -> e.getRowValue().setEncounterReferredBy(e.getNewValue())); }
        if (cPatientRef != null) { cPatientRef.setCellFactory(TextFieldTableCell.forTableColumn()); cPatientRef.setOnEditCommit(e -> e.getRowValue().setEncounterPatientReference(e.getNewValue())); }
    }

    @FXML private void onGenerate() {
        Optional<PurposeSelection> purpose = promptForPurposeSelection();
        if (purpose.isEmpty()) return;
        GenerateEncountersDialog dlg = new GenerateEncountersDialog();
        var opt = dlg.showAndWait();
        if (opt.isEmpty()) return;
        var cfg = opt.get();
        LocalDate start = cfg.start; LocalDate end = cfg.end;
        List<Encounter> list = new ArrayList<>();
        String selectedPurpose = purpose.get().getValue();
        for (int i = 0; i < cfg.total; i++) list.add(randomEncounter(start, end, selectedPurpose));
        // Link each generated encounter to a patient (round-robin) so resident_report rules apply
        if (patients != null && !patients.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                String pid = patients.get(i % patients.size()).getPatientId();
                list.get(i).setEncounterPatientReference(pid);
            }
        }
        encounters.addAll(list);
        markGeneratedEncounters(list);
        updateStatus();
    }

    private Encounter randomEncounter(LocalDate startDate, LocalDate endDate, String purpose) {
        Encounter e = new Encounter();
        e.setEncounterId(RandomDataUtil.uuid32().toUpperCase(java.util.Locale.ROOT));
        e.setEncounterStatus("finished");
        e.setEncounterDisplay(DISPLAYS[new java.util.Random().nextInt(DISPLAYS.length)]);
        LocalDateTime start = startDate.atTime(9, 0);
        e.setEncounterStart(RandomDataUtil.isoTimestampWithOffset(start, "+08:00"));
        e.setEncounterPurpose(purpose);
        String staff = PREFIXES[new java.util.Random().nextInt(PREFIXES.length)] + " " + STAFF[new java.util.Random().nextInt(STAFF.length)];
        e.setEncounterContactedStaffName(staff);
        e.setEncounterReferredBy(REFERRED[new java.util.Random().nextInt(REFERRED.length)]);
        return e;
    }

    @FXML private void onUploadExcel() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File f = fc.showOpenDialog(table.getScene() != null ? table.getScene().getWindow() : null);
        if (f == null) return;
        try {
            List<Encounter> list = ExcelReader.readEncounters(f);
            if (!list.isEmpty()) encounters.setAll(list);
            updateStatus();
            clearGeneratedEncountersHighlight();
        } catch (Exception ex) { showAlert("Failed to load encounters: " + ex.getMessage()); }
    }

    @FXML private void onAnalyze() {
        StringBuilder sb = new StringBuilder();
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < encounters.size(); i++) {
            var e = encounters.get(i);
            if (e.getEncounterId() == null || !e.getEncounterId().matches("[A-Za-z0-9]{32}")) sb.append("Row ").append(i+1).append(": Invalid encounter_id\n");
            if (!"finished".equalsIgnoreCase(e.getEncounterStatus())) sb.append("Row ").append(i+1).append(": status not 'finished'\n");
            if (!ids.add(e.getEncounterId())) sb.append("Row ").append(i+1).append(": duplicate id\n");
            if (e.getEncounterStart()==null || !e.getEncounterStart().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+08:00")) sb.append("Row ").append(i+1).append(": start not ISO+08:00\n");
        }
        if (sb.length()==0) showInfo("Encounter Master OK"); else showInfo(sb.toString());
    }

    @FXML private void onExport() {
        try {
            java.io.File dest = com.aac.kpi.service.AppState.getCurrentExcelFile();
            if (dest == null) {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
                fc.setInitialFileName("KPI_Data.xlsx");
                dest = fc.showSaveDialog(table.getScene().getWindow());
                if (dest == null) return;
            }
            File file = ExcelWriter.saveToExcel(patients, sessions, practitioners, encounters, java.util.List.of(), java.util.List.of(), dest);
            showInfo("Exported to: " + file.getAbsolutePath());
            statusLabel.setText(String.format("Generated %d patients | %d sessions | %d practitioners | %d encounters | Last export: %s",
                    patients.size(), sessions.size(), practitioners.size(), encounters.size(), ExcelWriter.nowStamp()));
            com.aac.kpi.service.AppState.setDirty(false);
        } catch (Exception ex) { showAlert("Export failed: " + ex.getMessage()); }
    }

    public void refreshTable() { if (table != null) table.refresh(); }

    private void updateStatus() {
        if (statusLabel != null) {
            statusLabel.setText(String.format("Generated %d patients | %d sessions | %d practitioners | %d encounters",
                    patients.size(), sessions.size(), practitioners.size(), encounters.size()));
        }
    }

    private Optional<PurposeSelection> promptForPurposeSelection() {
        Dialog<PurposeSelection> dialog = new Dialog<>();
        dialog.setTitle("Encounter purpose");
        dialog.setHeaderText("Select a uniform purpose of contact for every generated encounter:");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<PurposeSelection> combo = new ComboBox<>();
        combo.getItems().addAll(PurposeSelection.values());
        combo.getSelectionModel().select(PurposeSelection.BEFRIENDING_CAPITAL);

        Label note = new Label("Use the dropdown to choose one of the required purpose labels before generation.");
        note.setWrapText(true);
        note.setMaxWidth(320);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Purpose:"), 0, 0);
        grid.add(combo, 1, 0);
        grid.add(note, 0, 1, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? combo.getValue() : null);
        dialog.getDialogPane().setPrefWidth(380);
        return dialog.showAndWait();
    }

    private enum PurposeSelection {
        BEFRIENDING_CAPITAL("Befriending"),
        BEFRIENDING_LOWER("befriending"),
        BUDDYING_LOWER("buddying"),
        BUDDYING_CAPITAL("Buddying"),
        FUNCTIONAL("Functional or healthscreening Client Self-Declaration");

        private final String value;

        PurposeSelection(String value) { this.value = value; }

        public String getValue() { return value; }

        @Override
        public String toString() { return value; }
    }

    private void showAlert(String msg) { Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK); a.setHeaderText(null); a.showAndWait(); }
    private void showInfo(String msg) { Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK); a.setHeaderText(null); a.showAndWait(); }

    public void setClearAllHandler(Runnable handler) {
        this.clearAllHandler = handler;
    }

    private void markGeneratedEncounters(Collection<Encounter> newItems) {
        if (newItems == null || newItems.isEmpty()) return;
        Encounter first = newItems.iterator().next();
        TableHighlightSupport.add(table, first, highlightedGeneratedEncounters);
        AppState.addHighlightedEncounterId(first.getEncounterId());
    }

    private void clearGeneratedEncountersHighlight() {
        TableHighlightSupport.clear(table, highlightedGeneratedEncounters);
        AppState.clearHighlightedEncounterIds();
    }

    @FXML private void onClearSheet() {
        encounters.clear();
        clearGeneratedEncountersHighlight();
        AppState.setDirty(true);
        updateStatus();
        refreshTable();
    }

    @FXML private void onClearAll() {
        if (clearAllHandler != null) clearAllHandler.run();
    }

    // Utilities
    private static String toIsoOffset(String s) {
        if (s == null || s.isBlank()) return s;
        java.time.ZoneOffset off = java.time.ZoneOffset.of("+08:00");
        try {
            return java.time.OffsetDateTime.parse(s, java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .withOffsetSameInstant(off)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        } catch (Exception ignored) {}
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return ldt.atOffset(off).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        } catch (Exception ignored) {}
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return ldt.atOffset(off).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        } catch (Exception ignored) {}
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return d.atStartOfDay().atOffset(off).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        } catch (Exception ignored) {}
        return s;
    }
}
