package com.aac.kpi.controller;

import com.aac.kpi.model.CommonRow;
import com.aac.kpi.model.Encounter;
import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.Practitioner;
import com.aac.kpi.model.QuestionnaireResponse;
import com.aac.kpi.service.AppState;
import com.aac.kpi.service.JsonExportService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class JsonExportController {
    @FXML private TextField jarPathField;
    @FXML private Button jarBrowseButton;
    @FXML private TextField excelPathField;
    @FXML private Button excelBrowseButton;
    @FXML private TextField outputFolderField;
    @FXML private Button outputBrowseButton;

    @FXML private TextField aacCountField;
    @FXML private TextField residentCountField;
    @FXML private TextField volunteerCountField;
    @FXML private TextField eventCountField;
    @FXML private TextField organizationCountField;
    @FXML private TextField locationCountField;

    @FXML private Button autoFillButton;
    @FXML private Button runButton;
    @FXML private Button useCurrentExcelButton;

    @FXML private TextArea logArea;
    @FXML private Label statusLabel;

    private ObservableList<Patient> patients;
    private ObservableList<EventSession> sessions;
    private ObservableList<Practitioner> practitioners;
    private ObservableList<Encounter> encounters;
    private ObservableList<QuestionnaireResponse> questionnaires;
    private ObservableList<CommonRow> commonRows;

    private final FileChooser excelChooser = new FileChooser();
    private final FileChooser jarChooser = new FileChooser();
    private final DirectoryChooser directoryChooser = new DirectoryChooser();

    public void init(ObservableList<Patient> patients,
                     ObservableList<EventSession> sessions,
                     ObservableList<Practitioner> practitioners,
                     ObservableList<Encounter> encounters,
                     ObservableList<QuestionnaireResponse> questionnaires,
                     ObservableList<CommonRow> commonRows) {
        this.patients = patients;
        this.sessions = sessions;
        this.practitioners = practitioners;
        this.encounters = encounters;
        this.questionnaires = questionnaires;
        this.commonRows = commonRows;
        directoryChooser.setTitle("Select output folder");
        excelChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"));
        excelChooser.setTitle("Select KPI Excel");
        jarChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JAR Files", "*.jar"));
        jarChooser.setTitle("Select converter JAR");
        updateCountsFromData();
    }

    @FXML
    private void initialize() {
        logArea.setEditable(false);
        logArea.setWrapText(true);
        statusLabel.setText("Ready to export KPI JSON");
        setDefaultCounts();
        setDefaultJarPath();
    }

    public void setExcelPath(File file) {
        if (excelPathField == null) return;
        String value = file != null ? file.getAbsolutePath() : "";
        Platform.runLater(() -> excelPathField.setText(value));
    }

    @FXML
    private void onBrowseExcel() {
        File file = excelChooser.showOpenDialog(excelPathField.getScene().getWindow());
        if (file != null) {
            excelPathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void onBrowseJar() {
        File file = jarChooser.showOpenDialog(jarPathField.getScene().getWindow());
        if (file != null) {
            jarPathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void onUseCurrentExcel() {
        File current = AppState.getCurrentExcelFile();
        if (current != null) {
            excelPathField.setText(current.getAbsolutePath());
        }
    }

    @FXML
    private void onBrowseOutput() {
        File folder = directoryChooser.showDialog(outputFolderField.getScene().getWindow());
        if (folder != null) {
            outputFolderField.setText(folder.getAbsolutePath());
        }
    }

    @FXML
    private void onAutoFillCounts() {
        try {
            File excel = requireFile(excelPathField.getText(), "Excel input file");
            fillCountsFromExcelFile(excel);
            appendLog("Counts synced from " + excel.getName());
        } catch (IllegalArgumentException | IOException ex) {
            showAlert("Auto-fill failed: " + ex.getMessage());
        }
    }

    @FXML
    private void onRunExport() {
        try {
            File excel = requireFile(excelPathField.getText(), "Excel input file");
            File outputFolder = requireDirectory(outputFolderField.getText(), "Output folder");
            File jarFile = requireFile(jarPathField.getText(), "Converter JAR");

            int aacCount = parseCountField(aacCountField.getText(), "AAC count");
            int residentCount = parseCountField(residentCountField.getText(), "Resident count");
            int volunteerCount = parseCountField(volunteerCountField.getText(), "Volunteer count");
            int eventCount = parseCountField(eventCountField.getText(), "Event count");
            int organizationCount = parseCountField(organizationCountField.getText(), "Organization count");
            int locationCount = parseCountField(locationCountField.getText(), "Location count");

            try {
                fillCountsFromExcelFile(excel);
            } catch (IOException ioe) {
                showAlert("Unable to read Excel for counts: " + ioe.getMessage());
                return;
            }

            AppState.setJsonConverterJarPath(jarFile.getAbsolutePath());

            Task<JsonExportService.Result> task = new Task<>() {
                @Override
                protected JsonExportService.Result call() throws Exception {
                    updateMessage("Running JSON converter…");
                    JsonExportService.Result result = JsonExportService.run(
                            excel,
                            outputFolder,
                            aacCount,
                            residentCount,
                            volunteerCount,
                            eventCount,
                            organizationCount,
                            locationCount);
                    updateMessage("Completed with exit code " + result.getExitCode());
                    return result;
                }
            };

            task.setOnRunning(e -> {
                runButton.setDisable(true);
                autoFillButton.setDisable(true);
                statusLabel.setText("Running JSON conversion...");
            });
            task.setOnSucceeded(e -> {
                JsonExportService.Result result = task.getValue();
                appendLog("Command: " + result.getCommand());
                appendLog(result.getOutput());
                if (result.isSuccess()) {
                    statusLabel.setText("JSON export succeeded");
                } else {
                    statusLabel.setText("JSON export failed (exit " + result.getExitCode() + ")");
                    showAlert("JSON converter exited with code " + result.getExitCode());
                }
                runButton.setDisable(false);
                autoFillButton.setDisable(false);
            });
            task.setOnFailed(e -> {
                Throwable t = task.getException();
                appendLog("Error: " + (t != null ? t.getMessage() : "unknown"));
                statusLabel.setText("JSON export failed");
                showAlert("JSON export failed: " + (t == null ? "unknown error" : t.getMessage()));
                runButton.setDisable(false);
                autoFillButton.setDisable(false);
            });

            new Thread(task, "JsonExportRunner").start();
        } catch (IllegalArgumentException ex) {
            showAlert(ex.getMessage());
        }
    }

    private File requireFile(String path, String name) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        File file = new File(path.trim());
        if (!file.isFile()) {
            throw new IllegalArgumentException(name + " must point to an existing file.");
        }
        return file;
    }

    private File requireDirectory(String path, String name) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        File dir = new File(path.trim());
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(name + " must be an existing folder.");
        }
        return dir;
    }

    private int parseCountField(String value, String label) {
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (Exception e) {
            throw new IllegalArgumentException(label + " must be a non-negative integer.");
        }
    }

    private void updateCountsFromData() {
        String path = excelPathField.getText();
        if (path != null && !path.isBlank()) {
            File excel = new File(path.trim());
            if (excel.isFile()) {
                try {
                    fillCountsFromExcelFile(excel);
                    return;
                } catch (IOException ignored) {
                    // fall through to defaults
                }
            }
        }
        setDefaultCounts();
    }

    private void setDefaultCounts() {
        if (aacCountField != null && aacCountField.getText().isBlank()) aacCountField.setText("5");
        if (residentCountField != null && residentCountField.getText().isBlank()) residentCountField.setText("100");
        if (volunteerCountField != null && volunteerCountField.getText().isBlank()) volunteerCountField.setText("1");
        if (eventCountField != null && eventCountField.getText().isBlank()) eventCountField.setText("1404");
        if (organizationCountField != null && organizationCountField.getText().isBlank()) organizationCountField.setText("5");
        if (locationCountField != null && locationCountField.getText().isBlank()) locationCountField.setText("6");
    }

    private void setDefaultJarPath() {
        String path = AppState.getJsonConverterJarPath();
        if (jarPathField != null && (jarPathField.getText() == null || jarPathField.getText().isBlank())) {
            jarPathField.setText(path == null ? "" : path);
        }
    }

    private void fillCountsFromExcelFile(File excel) throws IOException {
        var counts = countCommonSections(excel);
        if (aacCountField != null) aacCountField.setText(String.valueOf(Math.max(0, counts.getOrDefault("aac_report", 0))));
        if (residentCountField != null) residentCountField.setText(String.valueOf(Math.max(0, counts.getOrDefault("resident_report", 0))));
        if (volunteerCountField != null) volunteerCountField.setText(String.valueOf(Math.max(0, counts.getOrDefault("volunteer_attendance_report", 0))));
        if (eventCountField != null) eventCountField.setText(String.valueOf(Math.max(0, counts.getOrDefault("event_report", 0))));
        if (organizationCountField != null) organizationCountField.setText(String.valueOf(Math.max(0, counts.getOrDefault("organization_report", 0))));
        if (locationCountField != null) locationCountField.setText(String.valueOf(Math.max(0, counts.getOrDefault("location_report", 0))));
    }

    private java.util.Map<String, Integer> countCommonSections(File excel) throws IOException {
        // Lower the inflate ratio to allow slightly more compressed XLSX files without tripping the zip-bomb guard
        ZipSecureFile.setMinInflateRatio(0.001d);
        try (FileInputStream fis = new FileInputStream(excel);
             XSSFWorkbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheet("Common");
            java.util.Map<String, Integer> counts = new java.util.HashMap<>();
            if (sheet == null) return counts;
            java.util.Set<String> sections = java.util.Set.of(
                    "aac_report",
                    "resident_report",
                    "volunteer_attendance_report",
                    "event_report",
                    "organization_report",
                    "location_report"
            );
            String currentSection = null;
            boolean skipHeader = false;
            for (Row row : sheet) {
                Cell first = row.getCell(0);
                String text = first != null ? first.toString().trim() : "";
                if (!text.isBlank() && sections.contains(text)) {
                    currentSection = text;
                    skipHeader = true;
                    continue;
                }
                if (currentSection == null) continue;
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }
                if (isRowEmpty(row)) continue;
                counts.merge(currentSection, 1, Integer::sum);
            }
            return counts;
        }
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell == null) continue;
            if (cell.getCellType() == CellType.BLANK) continue;
            if (!cell.toString().isBlank()) return false;
        }
        return true;
    }

    private void appendLog(String text) {
        Platform.runLater(() -> logArea.appendText(text + System.lineSeparator()));
    }

    private void showAlert(String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR, text, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

}
