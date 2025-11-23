package com.aac.kpi.controller;

import com.aac.kpi.model.CommonRow;
import com.aac.kpi.model.Encounter;
import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.QuestionnaireResponse;
import com.aac.kpi.model.ScenarioTestCase;
import com.aac.kpi.service.RagRuleService;
import com.google.gson.Gson;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Logic Graph (mind map) controller that embeds a Cytoscape-based web view.
 * Supports search, dependency toggle, auto-layout, export, and inline edits.
 */
public class LogicGraphController {
    @FXML private WebView webView;
    @FXML private TextField searchField;
    @FXML private CheckBox dependencyToggle;
    @FXML private Button refreshButton;
    @FXML private Button exportPngButton;
    @FXML private Button exportSvgButton;
    @FXML private Button autoLayoutButton;

    private RagRuleService ragService;
    private WebEngine engine;
    private static final Gson GSON = new Gson();

    public void init(ObservableList<Patient> patients,
                     ObservableList<EventSession> sessions,
                     ObservableList<Encounter> encounters,
                     ObservableList<QuestionnaireResponse> questionnaires,
                     ObservableList<CommonRow> commons,
                     ObservableList<ScenarioTestCase> scenarios) {
        ragService = new RagRuleService(patients, sessions, encounters, questionnaires, commons, scenarios);
        setupWebView();
    }

    private void setupWebView() {
        if (webView == null) return;
        engine = webView.getEngine();
        String html = loadTemplate();
        engine.loadContent(html, "text/html");
        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((obs, oldV, newV) -> {
            if (newV == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", new JsBridge());
                sendGraph();
            }
        });
        if (searchField != null) {
            searchField.textProperty().addListener((obs, ov, nv) -> {
                executeJs("highlightQuery(" + GSON.toJson(nv == null ? "" : nv) + ")");
            });
        }
        if (dependencyToggle != null) {
            dependencyToggle.selectedProperty().addListener((obs, ov, nv) -> sendGraph());
        }
    }

    private String loadTemplate() {
        try {
            String html = readResource("/com/aac/kpi/logic-map.html");
            String mindJs = readResource("/com/aac/kpi/mind-elixir.js");
            String domJs = readResource("/com/aac/kpi/dom-to-image.min.js");
            html = html.replace("<!-- MIND_ELIXIR_JS -->", mindJs);
            html = html.replace("<!-- DOM_TO_IMAGE_JS -->", domJs);
            return html;
        } catch (Exception ex) {
            return "<html><body>Failed to load mind map: " + ex.getMessage() + "</body></html>";
        }
    }

    private String readResource(String path) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new IllegalStateException("Missing resource: " + path);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return br.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    @FXML
    private void onRefresh() {
        sendGraph();
    }

    @FXML
    private void onExportPng() {
        executeJs("exportPng()");
    }

    @FXML
    private void onExportSvg() {
        executeJs("exportSvg()");
    }

    @FXML
    private void onAutoLayout() {
        executeJs("autoLayout()");
    }

    private void sendGraph() {
        boolean deps = dependencyToggle == null || dependencyToggle.isSelected();
        String json = ragService.buildGraphJson(deps);
        executeJs("updateGraph(" + json + ")");
    }

    private void executeJs(String script) {
        if (engine != null) {
            try {
                engine.executeScript(script);
            } catch (Exception ignored) {}
        }
    }

    /**
     * JS bridge for node click -> edit rule.
     */
    public class JsBridge {
        public void onNodeClick(String nodeId) {
            if (nodeId == null || !nodeId.startsWith("col|")) return;
            String[] parts = nodeId.split("\\|", 3);
            if (parts.length < 3) return;
            String sheet = parts[1];
            String column = parts[2];
            // open a quick inline edit dialog
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.TextInputDialog dlg = new javafx.scene.control.TextInputDialog();
                dlg.setTitle("Edit Rule");
                dlg.setHeaderText("Update logic for " + sheet + "." + column);
                dlg.setContentText("Enter logic type,format,sourceSheet,sourceColumn (comma-separated):");
                var res = dlg.showAndWait();
                if (res.isEmpty()) return;
                String[] vals = res.get().split(",");
                String logic = vals.length > 0 ? vals[0].trim() : "";
                String format = vals.length > 1 ? vals[1].trim() : "";
                String srcSheet = vals.length > 2 ? vals[2].trim() : "";
                String srcCol = vals.length > 3 ? vals[3].trim() : "";
                ragService.updateRule(new RagRuleService.RuleEdit(sheet, column, srcSheet, srcCol, "", logic, format, "", ""), "Edited via mind map", null);
                sendGraph();
            });
        }
    }
}
