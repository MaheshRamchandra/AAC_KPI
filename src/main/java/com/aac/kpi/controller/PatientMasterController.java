package com.aac.kpi.controller;

import com.aac.kpi.model.Patient;
import com.aac.kpi.service.*;
import com.aac.kpi.ui.TableHighlightSupport;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.scene.input.*;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.util.*;

public class PatientMasterController {
    private ObservableList<Patient> patients;
    private ObservableList<com.aac.kpi.model.EventSession> sessions;
    private ObservableList<com.aac.kpi.model.Practitioner> practitioners;
    private Runnable clearAllHandler;
    private Label statusLabel;
    private final Set<Patient> highlightedGeneratedPatients = new HashSet<>();
    private final Set<Patient> issueHighlights = new HashSet<>();

    @FXML private TableView<Patient> table;
    @FXML private TableColumn<Patient, String> cPatientId;
    @FXML private TableColumn<Patient, String> cIdentifier;
    @FXML private TableColumn<Patient, String> cBirthdate;
    @FXML private TableColumn<Patient, String> cPostal;
    @FXML private TableColumn<Patient, String> cRefs;
    @FXML private TableColumn<Patient, String> cRemarks;
    @FXML private TableColumn<Patient, Integer> cGroup;
    @FXML private TableColumn<Patient, String> cType;
    @FXML private TableColumn<Patient, String> cAac;
    @FXML private TableColumn<Patient, Integer> cCfs;
    @FXML private TableColumn<Patient, Integer> cRf;
    @FXML private TableColumn<Patient, String> cKpiType;
    @FXML private TableColumn<Patient, String> cKpiGroup;

    public void init(ObservableList<Patient> patients,
                     ObservableList<com.aac.kpi.model.EventSession> sessions,
                     ObservableList<com.aac.kpi.model.Practitioner> practitioners,
                     Label statusLabel) {
        this.patients = patients;
        this.sessions = sessions;
        this.practitioners = practitioners;
        this.statusLabel = statusLabel;
        table.setItems(this.patients);
        table.setEditable(true);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        installCopyHandler();
        TableHighlightSupport.install(table, highlightedGeneratedPatients, issueHighlights);
    }

