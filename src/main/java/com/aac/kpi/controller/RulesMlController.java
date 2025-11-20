package com.aac.kpi.controller;

import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.RulesConfig;
import com.aac.kpi.model.RuleGraph;
import com.aac.kpi.service.AppState;
import com.aac.kpi.service.KpiService;
import com.aac.kpi.service.RulesConfigService;
import com.aac.kpi.service.RuleGraphService;
import com.aac.kpi.service.RulesSuggestionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Non-coder friendly UI to edit KPI thresholds/columns, view ML-style suggestions,
 * and preview outcomes without altering the core rule engine.
 */
public class RulesMlController {
    @FXML private TextField robustMinField;
    @FXML private TextField frailMinField;
    @FXML private TextField buddyMinField;
    @FXML private TextField befMinField;
    @FXML private TextField befContactsField;
    @FXML private TextField screeningPurposeField;
    @FXML private CheckBox applyConfigCheckbox;

    @FXML private TableView<RulesConfig.PurposeRule> purposeTable;
    @FXML private TableColumn<RulesConfig.PurposeRule, String> cLabel;
    @FXML private TableColumn<RulesConfig.PurposeRule, String> cPurpose;
    @FXML private TableColumn<RulesConfig.PurposeRule, String> cMode;
    @FXML private TableColumn<RulesConfig.PurposeRule, Integer> cMinCount;
    @FXML private TableColumn<RulesConfig.PurposeRule, String> cDescription;

    @FXML private TableView<RulesConfig.ColumnSpec> columnTable;
    @FXML private TableColumn<RulesConfig.ColumnSpec, String> colSheet;
    @FXML private TableColumn<RulesConfig.ColumnSpec, String> colColumn;
    @FXML private TableColumn<RulesConfig.ColumnSpec, String> colSourceSheet;
    @FXML private TableColumn<RulesConfig.ColumnSpec, String> colSourceColumn;
    @FXML private TableColumn<RulesConfig.ColumnSpec, String> colRequired;
    @FXML private TableColumn<RulesConfig.ColumnSpec, String> colDefault;
    @FXML private TableColumn<RulesConfig.ColumnSpec, String> colNotes;

    @FXML private TextArea previewArea;
    @FXML private TextArea suggestionsArea;
    // Rule graph UI
    @FXML private TextField ruleSearchField;
    @FXML private ListView<String> sheetListView;
    @FXML private TableView<RuleGraph.ColumnRule> ruleCardTable;
    @FXML private TableColumn<RuleGraph.ColumnRule, String> rcColumn;
    @FXML private TableColumn<RuleGraph.ColumnRule, String> rcDataType;
    @FXML private TableColumn<RuleGraph.ColumnRule, String> rcFormat;
    @FXML private TableColumn<RuleGraph.ColumnRule, String> rcLogic;
    @FXML private TableColumn<RuleGraph.ColumnRule, String> rcSourceSheet;
    @FXML private TableColumn<RuleGraph.ColumnRule, String> rcSourceColumn;
    @FXML private TableColumn<RuleGraph.ColumnRule, String> rcTransform;
    @FXML private TableColumn<RuleGraph.ColumnRule, String> rcPreview;
    @FXML private TextArea rulePreviewArea;
    @FXML private Canvas flowCanvas;
    @FXML private ListView<String> scenarioOverridesList;

    private ObservableList<RulesConfig.PurposeRule> purposeRules = FXCollections.observableArrayList();
    private ObservableList<RulesConfig.ColumnSpec> columnSpecs = FXCollections.observableArrayList();
    private ObservableList<RuleGraph.ColumnRule> ruleColumns = FXCollections.observableArrayList();
    private FilteredList<RuleGraph.ColumnRule> filteredRuleColumns = new FilteredList<>(ruleColumns, r -> true);
    private RuleGraph ruleGraph = RuleGraph.defaults();
    private ObservableList<Patient> patients;
    private ObservableList<EventSession> sessions;
    private File configFile;
    private final LocalDate fyStart = LocalDate.of(2025, 4, 1);
    private final LocalDate fyEnd = LocalDate.of(2026, 3, 31);

