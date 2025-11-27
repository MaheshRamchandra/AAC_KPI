package com.aac.kpi.controller;

import com.aac.kpi.model.CommonRow;
import com.aac.kpi.model.Encounter;
import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.QuestionnaireResponse;
import com.aac.kpi.model.ScenarioTestCase;
import com.aac.kpi.service.RagRuleService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RAG-powered rule builder UI.
 * - Left: sheets
 * - Middle: rule cards and mapping editor
 * - Right: explainability + live preview
 * - Bottom: dependency graph and trace
 */
public class AiOverlayController {
    @FXML private ComboBox<String> scenarioBox;
    @FXML private ListView<String> sheetList;
    @FXML private TableView<RuleCardRow> ruleTable;
    @FXML private TableColumn<RuleCardRow, String> colName;
    @FXML private TableColumn<RuleCardRow, String> colType;
    @FXML private TableColumn<RuleCardRow, String> colFormat;
    @FXML private TableColumn<RuleCardRow, String> colLogic;
    @FXML private TableColumn<RuleCardRow, String> colMapping;
    @FXML private TableColumn<RuleCardRow, String> colPattern;
    @FXML private TextArea promptArea;
    @FXML private TextArea proposalArea;
    @FXML private TextArea answerArea;
    @FXML private TextArea validationsArea;
    @FXML private TextArea planArea;
    @FXML private TextArea toolsArea;
    @FXML private ListView<String> dependenciesList;
    @FXML private ListView<String> previewList;
    @FXML private TextField sourceSheetField;
    @FXML private TextField sourceColumnField;
    @FXML private TextField logicField;
    @FXML private TextField formatField;
    @FXML private TextField dataTypeField;
    @FXML private TextField patternField;
    @FXML private TextField transformField;
    @FXML private Canvas dependencyCanvas;
    @FXML private CheckBox showColumnsToggle;
    @FXML private CheckBox showEdgeLabelsToggle;
    @FXML private CheckBox highlightSelectedToggle;
    @FXML private Slider zoomSlider;
    @FXML private Label zoomLabel;
    @FXML private Label statusLabel;
    @FXML private Label legendLabel;
    @FXML private Button runButton;
    @FXML private Button applyButton;
    @FXML private Button openFullScreenButton;
    @FXML private Button applyFilterButton;
    @FXML private Button selectAllFilterButton;
    @FXML private Button copyMermaidButton;
    @FXML private ListView<String> filterList;

    private RagRuleService ragService;
    private final ObservableList<RuleCardRow> rows = FXCollections.observableArrayList();
    private final Set<String> visibleSheets = new HashSet<>();
    private double zoomFactor = 1.0;
    private static final double MIN_ZOOM = 0.6;
    private static final double MAX_ZOOM = 2.0;
    private static final double BASE_WIDTH = 1400;
    private static final double BASE_HEIGHT = 900;
    private static final double MAX_CANVAS_DIM = 4096;

