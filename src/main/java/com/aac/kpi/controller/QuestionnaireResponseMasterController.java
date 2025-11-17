package com.aac.kpi.controller;

import com.aac.kpi.model.*;
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
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.*;

public class QuestionnaireResponseMasterController {
    private ObservableList<QuestionnaireResponse> questionnaires;
    private ObservableList<Patient> patients;
    private ObservableList<EventSession> sessions;
    private ObservableList<Practitioner> practitioners;
    private ObservableList<Encounter> encounters;
    private Label statusLabel;
    private Runnable clearAllHandler;
    private final Set<QuestionnaireResponse> highlightedGeneratedQuestionnaires = new HashSet<>();

    @FXML private TableView<QuestionnaireResponse> table;
    @FXML private TableColumn<QuestionnaireResponse, String> cId;
    @FXML private TableColumn<QuestionnaireResponse, String> cStatus;
    @FXML private TableColumn<QuestionnaireResponse, String> cQ1;
    @FXML private TableColumn<QuestionnaireResponse, String> cQ2;
    @FXML private TableColumn<QuestionnaireResponse, String> cQ3;
    @FXML private TableColumn<QuestionnaireResponse, String> cQ4;
    @FXML private TableColumn<QuestionnaireResponse, String> cQ5;
    @FXML private TableColumn<QuestionnaireResponse, String> cQ6;
    @FXML private TableColumn<QuestionnaireResponse, String> cQ7;
    @FXML private TableColumn<QuestionnaireResponse, String> cQ8;
    @FXML private TableColumn<QuestionnaireResponse, String> cQ9;
    @FXML private TableColumn<QuestionnaireResponse, String> cQ10;
    @FXML private TableColumn<QuestionnaireResponse, String> cPatientRef;

    public void init(ObservableList<QuestionnaireResponse> questionnaires,
                     ObservableList<Patient> patients,
                     ObservableList<EventSession> sessions,
                     ObservableList<Practitioner> practitioners,
                     ObservableList<Encounter> encounters,
                     Label statusLabel) {
        this.questionnaires = questionnaires;
        this.patients = patients;
        this.sessions = sessions;
        this.practitioners = practitioners;
        this.encounters = encounters;
        this.statusLabel = statusLabel;
        table.setItems(this.questionnaires);
        table.setEditable(true);
        TableHighlightSupport.install(table, highlightedGeneratedQuestionnaires);
    }

    @FXML private void initialize() {
        if (cId != null) cId.setCellValueFactory(new PropertyValueFactory<>("questionnaireId"));
        if (cStatus != null) cStatus.setCellValueFactory(new PropertyValueFactory<>("questionnaireStatus"));
        if (cQ1 != null) cQ1.setCellValueFactory(new PropertyValueFactory<>("q1"));
        if (cQ2 != null) cQ2.setCellValueFactory(new PropertyValueFactory<>("q2"));
        if (cQ3 != null) cQ3.setCellValueFactory(new PropertyValueFactory<>("q3"));
        if (cQ4 != null) cQ4.setCellValueFactory(new PropertyValueFactory<>("q4"));
        if (cQ5 != null) cQ5.setCellValueFactory(new PropertyValueFactory<>("q5"));
        if (cQ6 != null) cQ6.setCellValueFactory(new PropertyValueFactory<>("q6"));
        if (cQ7 != null) cQ7.setCellValueFactory(new PropertyValueFactory<>("q7"));
        if (cQ8 != null) cQ8.setCellValueFactory(new PropertyValueFactory<>("q8"));
        if (cQ9 != null) cQ9.setCellValueFactory(new PropertyValueFactory<>("q9"));
        if (cQ10 != null) cQ10.setCellValueFactory(new PropertyValueFactory<>("q10"));
        if (cPatientRef != null) cPatientRef.setCellValueFactory(new PropertyValueFactory<>("questionnairePatientReference"));

        // Editable
        for (TableColumn<QuestionnaireResponse, String> col : List.of(cId,cStatus,cQ1,cQ2,cQ3,cQ4,cQ5,cQ6,cQ7,cQ8,cQ9,cQ10,cPatientRef)) {
            if (col != null) col.setCellFactory(TextFieldTableCell.forTableColumn());
        }
    }