    @FXML
    private void initialize() {
        if (cPatientId != null) cPatientId.setCellValueFactory(new PropertyValueFactory<>("patientId"));
        if (cIdentifier != null) cIdentifier.setCellValueFactory(new PropertyValueFactory<>("patientIdentifierValue"));
        if (cBirthdate != null) cBirthdate.setCellValueFactory(new PropertyValueFactory<>("patientBirthdate"));
        if (cPostal != null) cPostal.setCellValueFactory(new PropertyValueFactory<>("patientPostalCode"));
        if (cRefs != null) cRefs.setCellValueFactory(new PropertyValueFactory<>("attendedEventReferences"));
        if (cRemarks != null) cRemarks.setCellValueFactory(new PropertyValueFactory<>("workingRemarks"));
        if (cGroup != null) cGroup.setCellValueFactory(new PropertyValueFactory<>("group"));
        if (cType != null) cType.setCellValueFactory(new PropertyValueFactory<>("type"));
        if (cAac != null) cAac.setCellValueFactory(new PropertyValueFactory<>("aac"));
        if (cCfs != null) cCfs.setCellValueFactory(new PropertyValueFactory<>("cfs"));
        if (cRf != null) cRf.setCellValueFactory(new PropertyValueFactory<>("socialRiskFactor"));
        if (cKpiType != null) cKpiType.setCellValueFactory(new PropertyValueFactory<>("kpiType"));
        if (cKpiGroup != null) cKpiGroup.setCellValueFactory(new PropertyValueFactory<>("kpiGroup"));

        // Make columns editable
        if (cPatientId != null) {
            cPatientId.setCellFactory(TextFieldTableCell.forTableColumn());
            cPatientId.setOnEditCommit(e -> e.getRowValue().setPatientId(e.getNewValue()));
        }
        if (cIdentifier != null) {
            cIdentifier.setCellFactory(TextFieldTableCell.forTableColumn());
            cIdentifier.setOnEditCommit(e -> { e.getRowValue().setPatientIdentifierValue(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cBirthdate != null) {
            cBirthdate.setCellFactory(TextFieldTableCell.forTableColumn());
            cBirthdate.setOnEditCommit(e -> { e.getRowValue().setPatientBirthdate(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cPostal != null) {
            cPostal.setCellFactory(TextFieldTableCell.forTableColumn());
            cPostal.setOnEditCommit(e -> { e.getRowValue().setPatientPostalCode(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cRefs != null) {
            cRefs.setCellFactory(TextFieldTableCell.forTableColumn());
            cRefs.setOnEditCommit(e -> { e.getRowValue().setAttendedEventReferences(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cRemarks != null) {
            cRemarks.setCellFactory(TextFieldTableCell.forTableColumn());
            cRemarks.setOnEditCommit(e -> { e.getRowValue().setWorkingRemarks(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cGroup != null) {
            cGroup.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            cGroup.setOnEditCommit(e -> { e.getRowValue().setGroup(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cType != null) {
            cType.setCellFactory(TextFieldTableCell.forTableColumn());
            cType.setOnEditCommit(e -> { e.getRowValue().setType(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cAac != null) {
            cAac.setCellFactory(TextFieldTableCell.forTableColumn());
            cAac.setOnEditCommit(e -> { e.getRowValue().setAac(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cCfs != null) {
            cCfs.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            cCfs.setOnEditCommit(e -> { e.getRowValue().setCfs(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cRf != null) {
            cRf.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            cRf.setOnEditCommit(e -> { e.getRowValue().setSocialRiskFactor(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
    }

    @FXML
    private void onGenerate() {
        GeneratePatientsDialog dlg = new GeneratePatientsDialog();
        Optional<GeneratePatientsDialog.Config> res = dlg.showAndWait();
        if (res.isEmpty()) return;
        GeneratePatientsDialog.Config cfg = res.get();

        List<Patient> newOnes = new ArrayList<>();
        // Fixed age buckets
        for (var e : cfg.fixedAges.entrySet()) {
            int age = e.getKey();
            int count = e.getValue();
            for (int i = 0; i < count; i++) newOnes.add(newPatientWith(RandomDataUtil.dobForExactAge(age), cfg.nricMode));
        }
        // Random-age bucket in given range
        for (int i = 0; i < Math.max(0, cfg.randomCount); i++) {
            newOnes.add(newPatientWith(RandomDataUtil.randomDOBBetweenAges(cfg.randomMinAge, cfg.randomMaxAge), cfg.nricMode));
        }

        patients.addAll(newOnes);
        markGeneratedPatients(newOnes);
        com.aac.kpi.service.AppState.setDirty(true);
        updateStatus();
    }

    private Patient newPatientWith(String birthDate, com.aac.kpi.service.NricMode nricMode) {
        Patient p = new Patient();
        p.setPatientId(RandomDataUtil.uuid32());
        p.setPatientIdentifierValue(NRICGeneratorUtil.generateByMode(nricMode));
        p.setPatientBirthdate(birthDate);
        p.setPatientPostalCode(RandomDataUtil.randomPostal6());
        p.setWorkingRemarks("");
        p.setGroup(RandomDataUtil.randomGroup());
        p.setType(RandomDataUtil.randomType());
        p.setAac(RandomDataUtil.randomAAC());
        return p;
    }

    @FXML
    private void onUploadExcel() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File f = fc.showOpenDialog(table.getScene() != null ? table.getScene().getWindow() : null);
        if (f == null) return;
        try {
            List<Patient> list = ExcelReader.readPatients(f);
            patients.setAll(list);
            com.aac.kpi.service.AppState.setDirty(false);
            updateStatus();
            clearGeneratedPatientsHighlight();
        } catch (Exception ex) {
            showAlert("Failed to load: " + ex.getMessage());
        }
    }

    @FXML
    private void onAnalyze() {
        List<String> issues = ValidatorService.validatePatients(patients);
        StringBuilder msg = new StringBuilder();
        if (!issues.isEmpty()) {
            msg.append(String.join("\n", issues)).append("\n\n");
        }
        // Compute KPI classification for FY 2025-04-01..2026-03-31
        java.time.LocalDate fyStart = java.time.LocalDate.of(2025,4,1);
        java.time.LocalDate fyEnd = java.time.LocalDate.of(2026,3,31);
        com.aac.kpi.service.KpiService.computeForFY(patients, (java.util.List) sessions, fyStart, fyEnd);
        refreshTable();
        long robust = patients.stream().filter(p -> "Robust".equals(p.getKpiType())).count();
        long robustG2 = patients.stream().filter(p -> "Robust (Group 2 – CFS 6-9)".equals(p.getKpiType())).count();
        long frail = patients.stream().filter(p -> "Frail".equals(p.getKpiType())).count();
        long frailG2 = patients.stream().filter(p -> "Frail (Group 2 – Very Frail)".equals(p.getKpiType())).count();
        long buddy = patients.stream().filter(p -> p.getKpiType()!=null && p.getKpiType().startsWith("Buddying")).count();
        long bef = patients.stream().filter(p -> p.getKpiType()!=null && p.getKpiType().startsWith("Befriending")).count();
        msg.append(String.format("KPI: Robust=%d (G2=%d) | Frail=%d (G2=%d) | Buddy=%d | Bef=%d",
                robust, robustG2, frail, frailG2, buddy, bef));
        showInfo(msg.toString());
    }

    @FXML
    private void onExport() {
        try {
            java.io.File dest = com.aac.kpi.service.AppState.getCurrentExcelFile();
            if (dest == null) {
                // If no known file, default to chooser to make intent clear
                javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
                fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel", "*.xlsx"));
                fc.setInitialFileName("KPI_Data.xlsx");
                dest = fc.showSaveDialog(table.getScene().getWindow());
                if (dest == null) return;
            }
            com.aac.kpi.service.LinkService.fillPatientAttendedRefs(patients, sessions);
            File file = ExcelWriter.saveToExcel(patients, sessions, practitioners, dest);
            showInfo("Exported to: " + file.getAbsolutePath());
            statusLabel.setText(String.format("Generated %d patients | %d sessions | %d practitioners | Last export: %s",
                    patients.size(), sessions.size(), practitioners != null ? practitioners.size() : 0, ExcelWriter.nowStamp()));
            com.aac.kpi.service.AppState.setDirty(false);
        } catch (Exception ex) {
            showAlert("Export failed: " + ex.getMessage());
        }
    }

    private void updateStatus() {
        if (statusLabel != null) {
            int prc = practitioners != null ? practitioners.size() : 0;
            statusLabel.setText(String.format("Generated %d patients | %d sessions | %d practitioners", patients.size(), sessions.size(), prc));
        }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
    public void refreshTable() {
        if (table != null) table.refresh();
    }

    public void setClearAllHandler(Runnable handler) {
        this.clearAllHandler = handler;
    }

    private void markGeneratedPatients(Collection<Patient> newPatients) {
        if (newPatients == null || newPatients.isEmpty()) return;
        Patient first = newPatients.iterator().next();
        TableHighlightSupport.add(table, first, highlightedGeneratedPatients);
        AppState.addHighlightedPatientId(first.getPatientId());
    }

    private void clearGeneratedPatientsHighlight() {
        TableHighlightSupport.clear(table, highlightedGeneratedPatients);
        AppState.clearHighlightedPatientIds();
    }

    public void highlightIssues(Collection<Patient> issues) {
        TableHighlightSupport.replace(table, issues, issueHighlights);
    }

    @FXML
    private void onClearSheet() {
        patients.clear();
        clearGeneratedPatientsHighlight();
        AppState.setDirty(true);
        updateStatus();
        refreshTable();
    }

    @FXML
    private void onClearAll() {
        if (clearAllHandler != null) clearAllHandler.run();
    }

    private void installCopyHandler() {
        // Keyboard copy (Cmd/Ctrl+C)
        table.setOnKeyPressed(ev -> {
            if ((ev.isShortcutDown() && ev.getCode() == KeyCode.C)) {
                copySelectionToClipboard();
                ev.consume();
            }
        });
        // Context menu copy
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(e -> copySelectionToClipboard());
        ContextMenu menu = new ContextMenu(copyItem);
        table.setContextMenu(menu);
    }

    private void copySelectionToClipboard() {
        StringBuilder sb = new StringBuilder();
        var model = table.getSelectionModel();
        var cells = new ArrayList<>(model.getSelectedCells());
        cells.sort((p1, p2) -> {
            int c = Integer.compare(p1.getRow(), p2.getRow());
            if (c != 0) return c;
            return Integer.compare(p1.getColumn(), p2.getColumn());
        });
        int prevRow = -1;
        for (TablePosition<?, ?> pos : cells) {
            int row = pos.getRow();
            if (prevRow == row) sb.append('\t');
            else if (prevRow != -1) sb.append('\n');
            Object val = pos.getTableColumn().getCellData(row);
            sb.append(val == null ? "" : val.toString());
            prevRow = row;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }
}
