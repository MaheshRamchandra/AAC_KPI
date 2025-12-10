package com.aac.kpi.controller;

import com.aac.kpi.model.ScenarioTestCase;
import com.aac.kpi.service.AppState;
import com.aac.kpi.service.ScenarioReader;
import com.aac.kpi.service.ScenarioWriter;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.GridPane;

import com.aac.kpi.controller.CommonController.CfsSelection;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ScenarioBuilderController {
    @FXML private TextField tfNumberOfSeniors;
    @FXML private ComboBox<String> cbCfs;
    @FXML private ComboBox<String> cbModeOfEvent;
    @FXML private TextField tfAapSessionDate;
    @FXML private TextField tfAapAttendance;
    @FXML private ComboBox<String> cbBoundary;
    @FXML private ComboBox<String> cbPurpose;
    @FXML private TextField tfDateOfContact;
    @FXML private TextField tfAge;
    @FXML private TextArea taRemarks;
    @FXML private TextField tfContactLogs;
    @FXML private TextArea taCustomExtras;

    @FXML private ComboBox<String> cbKpiType;
    @FXML private Spinner<Integer> spTotalCases;

    @FXML private TableView<ScenarioTestCase> table;
    @FXML private TableColumn<ScenarioTestCase, String> cKpi;
    @FXML private TableColumn<ScenarioTestCase, String> cSeniors;
    @FXML private TableColumn<ScenarioTestCase, String> cCfs;
    @FXML private TableColumn<ScenarioTestCase, String> cMode;
    @FXML private TableColumn<ScenarioTestCase, String> cAapDate;
    @FXML private TableColumn<ScenarioTestCase, String> cAapAttendance;
    @FXML private TableColumn<ScenarioTestCase, String> cIsAttended;
    @FXML private TableColumn<ScenarioTestCase, String> cTotalRegistration;
    @FXML private TableColumn<ScenarioTestCase, String> cBoundary;
    @FXML private TableColumn<ScenarioTestCase, String> cPurpose;
    @FXML private TableColumn<ScenarioTestCase, String> cContactDate;
    @FXML private TableColumn<ScenarioTestCase, String> cEncounterStart;
    @FXML private TableColumn<ScenarioTestCase, String> cAge;
    @FXML private TableColumn<ScenarioTestCase, String> cPatientBirthdate;
    @FXML private TableColumn<ScenarioTestCase, String> cReportingMonth;
    @FXML private TableColumn<ScenarioTestCase, String> cReportDate;
    @FXML private TableColumn<ScenarioTestCase, String> cSocialRisk;
    @FXML private TableColumn<ScenarioTestCase, String> cBuddyStart;
    @FXML private TableColumn<ScenarioTestCase, String> cBuddyEnd;
    @FXML private TableColumn<ScenarioTestCase, String> cBefriendingStart;
    @FXML private TableColumn<ScenarioTestCase, String> cBefriendingEnd;
    @FXML private TableColumn<ScenarioTestCase, String> cRemarks;
    @FXML private TableColumn<ScenarioTestCase, String> cContactLogs;
    @FXML private TableColumn<ScenarioTestCase, String> cOverrides;

    @FXML private Label summaryLabel;

    private ObservableList<ScenarioTestCase> scenarios;
    private ListChangeListener<ScenarioTestCase> scenarioListener;
    private Runnable generateExcelHandler;
    // Global column overrides (apply to every scenario row)
    private final List<ScenarioTestCase.ColumnOverride> globalOverrides = new java.util.ArrayList<>();
    private File lastScenarioFile;
    private String lastScenarioSheetName = "";

    @FXML
    private void initialize() {
        if (cbKpiType != null) {
            cbKpiType.getItems().setAll("Robust", "Budding", "Befriending", "Frail");
        }
        if (spTotalCases != null) {
            spTotalCases.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 1));
        }
        if (cbCfs != null) {
            cbCfs.getItems().clear();
            for (CommonController.CfsSelection c : CommonController.CfsSelection.values()) {
                cbCfs.getItems().add(c.getLabel());
            }
        }
        if (cbModeOfEvent != null) {
            cbModeOfEvent.getItems().setAll(
                    EventSessionController.ModeSelection.IN_PERSON.getValue(),
                    EventSessionController.ModeSelection.FACE_TO_FACE.getValue(),
                    EventSessionController.ModeSelection.F2F.getValue(),
                    EventSessionController.ModeSelection.IN_PERSON_LOWER.getValue()
            );
        }
        if (cbBoundary != null) {
            cbBoundary.getItems().setAll("Within", "Outside", "Out of service boundary");
        }
        if (cbPurpose != null) {
            cbPurpose.getItems().clear();
            for (EncounterMasterController.PurposeSelection p : EncounterMasterController.PurposeSelection.values()) {
                cbPurpose.getItems().add(p.getValue());
            }
        }
        if (cKpi != null) {
            cKpi.setCellValueFactory(new PropertyValueFactory<>("kpiType"));
        }
        cSeniors.setCellValueFactory(new PropertyValueFactory<>("numberOfSeniors"));
        cCfs.setCellValueFactory(new PropertyValueFactory<>("cfs"));
        cMode.setCellValueFactory(new PropertyValueFactory<>("modeOfEvent"));
        cAapDate.setCellValueFactory(new PropertyValueFactory<>("aapSessionDate"));
        cAapAttendance.setCellValueFactory(new PropertyValueFactory<>("numberOfAapAttendance"));
        if (cIsAttended != null) {
            cIsAttended.setCellValueFactory(new PropertyValueFactory<>("attendedIndicator"));
        }
        if (cTotalRegistration != null) {
            cTotalRegistration.setCellValueFactory(new PropertyValueFactory<>("totalRegistrations"));
        }
        cBoundary.setCellValueFactory(new PropertyValueFactory<>("withinBoundary"));
        cPurpose.setCellValueFactory(new PropertyValueFactory<>("purposeOfContact"));
        cContactDate.setCellValueFactory(new PropertyValueFactory<>("dateOfContact"));
        if (cEncounterStart != null) {
            cEncounterStart.setCellValueFactory(new PropertyValueFactory<>("encounterStart"));
        }
        cAge.setCellValueFactory(new PropertyValueFactory<>("age"));
        if (cPatientBirthdate != null) {
            cPatientBirthdate.setCellValueFactory(new PropertyValueFactory<>("patientBirthdate"));
        }
        if (cReportingMonth != null) {
            cReportingMonth.setCellValueFactory(new PropertyValueFactory<>("reportingMonth"));
        }
        if (cReportDate != null) {
            cReportDate.setCellValueFactory(new PropertyValueFactory<>("reportDate"));
        }
        if (cSocialRisk != null) {
            cSocialRisk.setCellValueFactory(new PropertyValueFactory<>("socialRiskFactorScore"));
        }
        if (cBuddyStart != null) {
            cBuddyStart.setCellValueFactory(new PropertyValueFactory<>("buddyingProgrammePeriodStart"));
        }
        if (cBuddyEnd != null) {
            cBuddyEnd.setCellValueFactory(new PropertyValueFactory<>("buddyingProgrammePeriodEnd"));
        }
        if (cBefriendingStart != null) {
            cBefriendingStart.setCellValueFactory(new PropertyValueFactory<>("befriendingProgrammePeriodStart"));
        }
        if (cBefriendingEnd != null) {
            cBefriendingEnd.setCellValueFactory(new PropertyValueFactory<>("befriendingProgrammePeriodEnd"));
        }
        cRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));
        if (cContactLogs != null) {
            cContactLogs.setCellValueFactory(new PropertyValueFactory<>("contactLogs"));
        }
        if (cOverrides != null) {
            cOverrides.setCellValueFactory(cell -> {
                List<ScenarioTestCase.ColumnOverride> overrides = cell.getValue() != null ? cell.getValue().getColumnOverrides() : List.of();
                if (overrides == null || overrides.isEmpty()) return new javafx.beans.property.SimpleStringProperty("");
                String text = overrides.stream()
                        .map(ScenarioTestCase.ColumnOverride::toString)
                        .collect(Collectors.joining("; "));
                return new javafx.beans.property.SimpleStringProperty(text);
            });
            cOverrides.setCellFactory(TextFieldTableCell.forTableColumn());
            cOverrides.setOnEditCommit(e -> {
                ScenarioTestCase row = e.getRowValue();
                if (row == null) return;
                row.setColumnOverrides(parseOverrideString(e.getNewValue(), row.getColumnOverrides()));
            });
        }

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
        if (cKpi != null) {
            cKpi.setCellFactory(TextFieldTableCell.forTableColumn());
            cKpi.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setKpiType(trim(e.getNewValue())); });
        }
        if (cSeniors != null) {
            cSeniors.setCellFactory(TextFieldTableCell.forTableColumn());
            cSeniors.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setNumberOfSeniors(trim(e.getNewValue())); });
        }
        if (cCfs != null) {
            cCfs.setCellFactory(TextFieldTableCell.forTableColumn());
            cCfs.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setCfs(trim(e.getNewValue())); });
        }
        if (cMode != null) {
            cMode.setCellFactory(TextFieldTableCell.forTableColumn());
            cMode.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setModeOfEvent(trim(e.getNewValue())); });
        }
        if (cAapAttendance != null) {
            cAapAttendance.setCellFactory(TextFieldTableCell.forTableColumn());
            cAapAttendance.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setNumberOfAapAttendance(trim(e.getNewValue())); });
        }
        if (cIsAttended != null) {
            cIsAttended.setCellFactory(TextFieldTableCell.forTableColumn());
            cIsAttended.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setAttendedIndicator(trim(e.getNewValue())); });
        }
        if (cTotalRegistration != null) {
            cTotalRegistration.setCellFactory(TextFieldTableCell.forTableColumn());
            cTotalRegistration.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setTotalRegistrations(trim(e.getNewValue())); });
        }
        if (cBoundary != null) {
            cBoundary.setCellFactory(TextFieldTableCell.forTableColumn());
            cBoundary.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setWithinBoundary(trim(e.getNewValue())); });
        }
        if (cPurpose != null) {
            cPurpose.setCellFactory(TextFieldTableCell.forTableColumn());
            cPurpose.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setPurposeOfContact(trim(e.getNewValue())); });
        }
        if (cContactDate != null) {
            cContactDate.setCellFactory(TextFieldTableCell.forTableColumn());
            cContactDate.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setDateOfContact(trim(e.getNewValue())); });
        }
        if (cEncounterStart != null) {
            cEncounterStart.setCellFactory(TextFieldTableCell.forTableColumn());
            cEncounterStart.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setEncounterStart(trim(e.getNewValue())); });
        }
        if (cAge != null) {
            cAge.setCellFactory(TextFieldTableCell.forTableColumn());
            cAge.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setAge(trim(e.getNewValue())); });
        }
        if (cPatientBirthdate != null) {
            cPatientBirthdate.setCellFactory(TextFieldTableCell.forTableColumn());
            cPatientBirthdate.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setPatientBirthdate(trim(e.getNewValue())); });
        }
        if (cReportingMonth != null) {
            cReportingMonth.setCellFactory(TextFieldTableCell.forTableColumn());
            cReportingMonth.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setReportingMonth(trim(e.getNewValue())); });
        }
        if (cReportDate != null) {
            cReportDate.setCellFactory(TextFieldTableCell.forTableColumn());
            cReportDate.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setReportDate(trim(e.getNewValue())); });
        }
        if (cSocialRisk != null) {
            cSocialRisk.setCellFactory(TextFieldTableCell.forTableColumn());
            cSocialRisk.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setSocialRiskFactorScore(trim(e.getNewValue())); });
        }
        if (cBuddyStart != null) {
            cBuddyStart.setCellFactory(TextFieldTableCell.forTableColumn());
            cBuddyStart.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setBuddyingProgrammePeriodStart(trim(e.getNewValue())); });
        }
        if (cBuddyEnd != null) {
            cBuddyEnd.setCellFactory(TextFieldTableCell.forTableColumn());
            cBuddyEnd.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setBuddyingProgrammePeriodEnd(trim(e.getNewValue())); });
        }
        if (cBefriendingStart != null) {
            cBefriendingStart.setCellFactory(TextFieldTableCell.forTableColumn());
            cBefriendingStart.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setBefriendingProgrammePeriodStart(trim(e.getNewValue())); });
        }
        if (cBefriendingEnd != null) {
            cBefriendingEnd.setCellFactory(TextFieldTableCell.forTableColumn());
            cBefriendingEnd.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setBefriendingProgrammePeriodEnd(trim(e.getNewValue())); });
        }
        if (cRemarks != null) {
            cRemarks.setCellFactory(TextFieldTableCell.forTableColumn());
            cRemarks.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setRemarks(trim(e.getNewValue())); });
        }
        if (cContactLogs != null) {
            cContactLogs.setCellFactory(TextFieldTableCell.forTableColumn());
            cContactLogs.setOnEditCommit(e -> { if (e.getRowValue() != null) e.getRowValue().setContactLogs(trim(e.getNewValue())); });
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
        applyGlobalOverridesToScenarios(true);
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
            String sheetToRead = null;
            List<String> sheets = ScenarioReader.listSheetNames(file);
            boolean singleSheet = sheets.size() == 1;
            if (sheets.size() > 1) {
                String preferred = ScenarioReader.guessPreferredSheet(sheets);
                ChoiceDialog<String> dialog = new ChoiceDialog<>(preferred, sheets);
                dialog.setTitle("Select sheet");
                dialog.setHeaderText("Choose the sheet to load scenarios from.");
                dialog.setContentText("Sheet:");
                Optional<String> chosen = dialog.showAndWait();
                if (chosen.isEmpty()) return;
                sheetToRead = chosen.get();
            } else if (sheets.size() == 1) {
                sheetToRead = sheets.get(0);
            }
            List<ScenarioTestCase> loaded = ScenarioReader.readScenarios(file, sheetToRead);
            if (loaded.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "No scenarios found", "The selected file did not contain any scenario rows.");
            } else {
                scenarios.setAll(loaded);
                applyGlobalOverridesToScenarios(true);
                String sheetMsg = sheetToRead == null ? "" : (" from sheet \"" + sheetToRead + "\"");
                showAlert(Alert.AlertType.INFORMATION, "Loaded", loaded.size() + " scenario(s) imported" + sheetMsg + ".");
                AppState.setScenarioSkipPrompts(true);
                AppState.setScenarioSheetName(sheetToRead == null ? "" : sheetToRead);
                String kpiType = kpiTypeFromSheet(sheetToRead);
                if (singleSheet && (kpiType == null || kpiType.isBlank())) {
                    kpiType = sheetToRead == null ? "" : sheetToRead;
                }
                AppState.setScenarioSheetKpiType(kpiType);
                applyReportingMonthFromScenarios(loaded);
                lastScenarioFile = file;
                lastScenarioSheetName = sheetToRead == null ? "" : sheetToRead;
            }
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Failed to load scenarios", ex.getMessage());
        }
    }

    private String kpiTypeFromSheet(String sheet) {
        if (sheet == null) return "";
        String lower = sheet.toLowerCase(java.util.Locale.ENGLISH);
        if (lower.contains("robust")) return "Robust";
        if (lower.contains("frail")) return "Frail";
        if (lower.contains("buddy")) return "Buddying";
        if (lower.contains("befriend")) return "Befriending";
        return "";
    }

    private void applyReportingMonthFromScenarios(List<ScenarioTestCase> loaded) {
        if (loaded == null) return;
        String month = loaded.stream()
                .map(ScenarioTestCase::getReportingMonth)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse("");
        if (!month.isBlank()) {
            AppState.setReportingMonthOverride(month.trim());
        }
        String date = loaded.stream()
                .map(ScenarioTestCase::getReportDate)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse("");
        if (!date.isBlank()) {
            AppState.setReportDateOverride(date.trim());
        }
    }

    @FXML
    private void onAddScenario() {
        if (scenarios == null) return;
        ScenarioTestCase scenario = new ScenarioTestCase();
        scenario.setKpiType(getComboValue(cbKpiType));
        scenario.setNumberOfSeniors(getTrimmed(tfNumberOfSeniors));
        scenario.setCfs(getComboValue(cbCfs));
        scenario.setModeOfEvent(getComboValue(cbModeOfEvent));
        scenario.setAapSessionDate(getTrimmed(tfAapSessionDate));
        scenario.setNumberOfAapAttendance(getTrimmed(tfAapAttendance));
        scenario.setWithinBoundary(getComboValue(cbBoundary));
        scenario.setPurposeOfContact(getComboValue(cbPurpose));
        scenario.setDateOfContact(getTrimmed(tfDateOfContact));
        scenario.setAge(getTrimmed(tfAge));
        scenario.setRemarks(getTrimmed(taRemarks));
        scenario.setContactLogs(getTrimmed(tfContactLogs));
        scenario.setExtraFields(parseExtras(getTrimmed(taCustomExtras)));
        scenario.setColumnOverrides(new java.util.ArrayList<>(globalOverrides));
        scenarios.add(scenario);
        resetFormFields();
    }

    @FXML
    private void onAddColumnOverride() {
        Dialog<ScenarioTestCase.ColumnOverride> dialog = new Dialog<>();
        dialog.setTitle("Add Column Override");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> sheetBox = new ComboBox<>();
        sheetBox.getItems().addAll(
                "Patient (Master)",
                "Event Sessions",
                "Encounter (Master)",
                "QuestionnaireResponse (Master)",
                "Practitioner (Master)",
                "Common: resident_report",
                "Common: aac_report",
                "Common: event_report",
                "Common: organization_report",
                "Common: location_report"
        );
        sheetBox.setValue("Patient (Master)");

        ComboBox<String> columnBox = new ComboBox<>();
        columnBox.setEditable(true);
        columnBox.getItems().addAll(suggestedColumnsForSheet("Patient (Master)"));

        sheetBox.valueProperty().addListener((obs, ov, nv) -> {
            columnBox.getItems().setAll(suggestedColumnsForSheet(nv));
            if (!columnBox.getItems().isEmpty()) {
                columnBox.setValue(columnBox.getItems().get(0));
            }
        });

        TextField valueField = new TextField();
        valueField.setPromptText("Value to apply");

        GridPane content = new GridPane();
        content.setHgap(10);
        content.setVgap(10);
        content.add(new Label("Sheet"), 0, 0);
        content.add(sheetBox, 1, 0);
        content.add(new Label("Column"), 0, 1);
        content.add(columnBox, 1, 1);
        content.add(new Label("Value"), 0, 2);
        content.add(valueField, 1, 2);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return new ScenarioTestCase.ColumnOverride(
                        sheetBox.getValue(),
                        columnBox.getEditor().getText(),
                        valueField.getText()
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(ov -> {
            addGlobalOverride(ov);
            if (scenarios != null) {
                applyGlobalOverridesToScenarios(true);
            }
            if (table != null) table.refresh();
        });
    }

    @FXML
    private void onRemoveColumnOverride() {
        if (globalOverrides.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No columns", "There are no global columns to remove.");
            return;
        }
        ChoiceDialog<ScenarioTestCase.ColumnOverride> dlg = new ChoiceDialog<>(globalOverrides.get(0), globalOverrides);
        dlg.setTitle("Remove Column");
        dlg.setHeaderText("Pick a column override to remove");
        dlg.setContentText("Column:");
        dlg.showAndWait().ifPresent(selected -> {
            globalOverrides.remove(selected);
            if (scenarios != null) {
                for (ScenarioTestCase sc : scenarios) {
                    sc.getColumnOverrides().removeIf(ov ->
                            equalsIgnoreCase(ov.getSheet(), selected.getSheet())
                                    && equalsIgnoreCase(ov.getColumn(), selected.getColumn()));
                }
            }
            if (table != null) table.refresh();
        });
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

    @FXML
    private void onSaveScenarioExcel() {
        saveScenarios(false);
    }

    @FXML
    private void onSaveScenarioExcelAs() {
        saveScenarios(true);
    }

    private void saveScenarios(boolean forcePrompt) {
        if (scenarios == null) {
            showAlert(Alert.AlertType.INFORMATION, "No scenarios", "There are no scenario rows to save yet.");
            return;
        }
        finishActiveEdit();
        File target = forcePrompt ? null : lastScenarioFile;
        if (target == null) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Scenario Excel");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
            chooser.setInitialFileName(defaultScenarioFileName());
            target = chooser.showSaveDialog(table.getScene() != null ? table.getScene().getWindow() : null);
        }
        if (target == null) return;

        String sheetName = resolveScenarioSheetName();
        try {
            File saved = ScenarioWriter.write(target, new java.util.ArrayList<>(scenarios), sheetName);
            lastScenarioFile = saved;
            lastScenarioSheetName = sheetName;
            AppState.setScenarioSheetName(sheetName);
            reloadFromDisk(saved, sheetName);
            showAlert(Alert.AlertType.INFORMATION, "Saved",
                    "Scenarios saved to " + saved.getAbsolutePath() + " (sheet \"" + sheetName + "\").");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Failed to save scenarios", ex.getMessage());
        }
    }

    private String defaultScenarioFileName() {
        if (lastScenarioFile != null) {
            String name = lastScenarioFile.getName();
            if (name != null && !name.isBlank()) {
                return name.toLowerCase(Locale.ENGLISH).endsWith(".xlsx") ? name : name + ".xlsx";
            }
        }
        String base = AppState.getScenarioSheetName();
        if (base == null || base.isBlank()) base = "Scenarios";
        if (!base.toLowerCase(Locale.ENGLISH).endsWith(".xlsx")) {
            base = base + ".xlsx";
        }
        return base;
    }

    private String resolveScenarioSheetName() {
        if (lastScenarioSheetName != null && !lastScenarioSheetName.isBlank()) {
            return lastScenarioSheetName;
        }
        String fromState = AppState.getScenarioSheetName();
        if (fromState != null && !fromState.isBlank()) {
            return fromState;
        }
        return "Scenarios";
    }

    private void finishActiveEdit() {
        if (table == null) return;
        if (table.getEditingCell() != null) {
            table.edit(-1, null); // end any in-progress edit so values are committed
        }
    }

    private void reloadFromDisk(File file, String sheetName) {
        if (file == null || !file.exists()) return;
        try {
            List<ScenarioTestCase> persisted = ScenarioReader.readScenarios(file, sheetName);
            if (persisted != null && !persisted.isEmpty()) {
                scenarios.setAll(persisted);
                applyGlobalOverridesToScenarios(true);
                updateSummary();
            }
        } catch (IOException ignored) {
            // Best-effort; keep in-memory scenarios if reload fails
        }
    }

    private void resetFormFields() {
        if (tfNumberOfSeniors != null) tfNumberOfSeniors.clear();
        if (cbCfs != null) {
            cbCfs.getSelectionModel().clearSelection();
            if (cbCfs.isEditable() && cbCfs.getEditor() != null) {
                cbCfs.getEditor().clear();
            }
        }
        if (cbModeOfEvent != null) {
            cbModeOfEvent.getSelectionModel().clearSelection();
            if (cbModeOfEvent.isEditable() && cbModeOfEvent.getEditor() != null) {
                cbModeOfEvent.getEditor().clear();
            }
        }
        if (tfAapSessionDate != null) tfAapSessionDate.clear();
        if (tfAapAttendance != null) tfAapAttendance.clear();
        if (tfContactLogs != null) tfContactLogs.clear();
        if (cbBoundary != null) {
            cbBoundary.getSelectionModel().clearSelection();
            if (cbBoundary.isEditable() && cbBoundary.getEditor() != null) {
                cbBoundary.getEditor().clear();
            }
        }
        if (cbPurpose != null) {
            cbPurpose.getSelectionModel().clearSelection();
            if (cbPurpose.isEditable() && cbPurpose.getEditor() != null) {
                cbPurpose.getEditor().clear();
            }
        }
        if (tfDateOfContact != null) tfDateOfContact.clear();
        if (tfAge != null) tfAge.clear();
        if (taRemarks != null) taRemarks.clear();
        if (taCustomExtras != null) taCustomExtras.clear();
    }

    public void resetForm() {
        resetFormFields();
    }

    private String getTrimmed(TextInputControl control) {
        if (control == null) return "";
        return control.getText() == null ? "" : control.getText().trim();
    }

    private String getComboValue(ComboBox<String> combo) {
        if (combo == null) return "";
        if (combo.isEditable() && combo.getEditor() != null) {
            String text = combo.getEditor().getText();
            if (text != null && !text.trim().isEmpty()) return text.trim();
        }
        String sel = combo.getValue();
        return sel == null ? "" : sel.trim();
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    /**
     * Parse custom extras in the format:
     * key=value per line (comma or semicolon also supported).
     */
    private Map<String, String> parseExtras(String text) {
        Map<String, String> extras = new LinkedHashMap<>();
        if (text == null || text.isBlank()) return extras;
        String[] parts = text.split("[\\n;,]+");
        for (String part : parts) {
            if (part.isBlank()) continue;
            String[] kv = part.split("=", 2);
            String key = kv[0].trim();
            if (key.isEmpty()) continue;
            String value = kv.length > 1 ? kv[1].trim() : "";
            extras.put(key, value);
        }
        return extras;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void addGlobalOverride(ScenarioTestCase.ColumnOverride ov) {
        if (ov == null) return;
        // Avoid duplicate sheet+column entries
        for (ScenarioTestCase.ColumnOverride existing : globalOverrides) {
            if (equalsIgnoreCase(existing.getSheet(), ov.getSheet())
                    && equalsIgnoreCase(existing.getColumn(), ov.getColumn())) {
                existing.setValue(ov.getValue());
                return;
            }
        }
        globalOverrides.add(cloneOverride(ov));
    }

    private void applyGlobalOverridesToScenarios(boolean useExtrasWhenAvailable) {
        if (scenarios == null) return;
        for (ScenarioTestCase sc : scenarios) {
            List<ScenarioTestCase.ColumnOverride> merged = new java.util.ArrayList<>();
            for (ScenarioTestCase.ColumnOverride global : globalOverrides) {
                String value = global.getValue();
                if (useExtrasWhenAvailable) {
                    String fromExtra = sc.getExtraFields() == null ? null : sc.getExtraFields().get(global.getColumn());
                    if (fromExtra != null && !fromExtra.isBlank()) {
                        value = fromExtra;
                    }
                }
                merged.add(new ScenarioTestCase.ColumnOverride(global.getSheet(), global.getColumn(), value));
            }
            sc.setColumnOverrides(merged);
        }
        if (table != null) table.refresh();
    }

    private ScenarioTestCase.ColumnOverride cloneOverride(ScenarioTestCase.ColumnOverride ov) {
        return new ScenarioTestCase.ColumnOverride(ov.getSheet(), ov.getColumn(), ov.getValue());
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) return a == b;
        return a.equalsIgnoreCase(b);
    }

    private List<ScenarioTestCase.ColumnOverride> parseOverrideString(String text, List<ScenarioTestCase.ColumnOverride> fallback) {
        if (text == null || text.isBlank()) {
            return fallback == null ? List.of() : new java.util.ArrayList<>(fallback);
        }
        List<ScenarioTestCase.ColumnOverride> list = new java.util.ArrayList<>();
        String[] parts = text.split("[;\\n]+");
        for (String part : parts) {
            if (part.isBlank()) continue;
            String[] kv = part.split("=", 2);
            String left = kv[0].trim();
            String value = kv.length > 1 ? kv[1].trim() : "";
            String sheet = "";
            String col = left;
            if (left.contains(".")) {
                String[] sc = left.split("\\.", 2);
                sheet = sc[0].trim();
                col = sc[1].trim();
            }
            if (col.isEmpty()) continue;
            list.add(new ScenarioTestCase.ColumnOverride(sheet, col, value));
        }
        return list;
    }

    private List<String> suggestedColumnsForSheet(String sheet) {
        if (sheet == null) return List.of();
        String s = sheet.toLowerCase();
        if (s.contains("patient")) {
            return List.of("CFS", "RF", "KPI Type", "KPI Group", "patient_postalcode", "patient_birthdate", "AAC", "Type", "Group");
        }
        if (s.contains("event")) {
            return List.of("event_session_mode1", "event_session_start_date1", "event_session_end_date1",
                    "event_session_venue1", "event_session_capacity1", "purpose_of_contact", "attended_indicator");
        }
        if (s.contains("encounter")) {
            return List.of("encounter_status", "encounter_purpose", "encounter_contacted_staff_name", "encounter_referred_by");
        }
        if (s.contains("questionnaire")) {
            return List.of("q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10", "questionnaire_status");
        }
        if (s.contains("practitioner")) {
            return List.of("practitioner_manpower_position", "practitioner_volunteer_name", "practitioner_manpower_capacity",
                    "practitioner_volunteer_age", "working_remarks");
        }
        if (s.contains("resident_report")) {
            return List.of("social_risk_factor_score", "aap_recommendation", "social_support_recommendation", "cfs", "status");
        }
        if (s.contains("aac_report")) {
            return List.of("extension_total_clients", "status", "author_display");
        }
        if (s.contains("organization_report")) {
            return List.of("organization_type_code", "organization_type_display", "name");
        }
        if (s.contains("location_report")) {
            return List.of("postal_code", "reference");
        }
        if (s.contains("event_report")) {
            return List.of("event_category", "event_type", "target_attendees");
        }
        return List.of();
    }

    private void updateSummary() {
        int count = scenarios != null ? scenarios.size() : 0;
        if (summaryLabel != null) {
            summaryLabel.setText(String.format("%d scenario%s defined", count, count == 1 ? "" : "s"));
        }
    }
}