    public void init(ObservableList<Patient> patients,
                     ObservableList<EventSession> sessions) {
        this.patients = patients;
        this.sessions = sessions;
        configFile = RulesConfigService.defaultFile();
        setupTables();
        setupRuleGraphUi();
        loadConfig(RulesConfigService.ensureFile());
        loadRuleGraph(RuleGraphService.ensureFile());
    }

    private void setupTables() {
        if (purposeTable != null) {
            purposeTable.setEditable(true);
            cLabel.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    safe(cell.getValue().label)));
            cLabel.setCellFactory(TextFieldTableCell.forTableColumn());
            cLabel.setOnEditCommit(e -> e.getRowValue().label = e.getNewValue());

            cPurpose.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    safe(cell.getValue().purpose)));
            cPurpose.setCellFactory(TextFieldTableCell.forTableColumn());
            cPurpose.setOnEditCommit(e -> e.getRowValue().purpose = e.getNewValue());

            cMode.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    safe(cell.getValue().mode)));
            cMode.setCellFactory(TextFieldTableCell.forTableColumn());
            cMode.setOnEditCommit(e -> e.getRowValue().mode = e.getNewValue());

            cMinCount.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(
                    cell.getValue().minCount).asObject());
            cMinCount.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            cMinCount.setOnEditCommit(e -> e.getRowValue().minCount = e.getNewValue());

            cDescription.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    safe(cell.getValue().description)));
            cDescription.setCellFactory(TextFieldTableCell.forTableColumn());
            cDescription.setOnEditCommit(e -> e.getRowValue().description = e.getNewValue());

            purposeTable.setItems(purposeRules);
        }

        if (columnTable != null) {
            columnTable.setEditable(true);
            colSheet.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    safe(cell.getValue().sheet)));
            colSheet.setCellFactory(TextFieldTableCell.forTableColumn());
            colSheet.setOnEditCommit(e -> e.getRowValue().sheet = e.getNewValue());

            colColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    safe(cell.getValue().column)));
            colColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            colColumn.setOnEditCommit(e -> e.getRowValue().column = e.getNewValue());

            colSourceSheet.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    safe(cell.getValue().sourceSheet)));
            colSourceSheet.setCellFactory(TextFieldTableCell.forTableColumn());
            colSourceSheet.setOnEditCommit(e -> e.getRowValue().sourceSheet = e.getNewValue());

            colSourceColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    safe(cell.getValue().sourceColumn)));
            colSourceColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            colSourceColumn.setOnEditCommit(e -> e.getRowValue().sourceColumn = e.getNewValue());

            colRequired.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    safe(cell.getValue().required)));
            colRequired.setCellFactory(TextFieldTableCell.forTableColumn());
            colRequired.setOnEditCommit(e -> e.getRowValue().required = e.getNewValue());

            colDefault.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    safe(cell.getValue().defaultValue)));
            colDefault.setCellFactory(TextFieldTableCell.forTableColumn());
            colDefault.setOnEditCommit(e -> e.getRowValue().defaultValue = e.getNewValue());

            colNotes.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                    safe(cell.getValue().notes)));
            colNotes.setCellFactory(TextFieldTableCell.forTableColumn());
            colNotes.setOnEditCommit(e -> e.getRowValue().notes = e.getNewValue());

            columnTable.setItems(columnSpecs);
        }
    }

    private void setupRuleGraphUi() {
        if (ruleCardTable != null) {
            ruleCardTable.setEditable(true);
            rcColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(safe(cell.getValue().name)));
            rcColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            rcColumn.setOnEditCommit(e -> e.getRowValue().name = e.getNewValue());

            rcDataType.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(safe(cell.getValue().dataType)));
            rcDataType.setCellFactory(TextFieldTableCell.forTableColumn());
            rcDataType.setOnEditCommit(e -> e.getRowValue().dataType = e.getNewValue());

            rcFormat.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(safe(cell.getValue().format)));
            rcFormat.setCellFactory(TextFieldTableCell.forTableColumn());
            rcFormat.setOnEditCommit(e -> e.getRowValue().format = e.getNewValue());

            rcLogic.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(safe(cell.getValue().logicType)));
            rcLogic.setCellFactory(TextFieldTableCell.forTableColumn());
            rcLogic.setOnEditCommit(e -> e.getRowValue().logicType = e.getNewValue());

            rcSourceSheet.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(safe(cell.getValue().sourceSheet)));
            rcSourceSheet.setCellFactory(TextFieldTableCell.forTableColumn());
            rcSourceSheet.setOnEditCommit(e -> e.getRowValue().sourceSheet = e.getNewValue());

            rcSourceColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(safe(cell.getValue().sourceColumn)));
            rcSourceColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            rcSourceColumn.setOnEditCommit(e -> e.getRowValue().sourceColumn = e.getNewValue());

            rcTransform.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(safe(cell.getValue().transform)));
            rcTransform.setCellFactory(TextFieldTableCell.forTableColumn());
            rcTransform.setOnEditCommit(e -> e.getRowValue().transform = e.getNewValue());

            rcPreview.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(safe(cell.getValue().previewPattern)));
            rcPreview.setCellFactory(TextFieldTableCell.forTableColumn());
            rcPreview.setOnEditCommit(e -> e.getRowValue().previewPattern = e.getNewValue());

            ruleCardTable.setItems(filteredRuleColumns);
        }

        if (sheetListView != null) {
            sheetListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                selectSheet(newV);
            });
        }

        if (ruleSearchField != null) {
            ruleSearchField.textProperty().addListener((obs, oldV, newV) -> {
                String q = newV == null ? "" : newV.toLowerCase();
                filteredRuleColumns.setPredicate(rule -> {
                    if (rule == null) return false;
                    if (q.isBlank()) return true;
                    return safe(rule.name).toLowerCase().contains(q)
                            || safe(rule.sourceSheet).toLowerCase().contains(q)
                            || safe(rule.sourceColumn).toLowerCase().contains(q);
                });
            });
        }
    }

    @FXML
    private void onLoadConfig() {
        RulesConfig cfg = RulesConfigService.ensureFile();
        loadConfig(cfg);
        showInfo("Config loaded from " + configFile.getPath());
    }

    @FXML
    private void onLoadRuleGraph() {
        loadRuleGraph(RuleGraphService.ensureFile());
        showInfo("Rule graph loaded from " + RuleGraphService.defaultFile().getPath());
    }

    @FXML
    private void onSaveRuleGraph() {
        try {
            RuleGraphService.save(ruleGraph);
            showInfo("Rule graph saved to " + RuleGraphService.defaultFile().getPath());
        } catch (Exception ex) {
            showError("Failed to save rule graph: " + ex.getMessage());
        }
    }

    @FXML
    private void onResetRuleGraph() {
        loadRuleGraph(RuleGraph.defaults());
    }

    @FXML
    private void onSaveConfig() {
        try {
            RulesConfig cfg = collectConfigFromUi();
            cfg.applyToGeneration = applyConfigCheckbox.isSelected();
            RulesConfigService.save(cfg, configFile);
            AppState.setRulesConfig(cfg);
            showInfo("Config saved to " + configFile.getPath());
        } catch (Exception ex) {
            showError("Failed to save config: " + ex.getMessage());
        }
    }

    @FXML
    private void onResetDefaults() {
        loadConfig(RulesConfig.defaults());
    }

    @FXML
    private void onAddPurposeRule() {
        purposeRules.add(new RulesConfig.PurposeRule("New Rule", "purpose", "In-person", 1, ""));
    }

    @FXML
    private void onRemovePurposeRule() {
        RulesConfig.PurposeRule selected = purposeTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            purposeRules.remove(selected);
        }
    }

    @FXML
    private void onAddColumnSpec() {
        columnSpecs.add(new RulesConfig.ColumnSpec("Sheet", "column_name", "", "", "yes", "", ""));
    }

    @FXML
    private void onRemoveColumnSpec() {
        RulesConfig.ColumnSpec selected = columnTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            columnSpecs.remove(selected);
        }
    }

    @FXML
    private void onRefreshPreview() {
        RulesConfig cfg = collectConfigFromUi();
        StringBuilder sb = new StringBuilder();
        sb.append("Built-in thresholds: ")
                .append(AppState.getKpiConfig().robustMinInPerson).append("/").append(AppState.getKpiConfig().frailMinInPerson)
                .append(" (Robust/Frail in-person), buddy ").append(AppState.getKpiConfig().buddyingMinInPerson)
                .append(", bef ").append(AppState.getKpiConfig().befriendingMinInPerson).append("\n");
        sb.append("UI thresholds: ").append(cfg.thresholds.robustMinInPerson).append("/")
                .append(cfg.thresholds.frailMinInPerson).append(", buddy ")
                .append(cfg.thresholds.buddyingMinInPerson).append(", bef ")
                .append(cfg.thresholds.befriendingMinInPerson).append(" (contacts ").append(cfg.thresholds.befriendingMinContacts)
                .append(")\n\n");

        if (patients == null || patients.isEmpty()) {
            sb.append("No loaded patients; preview shows thresholds only.");
            previewArea.setText(sb.toString());
            return;
        }
        try {
            PreviewCounts base = simulateCounts(AppState.getKpiConfig());
            PreviewCounts ui = simulateCounts(cfg.toKpiConfig());
            sb.append("FY Window: ").append(fyStart).append(" to ").append(fyEnd).append("\n");
            sb.append("Built-in classification -> ").append(base.summary).append("\n");
            sb.append("UI config classification -> ").append(ui.summary).append("\n");
        } catch (Exception ex) {
            sb.append("Preview error: ").append(ex.getMessage());
        }
        previewArea.setText(sb.toString());
    }

    @FXML
    private void onRefreshSuggestions() {
        RulesConfig cfg = collectConfigFromUi();
        List<RulesSuggestionService.Suggestion> list = RulesSuggestionService.suggest(
                sessions != null ? sessions : List.of(),
                patients != null ? patients : List.of(),
                cfg, fyStart, fyEnd);
        if (list.isEmpty()) {
            suggestionsArea.setText("No suggestions at the moment.");
            return;
        }
        String out = list.stream()
                .map(s -> String.format("[%.0f%%] %s (event %s)", s.confidence * 100, s.message,
                        s.compositionId == null ? "n/a" : s.compositionId))
                .collect(Collectors.joining("\n"));
        suggestionsArea.setText(out);
    }

    private void loadRuleGraph(RuleGraph g) {
        if (g == null) g = RuleGraph.defaults();
        this.ruleGraph = g;
        ruleColumns.setAll(collectAllColumnRules(g));
        // Populate sheet list
        if (sheetListView != null) {
            sheetListView.getItems().setAll(
                    g.sheets == null ? List.of() : g.sheets.stream().map(s -> safe(s.name)).toList());
            if (!sheetListView.getItems().isEmpty()) {
                sheetListView.getSelectionModel().select(0);
            }
        }
        updateRulePreviewArea();
        updateScenarioOverridesView();
        drawFlowDiagram();
    }

    private void selectSheet(String name) {
        if (ruleGraph == null || ruleGraph.sheets == null) return;
        for (RuleGraph.SheetRule s : ruleGraph.sheets) {
            if (safe(s.name).equals(name)) {
                ruleColumns.setAll(s.columns == null ? List.of() : s.columns);
                updateRulePreviewArea();
                return;
            }
        }
        ruleColumns.clear();
        updateRulePreviewArea();
    }

    private List<RuleGraph.ColumnRule> collectAllColumnRules(RuleGraph g) {
        List<RuleGraph.ColumnRule> cols = new ArrayList<>();
        if (g != null && g.sheets != null) {
            for (RuleGraph.SheetRule s : g.sheets) {
                if (s.columns != null) cols.addAll(s.columns);
            }
        }
        return cols;
    }

    private void updateRulePreviewArea() {
        if (rulePreviewArea == null) return;
        StringBuilder sb = new StringBuilder();
        String sheet = sheetListView != null ? sheetListView.getSelectionModel().getSelectedItem() : "";
        sb.append("Preview (patterns only) for sheet: ").append(sheet == null ? "" : sheet).append("\n");
        for (RuleGraph.ColumnRule c : filteredRuleColumns) {
            sb.append("- ").append(c.name)
                    .append(" [").append(c.dataType).append(", ").append(c.format).append(", ").append(c.logicType).append("]")
                    .append(" src: ").append(safe(c.sourceSheet)).append(".").append(safe(c.sourceColumn))
                    .append(" → ").append(safe(c.previewPattern))
                    .append("\n");
        }
        rulePreviewArea.setText(sb.toString().trim());
    }

    private void updateScenarioOverridesView() {
        if (scenarioOverridesList == null) return;
        List<String> overrides = new ArrayList<>();
        if (ruleGraph != null && ruleGraph.scenarios != null) {
            for (RuleGraph.ScenarioRule s : ruleGraph.scenarios) {
                for (RuleGraph.ScenarioOverride ov : s.overrides) {
                    overrides.add(s.name + " → " + ov.toString());
                }
            }
        }
        scenarioOverridesList.getItems().setAll(overrides);
    }

    private void drawFlowDiagram() {
        if (flowCanvas == null) return;
        GraphicsContext gc = flowCanvas.getGraphicsContext2D();
        double w = flowCanvas.getWidth();
        double h = flowCanvas.getHeight();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, w, h);
        gc.setStroke(Color.web("#455a64"));
        gc.setLineWidth(1.4);
        // Simple layout of nodes
        DiagramNode[] nodes = new DiagramNode[] {
                new DiagramNode("Patient", 100, 80),
                new DiagramNode("Event", 280, 80),
                new DiagramNode("Encounter", 460, 80),
                new DiagramNode("Questionnaire", 640, 80),
                new DiagramNode("Practitioner", 820, 80),
                new DiagramNode("Common", 460, 200),
                new DiagramNode("AAC/Org/Location", 280, 200)
        };
        int[][] edges = {
                {0,1}, {0,2}, {0,3}, {1,5}, {2,5}, {3,5}, {4,5}, {6,1}
        };
        for (int[] e : edges) {
            DiagramNode a = nodes[e[0]];
            DiagramNode b = nodes[e[1]];
            gc.strokeLine(a.x, a.y, b.x, b.y);
        }
        gc.setFill(Color.web("#e8f0fe"));
        gc.setStroke(Color.web("#1e88e5"));
        gc.setLineWidth(1.2);
        for (DiagramNode n : nodes) {
            double bw = 120, bh = 36;
            double x0 = n.x - bw/2, y0 = n.y - bh/2;
            gc.fillRoundRect(x0, y0, bw, bh, 10, 10);
            gc.strokeRoundRect(x0, y0, bw, bh, 10, 10);
            gc.setFill(Color.web("#1f2933"));
            gc.fillText(n.label, n.x - 40, n.y + 4);
            gc.setFill(Color.web("#e8f0fe"));
        }
    }

    private void loadConfig(RulesConfig cfg) {
        if (cfg == null) cfg = RulesConfig.defaults();
        robustMinField.setText(String.valueOf(cfg.thresholds.robustMinInPerson));
        frailMinField.setText(String.valueOf(cfg.thresholds.frailMinInPerson));
        buddyMinField.setText(String.valueOf(cfg.thresholds.buddyingMinInPerson));
        befMinField.setText(String.valueOf(cfg.thresholds.befriendingMinInPerson));
        befContactsField.setText(String.valueOf(cfg.thresholds.befriendingMinContacts));
        screeningPurposeField.setText(cfg.screening.purpose);
        applyConfigCheckbox.setSelected(cfg.applyToGeneration);

        purposeRules.setAll(cfg.purposes == null ? List.of() : cfg.purposes);
        columnSpecs.setAll(cfg.columns == null ? List.of() : cfg.columns);
    }

    private RulesConfig collectConfigFromUi() {
        RulesConfig cfg = new RulesConfig();
        cfg.thresholds.robustMinInPerson = parseInt(robustMinField, 2);
        cfg.thresholds.frailMinInPerson = parseInt(frailMinField, 6);
        cfg.thresholds.buddyingMinInPerson = parseInt(buddyMinField, 6);
        cfg.thresholds.befriendingMinInPerson = parseInt(befMinField, 12);
        cfg.thresholds.befriendingMinContacts = parseInt(befContactsField, 52);
        cfg.screening.purpose = screeningPurposeField.getText();
        cfg.purposes = new ArrayList<>(purposeRules);
        cfg.columns = new ArrayList<>(columnSpecs);
        cfg.applyToGeneration = applyConfigCheckbox.isSelected();
        return cfg;
    }

    private PreviewCounts simulateCounts(com.aac.kpi.service.KpiConfig config) {
        List<Patient> previewPatients = copyPatients(patients);
        List<EventSession> previewSessions = copySessions(sessions);
        KpiService.computeForFY(previewPatients, previewSessions, fyStart, fyEnd, config);
        long robust = previewPatients.stream().filter(p -> "Robust".equals(p.getKpiType())).count();
        long robustG2 = previewPatients.stream().filter(p -> p.getKpiType() != null && p.getKpiType().contains("CFS 6-9")).count();
        long frail = previewPatients.stream().filter(p -> "Frail".equals(p.getKpiType())).count();
        long frailG2 = previewPatients.stream().filter(p -> p.getKpiType() != null && p.getKpiType().contains("Very Frail")).count();
        long buddy = previewPatients.stream().filter(p -> p.getKpiType() != null && p.getKpiType().startsWith("Buddying")).count();
        long bef = previewPatients.stream().filter(p -> p.getKpiType() != null && p.getKpiType().startsWith("Befriending")).count();
        String summary = String.format("Robust=%d(G2=%d) Frail=%d(G2=%d) Buddy=%d Bef=%d",
                robust, robustG2, frail, frailG2, buddy, bef);
        return new PreviewCounts(summary);
    }

    private List<Patient> copyPatients(List<Patient> src) {
        List<Patient> out = new ArrayList<>();
        if (src == null) return out;
        for (Patient p : src) {
            Patient c = new Patient();
            c.setPatientId(p.getPatientId());
            c.setCfs(p.getCfs());
            c.setSocialRiskFactor(p.getSocialRiskFactor());
            c.setBuddyingProgramStartDate(p.getBuddyingProgramStartDate());
            c.setBuddyingProgramEndDate(p.getBuddyingProgramEndDate());
            c.setBefriendingProgramStartDate(p.getBefriendingProgramStartDate());
            c.setBefriendingProgramEndDate(p.getBefriendingProgramEndDate());
            c.setKpiType(p.getKpiType());
            c.setKpiGroup(p.getKpiGroup());
            out.add(c);
        }
        return out;
    }

    private List<EventSession> copySessions(List<EventSession> src) {
        List<EventSession> out = new ArrayList<>();
        if (src == null) return out;
        for (EventSession s : src) {
            EventSession c = new EventSession();
            c.setCompositionId(s.getCompositionId());
            c.setEventSessionMode1(s.getEventSessionMode1());
            c.setEventSessionStartDate1(s.getEventSessionStartDate1());
            c.setEventSessionEndDate1(s.getEventSessionEndDate1());
            c.setEventSessionPatientReferences1(s.getEventSessionPatientReferences1());
            c.setPurposeOfContact(s.getPurposeOfContact());
            c.setAttendedIndicator(s.isAttendedIndicator());
            out.add(c);
        }
        return out;
    }

    private int parseInt(TextField field, int fallback) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private record DiagramNode(String label, double x, double y) {}

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private record PreviewCounts(String summary) {}
}
