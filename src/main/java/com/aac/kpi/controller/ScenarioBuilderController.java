package com.aac.kpi.controller;

import com.aac.kpi.model.ScenarioTestCase;
import com.aac.kpi.service.ScenarioReader;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SelectionMode;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ScenarioBuilderController {
    @FXML private TextField tfNumberOfSeniors;
    @FXML private TextField tfCfs;
    @FXML private TextField tfModeOfEvent;
    @FXML private TextField tfAapSessionDate;
    @FXML private TextField tfAapAttendance;
    @FXML private TextField tfBoundary;
    @FXML private TextField tfPurpose;
    @FXML private TextField tfDateOfContact;
    @FXML private TextField tfAge;
    @FXML private TextArea taRemarks;

    @FXML private ComboBox<String> cbKpiType;
    @FXML private Spinner<Integer> spTotalCases;

    @FXML private TableView<ScenarioTestCase> table;
    @FXML private TableColumn<ScenarioTestCase, String> cSeniors;
    @FXML private TableColumn<ScenarioTestCase, String> cCfs;
    @FXML private TableColumn<ScenarioTestCase, String> cMode;
    @FXML private TableColumn<ScenarioTestCase, String> cAapDate;
    @FXML private TableColumn<ScenarioTestCase, String> cAapAttendance;
    @FXML private TableColumn<ScenarioTestCase, String> cBoundary;
    @FXML private TableColumn<ScenarioTestCase, String> cPurpose;
    @FXML private TableColumn<ScenarioTestCase, String> cContactDate;
    @FXML private TableColumn<ScenarioTestCase, String> cAge;
    @FXML private TableColumn<ScenarioTestCase, String> cRemarks;

    @FXML private Label summaryLabel;

    private ObservableList<ScenarioTestCase> scenarios;
    private ListChangeListener<ScenarioTestCase> scenarioListener;
    private Runnable generateExcelHandler;

    @FXML
    private void initialize() {
        if (cbKpiType != null) {
            cbKpiType.getItems().setAll("Robust", "Budding", "Befriending", "Frail");
        }
        if (spTotalCases != null) {
            spTotalCases.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 1));
        }
        cSeniors.setCellValueFactory(new PropertyValueFactory<>("numberOfSeniors"));
        cCfs.setCellValueFactory(new PropertyValueFactory<>("cfs"));
        cMode.setCellValueFactory(new PropertyValueFactory<>("modeOfEvent"));
        cAapDate.setCellValueFactory(new PropertyValueFactory<>("aapSessionDate"));
        cAapAttendance.setCellValueFactory(new PropertyValueFactory<>("numberOfAapAttendance"));
        cBoundary.setCellValueFactory(new PropertyValueFactory<>("withinBoundary"));
        cPurpose.setCellValueFactory(new PropertyValueFactory<>("purposeOfContact"));
        cContactDate.setCellValueFactory(new PropertyValueFactory<>("dateOfContact"));
        cAge.setCellValueFactory(new PropertyValueFactory<>("age"));
        cRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));

        if (cAapDate != null) {
            cAapDate.setCellFactory(TextFieldTableCell.forTableColumn());
            cAapDate.setOnEditCommit(e -> {
                ScenarioTestCase row = e.getRowValue();
                if (row != null) {
                    String newVal = e.getNewValue();
                    row.setAapSessionDate(newVal == null ? "" : newVal.trim());
                }
                if (table != null) table.refresh();
            });
        }

        resetFormFields();
        if (table != null) {
            table.setEditable(true);
            table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }
    }

    public void init(ObservableList<ScenarioTestCase> scenarios) {
        if (scenarioListener != null && this.scenarios != null) {
            this.scenarios.removeListener(scenarioListener);
        }
        this.scenarios = scenarios;
        table.setItems(scenarios);
        scenarioListener = change -> updateSummary();
        scenarios.addListener(scenarioListener);
        updateSummary();
    }

    public void setOnGenerateExcel(Runnable handler) {
        this.generateExcelHandler = handler;
    }

    @FXML
    private void onLoadFromExcel() {
        if (scenarios == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Scenario Configuration");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File file = chooser.showOpenDialog(table.getScene() != null ? table.getScene().getWindow() : null);
        if (file == null) return;
        try {
            List<ScenarioTestCase> loaded = ScenarioReader.readScenarios(file);
            if (loaded.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "No scenarios found", "The selected file did not contain any scenario rows.");
            } else {
                scenarios.setAll(loaded);
                showAlert(Alert.AlertType.INFORMATION, "Loaded", loaded.size() + " scenario(s) imported.");
            }
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Failed to load scenarios", ex.getMessage());
        }
    }

    @FXML
    private void onAddScenario() {
        if (scenarios == null) return;
        ScenarioTestCase scenario = new ScenarioTestCase();
        scenario.setNumberOfSeniors(getTrimmed(tfNumberOfSeniors));
        scenario.setCfs(getTrimmed(tfCfs));
        scenario.setModeOfEvent(getTrimmed(tfModeOfEvent));
        scenario.setAapSessionDate(getTrimmed(tfAapSessionDate));
        scenario.setNumberOfAapAttendance(getTrimmed(tfAapAttendance));
        scenario.setWithinBoundary(getTrimmed(tfBoundary));
        scenario.setPurposeOfContact(getTrimmed(tfPurpose));
        scenario.setDateOfContact(getTrimmed(tfDateOfContact));
        scenario.setAge(getTrimmed(tfAge));
        scenario.setRemarks(getTrimmed(taRemarks));
        scenarios.add(scenario);
        resetFormFields();
    }

    @FXML
    private void onRemoveSelected() {
        if (scenarios == null) return;
        var selected = table.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) return;
        scenarios.removeAll(java.util.List.copyOf(selected));
    }

    @FXML
    private void onResetForm() {
        resetFormFields();
    }

    @FXML
    private void onGenerateExcel() {
        if (generateExcelHandler != null) {
            String kpi = cbKpiType != null ? cbKpiType.getValue() : "";
            if (kpi != null && !kpi.isBlank()) {
                showAlert(Alert.AlertType.INFORMATION, "KPI Type",
                        "Generating Excel for KPI type: " + kpi + ".\nScenarios defined: " + (scenarios != null ? scenarios.size() : 0));
            }
            generateExcelHandler.run();
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Generate Excel",
                    "Scenario Builder is not yet wired to the Excel generator. Please use the File → Save / Save As menu for now.");
        }
    }

    private void resetFormFields() {
        if (tfNumberOfSeniors != null) tfNumberOfSeniors.clear();
        if (tfCfs != null) tfCfs.clear();
        if (tfModeOfEvent != null) tfModeOfEvent.clear();
        if (tfAapSessionDate != null) tfAapSessionDate.clear();
        if (tfAapAttendance != null) tfAapAttendance.clear();
        if (tfBoundary != null) tfBoundary.clear();
        if (tfPurpose != null) tfPurpose.clear();
        if (tfDateOfContact != null) tfDateOfContact.clear();
        if (tfAge != null) tfAge.clear();
        if (taRemarks != null) taRemarks.clear();
    }

    public void resetForm() {
        resetFormFields();
    }

    private String getTrimmed(TextInputControl control) {
        if (control == null) return "";
        return control.getText() == null ? "" : control.getText().trim();
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    private void updateSummary() {
        int count = scenarios != null ? scenarios.size() : 0;
        if (summaryLabel != null) {
            summaryLabel.setText(String.format("%d scenario%s defined", count, count == 1 ? "" : "s"));
        }
    }
}