    @FXML private void onGenerate() {
        GenerateQuestionnairesDialog dlg = new GenerateQuestionnairesDialog();
        var res = dlg.showAndWait();
        if (res.isEmpty()) return;
        var cfg = res.get();
        List<QuestionnaireResponse> list = new ArrayList<>();
        for (int i = 0; i < cfg.total; i++) {
            list.add(randomQuestionnaire(i, cfg));
        }
        questionnaires.addAll(list);
        markGeneratedQuestionnaires(list);
        updateStatus();
    }

    private QuestionnaireResponse randomQuestionnaire(int index, GenerateQuestionnairesDialog.Config cfg) {
        // Even-indexed rows use slightly shifted window
        java.util.Random r = new java.util.Random();
        int shift = (index % 2 == 0) ? (r.nextInt(61) - 30) : 0; // -30..+30 days
        LocalDate s = cfg.start.plusDays(shift);
        LocalDate e = cfg.end.plusDays(shift);
        if (s.isAfter(e)) { LocalDate tmp = s; s = e; e = tmp; }

        QuestionnaireResponse qr = new QuestionnaireResponse();
        qr.setQuestionnaireId(RandomDataUtil.uuid32().toUpperCase(java.util.Locale.ROOT));
        qr.setQuestionnaireStatus("completed");

        // Alternate date (yyyy-MM-dd) and rating (1-5)
        qr.setQ1(RandomDataUtil.randomDateBetween(s, e));
        qr.setQ2(String.valueOf(RandomDataUtil.randomInt(cfg.numericMin, cfg.numericMax)));
        qr.setQ3(RandomDataUtil.randomDateBetween(s, e));
        qr.setQ4(String.valueOf(RandomDataUtil.randomInt(cfg.numericMin, cfg.numericMax)));
        qr.setQ5(RandomDataUtil.randomDateBetween(s, e));
        qr.setQ6(String.valueOf(RandomDataUtil.randomInt(cfg.numericMin, cfg.numericMax)));
        qr.setQ7(RandomDataUtil.randomDateBetween(s, e));
        qr.setQ8(String.valueOf(RandomDataUtil.randomInt(cfg.numericMin, cfg.numericMax)));
        qr.setQ9(RandomDataUtil.randomDateBetween(s, e));
        qr.setQ10(String.valueOf(RandomDataUtil.randomInt(cfg.numericMin, cfg.numericMax)));
        // Link questionnaire to a patient (round-robin)
        if (patients != null && !patients.isEmpty()) {
            String pid = patients.get(index % patients.size()).getPatientId();
            qr.setQuestionnairePatientReference(pid);
        }
        return qr;
    }