    public void init(ObservableList<Patient> patients,
                     ObservableList<EventSession> sessions,
                     ObservableList<Encounter> encounters,
                     ObservableList<QuestionnaireResponse> questionnaires,
                     ObservableList<CommonRow> commons,
                     ObservableList<ScenarioTestCase> scenarios) {
        ragService = new RagRuleService(patients, sessions, encounters, questionnaires, commons, scenarios);
        setupTable();
        if (sheetList != null) {
            sheetList.getItems().setAll(ragService.sheetNames());
            sheetList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> loadRules());
            if (!sheetList.getItems().isEmpty()) {
                sheetList.getSelectionModel().select(0);
            }
        }
        if (filterList != null) {
            filterList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            filterList.getItems().setAll(ragService.sheetNames());
            filterList.getSelectionModel().selectAll();
            visibleSheets.clear();
            visibleSheets.addAll(filterList.getItems());
        }
        if (scenarioBox != null) {
            scenarioBox.getItems().setAll(ragService.scenarioNames());
            scenarioBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> loadRules());
        }
        if (statusLabel != null) {
            statusLabel.setText("RAG Rule Builder: click a column to view logic, mappings, dependencies, and live preview.");
        }
        if (legendLabel != null) {
            legendLabel.setText("Legend: sheet clusters colored; columns shown only when used; edges labeled by logic (if enabled); filters limit visible sheets.");
        }
        if (showColumnsToggle != null) {
            showColumnsToggle.selectedProperty().addListener((obs, ov, nv) -> drawGraph());
        }
        if (showEdgeLabelsToggle != null) {
            showEdgeLabelsToggle.selectedProperty().addListener((obs, ov, nv) -> drawGraph());
        }
        if (highlightSelectedToggle != null) {
            highlightSelectedToggle.setSelected(true);
            highlightSelectedToggle.selectedProperty().addListener((obs, ov, nv) -> drawGraph());
        }
        if (zoomSlider != null) {
            zoomSlider.setMin(MIN_ZOOM);
            zoomSlider.setMax(MAX_ZOOM);
            zoomSlider.setBlockIncrement(0.1);
            zoomSlider.setValue(1.0);
            zoomSlider.valueProperty().addListener((obs, ov, nv) -> setZoom(nv == null ? 1.0 : nv.doubleValue(), dependencyCanvas, zoomSlider, null));
            updateZoomLabel();
        }
    }

    private void setupTable() {
        if (ruleTable == null) return;
        ruleTable.setItems(rows);
        if (colName != null) colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().column()));
        if (colType != null) colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().dataType()));
        if (colFormat != null) colFormat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().format()));
        if (colLogic != null) colLogic.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().logicType()));
        if (colMapping != null) colMapping.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mapping()));
        if (colPattern != null) colPattern.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().pattern()));
        ruleTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> renderSelection());
    }

    @FXML
    private void onRun() {
        RuleCardRow selected = ruleTable != null ? ruleTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            setStatus("Select a column first.");
            return;
        }
        String question = promptArea != null && promptArea.getText() != null ? promptArea.getText().trim() : "";
        RagRuleService.RagResult res = ragService.explain(selected.sheet(), selected.column(), question, currentScenario());
        renderResult(selected, res);
        setStatus("Explained logic for " + selected.sheet() + "." + selected.column());
    }

    @FXML
    private void onApply() {
        RuleCardRow selected = ruleTable != null ? ruleTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            setStatus("Select a column to edit.");
            return;
        }
        RagRuleService.RuleEdit edit = new RagRuleService.RuleEdit(
                selected.sheet(),
                selected.column(),
                text(sourceSheetField, selected.sourceSheet()),
                text(sourceColumnField, selected.sourceColumn()),
                text(transformField, selected.transform()),
                text(logicField, selected.logicType()),
                text(formatField, selected.format()),
                text(dataTypeField, selected.dataType()),
                text(patternField, selected.pattern())
        );
        String note = promptArea != null ? promptArea.getText() : "";
        ragService.updateRule(edit, note, currentScenario());
        loadRules();
        setStatus("Updated mapping/logic for " + selected.sheet() + "." + selected.column());
        onRun();
    }

    @FXML
    private void onClear() {
        if (promptArea != null) promptArea.clear();
        if (proposalArea != null) proposalArea.clear();
        if (answerArea != null) answerArea.clear();
        if (validationsArea != null) validationsArea.clear();
        if (planArea != null) planArea.clear();
        if (toolsArea != null) toolsArea.clear();
        if (dependenciesList != null) dependenciesList.getItems().clear();
        if (previewList != null) previewList.getItems().clear();
    }

    @FXML
    private void onRefreshGraph() {
        drawGraph();
    }

    @FXML
    private void onApplyFilters() {
        if (filterList == null) return;
        visibleSheets.clear();
        visibleSheets.addAll(filterList.getSelectionModel().getSelectedItems());
        if (visibleSheets.isEmpty()) {
            visibleSheets.addAll(filterList.getItems());
        }
        drawGraph();
    }

    @FXML
    private void onSelectAllFilters() {
        if (filterList == null) return;
        filterList.getSelectionModel().selectAll();
        visibleSheets.clear();
        visibleSheets.addAll(filterList.getItems());
        drawGraph();
    }

    @FXML
    private void onCopyMermaid() {
        String mermaid = buildMermaid();
        ClipboardContent content = new ClipboardContent();
        content.putString(mermaid);
        Clipboard.getSystemClipboard().setContent(content);
        setStatus("Copied Mermaid graph to clipboard.");
    }

    private void loadRules() {
        String sheet = sheetList != null ? sheetList.getSelectionModel().getSelectedItem() : "";
        if (sheet == null || sheet.isBlank()) return;
        List<RagRuleService.RuleCard> cards = ragService.ruleCards(sheet, currentScenario());
        rows.setAll(cards.stream().map(RuleCardRow::from).toList());
        if (!rows.isEmpty()) {
            ruleTable.getSelectionModel().select(0);
            renderSelection();
        }
        drawGraph();
    }

    private void renderSelection() {
        RuleCardRow row = ruleTable != null ? ruleTable.getSelectionModel().getSelectedItem() : null;
        if (row == null) return;
        setField(sourceSheetField, row.sourceSheet());
        setField(sourceColumnField, row.sourceColumn());
        setField(logicField, row.logicType());
        setField(formatField, row.format());
        setField(dataTypeField, row.dataType());
        setField(patternField, row.pattern());
        setField(transformField, row.transform());
        if (proposalArea != null) {
            proposalArea.setText(row.summary());
        }
        onRun();
    }

    private void renderResult(RuleCardRow row, RagRuleService.RagResult res) {
        if (res == null) return;
        if (answerArea != null) answerArea.setText(res.explanation());
        if (planArea != null) planArea.setText(String.join("\n", res.plan()));
        if (toolsArea != null) {
            toolsArea.setText(res.hits().stream()
                    .map(h -> String.format("[%.2f] %s", h.score(), h.text()))
                    .collect(Collectors.joining("\n")));
        }
        if (dependenciesList != null) dependenciesList.getItems().setAll(res.dependencies());
        if (previewList != null) previewList.getItems().setAll(res.previews());
        if (validationsArea != null) validationsArea.setText(String.join("\n", res.validations()));
    }

    @FXML
    private void onOpenGraphFullScreen() {
        Canvas canvas = new Canvas(
                dependencyCanvas != null ? dependencyCanvas.getWidth() : BASE_WIDTH,
                dependencyCanvas != null ? dependencyCanvas.getHeight() : BASE_HEIGHT);
        StackPane pane = new StackPane(canvas);
        ScrollPane scroll = new ScrollPane(pane);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.setPannable(true);
        Label zoomDisplay = new Label(String.format("%.0f%%", zoomFactor * 100));
        Slider fsSlider = new Slider(MIN_ZOOM, MAX_ZOOM, zoomFactor);
        fsSlider.setBlockIncrement(0.1);
        fsSlider.valueProperty().addListener((obs, ov, nv) -> {
            setZoom(nv == null ? zoomFactor : nv.doubleValue(), canvas, fsSlider, zoomDisplay);
        });
        Button zoomOut = new Button("-");
        zoomOut.setOnAction(e -> setZoom(zoomFactor - 0.1, canvas, fsSlider, zoomDisplay));
        Button zoomIn = new Button("+");
        zoomIn.setOnAction(e -> setZoom(zoomFactor + 0.1, canvas, fsSlider, zoomDisplay));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controls = new HBox(8, zoomOut, fsSlider, zoomIn, zoomDisplay, spacer);
        controls.setPadding(new Insets(8, 12, 8, 12));
        Scene scene = new Scene(new BorderPane(scroll, controls, null, null, null), 1600, 900);
        Stage stage = new Stage();
        stage.setTitle("Dependency Mind Map");
        stage.setScene(scene);
        stage.setMaximized(true);
        scroll.viewportBoundsProperty().addListener((obs, ov, nv) -> {
            double w = clampDim(Math.max(BASE_WIDTH * zoomFactor, nv.getWidth() * zoomFactor));
            double h = clampDim(Math.max(BASE_HEIGHT * zoomFactor, nv.getHeight() * zoomFactor));
            canvas.setWidth(w);
            canvas.setHeight(h);
            renderGraph(canvas);
        });
        double initW = clampDim(Math.max(BASE_WIDTH * zoomFactor, scroll.getViewportBounds().getWidth() * zoomFactor));
        double initH = clampDim(Math.max(BASE_HEIGHT * zoomFactor, scroll.getViewportBounds().getHeight() * zoomFactor));
        canvas.setWidth(initW);
        canvas.setHeight(initH);
        stage.show();
        renderGraph(canvas);
    }

    private void drawGraph() {
        if (dependencyCanvas == null) return;
        renderGraph(dependencyCanvas);
    }

    private void renderGraph(Canvas canvas) {
        if (canvas == null) return;
        double zoom = zoomFactor <= 0 ? 1.0 : zoomFactor;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        List<RagRuleService.DependencyEdge> edges = ragService.dependencyGraph();
        List<String> allSheets = ragService.sheetNames();
        Set<String> allowed = visibleSheets.isEmpty() ? new HashSet<>(allSheets) : new HashSet<>(visibleSheets);
        List<String> sheets = allSheets.stream().filter(allowed::contains).toList();
        String selectedSheet = (sheetList != null && highlightSelectedToggle != null && highlightSelectedToggle.isSelected())
                ? sheetList.getSelectionModel().getSelectedItem()
                : null;
        double faded = 0.22;
        boolean showColumns = showColumnsToggle != null && showColumnsToggle.isSelected();
        boolean showEdgeLabels = showEdgeLabelsToggle != null && showEdgeLabelsToggle.isSelected();

        // Radial mind-map layout
        double layoutW = Math.max(BASE_WIDTH, canvas.getWidth() / zoom);
        double layoutH = Math.max(BASE_HEIGHT, canvas.getHeight() / zoom);
        double desiredW = clampDim(layoutW * zoom);
        double desiredH = clampDim(layoutH * zoom);
        if (Math.abs(desiredW - canvas.getWidth()) > 1 || Math.abs(desiredH - canvas.getHeight()) > 1) {
            canvas.setWidth(desiredW);
            canvas.setHeight(desiredH);
        }
        double cx = layoutW / 2;
        double cy = layoutH / 2;
        double radius = Math.min(layoutW, layoutH) / 2.6;
        int n = Math.max(1, sheets.size());
        Map<String, double[]> pos = new HashMap<>();
        Map<String, Color> sheetColors = new HashMap<>();
        Color[] palette = {
                Color.web("#1e88e5"), Color.web("#8e24aa"), Color.web("#43a047"),
                Color.web("#f4511e"), Color.web("#3949ab"), Color.web("#00acc1"),
                Color.web("#6d4c41"), Color.web("#e53935"), Color.web("#00897b")
        };
        for (int i = 0; i < sheets.size(); i++) {
            double angle = (2 * Math.PI * i) / n - Math.PI / 2;
            double x = cx + radius * Math.cos(angle);
            double y = cy + radius * Math.sin(angle);
            pos.put(sheets.get(i), new double[]{x, y, angle});
            sheetColors.put(sheets.get(i), palette[i % palette.length]);
        }

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Sheets
        for (String sheet : sheets) {
            double[] p = pos.get(sheet);
            boolean isSelected = selectedSheet != null && selectedSheet.equals(sheet);
            double alpha = selectedSheet == null || isSelected ? 1.0 : faded;
            drawNode(gc, p[0] * zoom, p[1] * zoom, sheet, zoom, true, sheetColors.get(sheet), alpha);
        }

        // Sheet-to-sheet edges
        gc.setLineWidth(1.1 * zoom);
        for (RagRuleService.DependencyEdge e : edges) {
            double[] a = pos.get(e.fromSheet());
            double[] b = pos.get(e.toSheet());
            if (a == null || b == null) continue;
            boolean relevant = selectedSheet == null || selectedSheet.equals(e.fromSheet()) || selectedSheet.equals(e.toSheet());
            double alpha = relevant ? 0.85 : faded;
            Color c = sheetColors.getOrDefault(e.fromSheet(), Color.web("#607d8b"));
            gc.setStroke(c.deriveColor(0, 1, 1, alpha));
            double y1 = (a[1] + 6) * zoom;
            double y2 = (b[1] - 6) * zoom;
            double midX = ((a[0] + b[0]) / 2) * zoom;
            double ctrlY = ((a[1] + b[1]) / 2) * zoom;
            gc.beginPath();
            gc.moveTo(a[0] * zoom, y1);
            gc.quadraticCurveTo(midX, ctrlY, b[0] * zoom, y2);
            gc.stroke();
            if (showEdgeLabels) {
                gc.setFill(Color.web("#263238"));
                String lbl = trimLabel(e.fromColumn() + " → " + e.toColumn(), 32);
                gc.fillText(lbl, midX - 60 * zoom, ctrlY + 14 * zoom);
            }
        }

        // Sheet -> column nodes (only columns that participate in dependencies)
        if (showColumns && ragService.ruleGraph() != null && ragService.ruleGraph().sheets != null) {
            gc.setStroke(Color.web("#b0bec5"));
            gc.setLineWidth(0.8 * zoom);
            for (var sheetRule : ragService.ruleGraph().sheets) {
                double[] origin = pos.get(sheetRule.name);
                if (origin == null || sheetRule.columns == null) continue;
                var relevantCols = sheetRule.columns.stream()
                        .filter(col -> (col.sourceSheet != null && !col.sourceSheet.isBlank())
                                || hasDependents(sheetRule.name, col.name))
                        .toList();
                int cols = relevantCols.size();
                if (cols == 0) continue;
                double baseAngle = origin[2];
                double angleSpread = Math.min(Math.PI / 1.15, Math.max(0.35, cols * 0.1));
                double startAngle = baseAngle - angleSpread / 2.0;
                double step = cols <= 1 ? 0 : angleSpread / (cols - 1);
                double colRadius = radius * (1.1 + Math.min(0.8, cols / 10.0));
                for (int i = 0; i < cols; i++) {
                    var col = relevantCols.get(i);
                    double angle = startAngle + step * i;
                    double cxCol = cx + colRadius * Math.cos(angle);
                    double cyCol = cy + colRadius * Math.sin(angle);
                    String label = trimLabel(col.name, 16) + " • " + trimLabel(col.logicType, 10);
                    boolean relevant = selectedSheet == null || selectedSheet.equals(sheetRule.name)
                            || (col.sourceSheet != null && col.sourceSheet.equals(selectedSheet));
                    double alpha = relevant ? 1.0 : faded;
                    drawNode(gc, cxCol * zoom, cyCol * zoom, label, zoom, false, sheetColors.getOrDefault(sheetRule.name, Color.web("#90a4ae")), alpha);
                    gc.setStroke(Color.web("#b0bec5").deriveColor(0, 1, 1, alpha));
                    gc.strokeLine(origin[0] * zoom, origin[1] * zoom, cxCol * zoom, cyCol * zoom);
                    // Column dependency arrows
                    if (col.sourceSheet != null && !col.sourceSheet.isBlank() && col.sourceColumn != null) {
                        String fromId = col.sourceSheet;
                        String toId = sheetRule.name;
                        double[] fromPos = pos.get(fromId);
                        if (fromPos != null) {
                            boolean edgeRelevant = selectedSheet == null || selectedSheet.equals(fromId) || selectedSheet.equals(toId);
                            double edgeAlpha = edgeRelevant ? 0.9 : faded;
                            gc.setStroke(sheetColors.getOrDefault(fromId, Color.web("#546e7a")).deriveColor(0, 1, 1, edgeAlpha));
                            gc.setLineWidth(0.9 * zoom);
                            gc.beginPath();
                            gc.moveTo(fromPos[0] * zoom, fromPos[1] * zoom);
                            gc.lineTo(cxCol * zoom, cyCol * zoom);
                            gc.stroke();
                            if (showEdgeLabels) {
                                String lbl = trimLabel(safe(col.logicType), 12);
                                gc.setFill(Color.web("#263238").deriveColor(0, 1, 1, edgeAlpha));
                                gc.fillText(lbl, (fromPos[0] * zoom + cxCol * zoom) / 2.0,
                                        (fromPos[1] * zoom + cyCol * zoom) / 2.0);
                            }
                        }
                    }
                }
            }
        }
    }

    private void drawNode(GraphicsContext gc, double x, double y, String label, double zoom, boolean highlight, Color color, double alpha) {
        double bw = 150 * zoom;
        double bh = 32 * zoom;
        Color fill = highlight ? color.deriveColor(0, 1, 1.0, 0.14 * alpha) : Color.web("#f6f9fb").deriveColor(0, 1, 1, alpha);
        Color stroke = (color != null ? color : Color.web("#1e88e5")).deriveColor(0, 1, 1, alpha);
        gc.setFill(fill);
        gc.fillRoundRect(x - bw / 2, y - bh / 2, bw, bh, 10 * zoom, 10 * zoom);
        gc.setStroke(stroke);
        gc.setLineWidth(highlight ? 1.6 * zoom : 1.0 * zoom);
        gc.strokeRoundRect(x - bw / 2, y - bh / 2, bw, bh, 10 * zoom, 10 * zoom);
        gc.setFill(Color.web("#1f2933").deriveColor(0, 1, 1, alpha));
        gc.fillText(label, x - bw / 2 + 8 * zoom, y + 5 * zoom);
    }

    private String trimLabel(String text, int max) {
        if (text == null) return "";
        String t = text.trim();
        if (t.length() <= max) return t;
        return t.substring(0, Math.max(0, max - 1)) + "…";
    }

    private boolean hasDependents(String sheet, String column) {
        if (ragService.ruleGraph() == null || ragService.ruleGraph().sheets == null) return false;
        for (var s : ragService.ruleGraph().sheets) {
            if (s.columns == null) continue;
            for (var c : s.columns) {
                if (safe(c.sourceSheet).equals(sheet) && safe(c.sourceColumn).equals(column)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String text(TextField field, String fallback) {
        if (field == null || field.getText() == null || field.getText().isBlank()) return fallback;
        return field.getText().trim();
    }

    private void setField(TextField field, String value) {
        if (field != null) field.setText(value);
    }

    private String currentScenario() {
        return scenarioBox != null ? scenarioBox.getValue() : null;
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private void updateZoomLabel() {
        if (zoomLabel != null) {
            zoomLabel.setText(String.format("%.0f%%", zoomFactor * 100));
        }
    }

    private void setZoom(double value, Canvas targetCanvas, Slider source, Label extraLabel) {
        double clamped = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
        zoomFactor = clamped;
        updateZoomLabel();
        if (extraLabel != null) {
            extraLabel.setText(String.format("%.0f%%", zoomFactor * 100));
        }
        if (zoomSlider != null && zoomSlider != source && Math.abs(zoomSlider.getValue() - zoomFactor) > 0.001) {
            zoomSlider.setValue(zoomFactor);
        }
        if (targetCanvas != null) {
            renderGraph(targetCanvas);
        } else {
            drawGraph();
        }
    }

    private double clampDim(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return Math.min(MAX_CANVAS_DIM, Math.max(BASE_WIDTH, 800));
        return Math.max(400, Math.min(MAX_CANVAS_DIM, v));
    }

    private String buildMermaid() {
        StringBuilder sb = new StringBuilder("flowchart LR\n");
        var graph = ragService.ruleGraph();
        Set<String> allowed = visibleSheets.isEmpty() && graph != null && graph.sheets != null
                ? graph.sheets.stream().map(s -> safe(s.name)).collect(Collectors.toSet())
                : new HashSet<>(visibleSheets);
        if (graph != null && graph.sheets != null) {
            for (var s : graph.sheets) {
                if (!allowed.contains(safe(s.name))) continue;
                String sheetId = sanitizeId(s.name);
                sb.append("  subgraph ").append(sheetId).append("[\"").append(s.name).append("\"]\n");
                if (s.columns != null) {
                    for (var c : s.columns) {
                        if ((c.sourceSheet == null || c.sourceSheet.isBlank()) && !hasDependents(s.name, c.name)) continue;
                        String colId = sheetId + "_" + sanitizeId(c.name);
                        sb.append("    ").append(colId).append("[\"").append(c.name).append("\"]\n");
                    }
                }
                sb.append("  end\n\n");
            }
            if (graph.sheets != null) {
                for (var s : graph.sheets) {
                    if (!allowed.contains(safe(s.name)) || s.columns == null) continue;
                    for (var c : s.columns) {
                        if (c.sourceSheet == null || c.sourceSheet.isBlank() || c.sourceColumn == null) continue;
                        if (!allowed.contains(safe(c.sourceSheet))) continue;
                        String fromId = sanitizeId(c.sourceSheet) + "_" + sanitizeId(c.sourceColumn);
                        String toId = sanitizeId(s.name) + "_" + sanitizeId(c.name);
                        String lbl = safe(c.logicType).isBlank() ? "derived" : safe(c.logicType);
                        sb.append("  ").append(fromId).append(" -. ").append(lbl).append(" .-> ").append(toId).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    private String sanitizeId(String v) {
        return safe(v).replaceAll("[^A-Za-z0-9_]+", "_");
    }

    public record RuleCardRow(String sheet, String column, String dataType, String format, String logicType,
                              String pattern, String sourceSheet, String sourceColumn, String transform) {
        static RuleCardRow from(RagRuleService.RuleCard c) {
            return new RuleCardRow(c.sheet(), c.column(), c.dataType(), c.format(), c.logicType(),
                    c.previewPattern(), c.sourceSheet(), c.sourceColumn(), c.transform());
        }

        String mapping() {
            if (sourceSheet == null || sourceSheet.isBlank()) return "";
            return sourceSheet + "." + safe(sourceColumn);
        }

        String summary() {
            return "%s.%s [%s/%s, logic=%s] from %s.%s (%s)"
                    .formatted(sheet, column, dataType, format, logicType, safe(sourceSheet),
                            safe(sourceColumn), safe(transform));
        }

        private String safe(String v) { return v == null ? "" : v; }
    }
}