    @FXML private void onUploadExcel() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File f = fc.showOpenDialog(table.getScene() != null ? table.getScene().getWindow() : null);
        if (f == null) return;
        try {
            List<QuestionnaireResponse> list = ExcelReader.readQuestionnaires(f);
            if (!list.isEmpty()) questionnaires.setAll(list);
            updateStatus();
            clearGeneratedQuestionnairesHighlight();
        } catch (Exception ex) { showAlert("Failed to load questionnaires: " + ex.getMessage()); }
    }

    @FXML private void onAnalyze() {
        StringBuilder sb = new StringBuilder();
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < questionnaires.size(); i++) {
            var q = questionnaires.get(i);
            if (q.getQuestionnaireId()==null || !q.getQuestionnaireId().matches("[A-Za-z0-9]{32}")) sb.append("Row ").append(i+1).append(": Invalid questionnaire_id\n");
            if (!"completed".equalsIgnoreCase(q.getQuestionnaireStatus())) sb.append("Row ").append(i+1).append(": status not 'completed'\n");
            if (!ids.add(q.getQuestionnaireId())) sb.append("Row ").append(i+1).append(": duplicate id\n");
            // Validate pattern: odd q = date yyyy-MM-dd; even q = 1..5
            if (!q.getQ1().matches("\\d{4}-\\d{2}-\\d{2}")) sb.append("Row ").append(i+1).append(": q1 not date\n");
            if (!q.getQ2().matches("[1-5]")) sb.append("Row ").append(i+1).append(": q2 not 1-5\n");
            if (!q.getQ3().matches("\\d{4}-\\d{2}-\\d{2}")) sb.append("Row ").append(i+1).append(": q3 not date\n");
            if (!q.getQ4().matches("[1-5]")) sb.append("Row ").append(i+1).append(": q4 not 1-5\n");
            if (!q.getQ5().matches("\\d{4}-\\d{2}-\\d{2}")) sb.append("Row ").append(i+1).append(": q5 not date\n");
            if (!q.getQ6().matches("[1-5]")) sb.append("Row ").append(i+1).append(": q6 not 1-5\n");
            if (!q.getQ7().matches("\\d{4}-\\d{2}-\\d{2}")) sb.append("Row ").append(i+1).append(": q7 not date\n");
            if (!q.getQ8().matches("[1-5]")) sb.append("Row ").append(i+1).append(": q8 not 1-5\n");
            if (!q.getQ9().matches("\\d{4}-\\d{2}-\\d{2}")) sb.append("Row ").append(i+1).append(": q9 not date\n");
            if (!q.getQ10().matches("[1-5]")) sb.append("Row ").append(i+1).append(": q10 not 1-5\n");
        }
        if (sb.length()==0) showInfo("QuestionnaireResponse Master OK"); else showInfo(sb.toString());
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
            File file = ExcelWriter.saveToExcel(patients, sessions, practitioners, encounters, questionnaires, java.util.List.of(), dest);
            showInfo("Exported to: " + file.getAbsolutePath());
            statusLabel.setText(String.format("Generated %d patients | %d sessions | %d practitioners | %d encounters | %d questionnaires | Last export: %s",
                    patients.size(), sessions.size(), practitioners.size(), encounters.size(), questionnaires.size(), ExcelWriter.nowStamp()));
            com.aac.kpi.service.AppState.setDirty(false);
        } catch (Exception ex) { showAlert("Export failed: " + ex.getMessage()); }
    }

    public void refreshTable() { if (table != null) table.refresh(); }

    private void updateStatus() {
        if (statusLabel != null) {
            statusLabel.setText(String.format("Generated %d patients | %d sessions | %d practitioners | %d encounters | %d questionnaires",
                    patients.size(), sessions.size(), practitioners.size(), encounters.size(), questionnaires.size()));
        }
    }

    public void setClearAllHandler(Runnable handler) {
        this.clearAllHandler = handler;
    }

    private void markGeneratedQuestionnaires(Collection<QuestionnaireResponse> newItems) {
        if (newItems == null || newItems.isEmpty()) return;
        QuestionnaireResponse first = newItems.iterator().next();
        TableHighlightSupport.add(table, first, highlightedGeneratedQuestionnaires);
        AppState.addHighlightedQuestionnaireId(first.getQuestionnaireId());
    }

    private void clearGeneratedQuestionnairesHighlight() {
        TableHighlightSupport.clear(table, highlightedGeneratedQuestionnaires);
        AppState.clearHighlightedQuestionnaireIds();
    }

    @FXML
    private void onClearSheet() {
        questionnaires.clear();
        clearGeneratedQuestionnairesHighlight();
        AppState.setDirty(true);
        updateStatus();
        refreshTable();
    }

    @FXML
    private void onClearAll() {
        if (clearAllHandler != null) clearAllHandler.run();
    }

    private void showAlert(String msg) { Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK); a.setHeaderText(null); a.showAndWait(); }
    private void showInfo(String msg) { Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK); a.setHeaderText(null); a.showAndWait(); }
}
