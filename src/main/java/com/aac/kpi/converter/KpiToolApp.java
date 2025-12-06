package com.aac.kpi.converter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Simple JavaFX UI wrapper that collects user input and delegates to {@link ReportRunner}.
 */
public class KpiToolApp extends Application {

    private final ReportRunner runner = new ReportRunner();

    @Override
    public void start(Stage stage) {
        stage.setTitle("KPI Tool");

        TextField inputField = new TextField();
        inputField.setPromptText("Input Excel (.xlsx)");
        Button browseInputButton = new Button("Browse");
        browseInputButton.setOnAction(e -> chooseInput(stage, inputField));

        TextField outputField = new TextField();
        outputField.setPromptText("Output folder");
        Button browseOutputButton = new Button("Browse");
        browseOutputButton.setOnAction(e -> chooseOutput(stage, outputField));

        Spinner<Integer> aacSpinner = buildSpinner(0);
        Spinner<Integer> residentSpinner = buildSpinner(0);
        Spinner<Integer> volunteerSpinner = buildSpinner(0);
        Spinner<Integer> eventSpinner = buildSpinner(0);
        Spinner<Integer> organizationSpinner = buildSpinner(0);
        Spinner<Integer> locationSpinner = buildSpinner(0);

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);

        Label statusLabel = new Label("Ready");
        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPrefRowCount(6);

        Button autoReadButton = new Button("Auto read counts");
        autoReadButton.setOnAction(e -> autoReadCounts(stage, inputField, aacSpinner, residentSpinner,
                volunteerSpinner, eventSpinner, organizationSpinner, locationSpinner, statusLabel, outputArea));

        Button generateButton = new Button("Generate JSON");
        generateButton.setOnAction(e -> {
            ReportConfig config;
            try {
                config = new ReportConfig(
                        inputField.getText(),
                        outputField.getText(),
                        aacSpinner.getValue(),
                        residentSpinner.getValue(),
                        volunteerSpinner.getValue(),
                        eventSpinner.getValue(),
                        organizationSpinner.getValue(),
                        locationSpinner.getValue()
                );
            } catch (IllegalArgumentException ex) {
                showError(stage, "Check your inputs", ex.getMessage());
                return;
            }

            runInBackground(stage, config, statusLabel, progressIndicator, outputArea, generateButton);
        });

        GridPane grid = buildGrid(inputField, browseInputButton, outputField, browseOutputButton,
                aacSpinner, residentSpinner, volunteerSpinner, eventSpinner, organizationSpinner, locationSpinner);

        HBox actions = new HBox(10, autoReadButton, generateButton, progressIndicator, statusLabel);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(14,
                new Label("Generate KPI JSON payloads from Excel"),
                grid,
                actions,
                new Label("Output"),
                outputArea
        );
        root.setPadding(new Insets(16));
        VBox.setVgrow(outputArea, Priority.ALWAYS);

        Scene scene = new Scene(root, 900, 520);
        stage.setScene(scene);
        stage.show();
    }

    private GridPane buildGrid(TextField inputField, Button browseInputButton,
                               TextField outputField, Button browseOutputButton,
                               Spinner<Integer> aacSpinner, Spinner<Integer> residentSpinner,
                               Spinner<Integer> volunteerSpinner, Spinner<Integer> eventSpinner,
                               Spinner<Integer> organizationSpinner, Spinner<Integer> locationSpinner) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(140);
        ColumnConstraints growCol = new ColumnConstraints();
        growCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, growCol, new ColumnConstraints());

        grid.addRow(0, new Label("Excel file"), inputField, browseInputButton);
        grid.addRow(1, new Label("Output folder"), outputField, browseOutputButton);
        grid.addRow(2, new Label("AAC reports"), aacSpinner);
        grid.addRow(3, new Label("Resident reports"), residentSpinner);
        grid.addRow(4, new Label("Volunteer attendance"), volunteerSpinner);
        grid.addRow(5, new Label("Event reports"), eventSpinner);
        grid.addRow(6, new Label("Organization reports"), organizationSpinner);
        grid.addRow(7, new Label("Location reports"), locationSpinner);

        return grid;
    }

    private void chooseInput(Stage stage, TextField inputField) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Excel workbook");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel files", "*.xlsx"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            inputField.setText(file.getAbsolutePath());
        }
    }

    private void chooseOutput(Stage stage, TextField outputField) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select output folder");
        File dir = directoryChooser.showDialog(stage);
        if (dir != null) {
            outputField.setText(dir.getAbsolutePath());
        }
    }

    private Spinner<Integer> buildSpinner(int defaultValue) {
        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10_000, defaultValue));
        spinner.setEditable(true);
        spinner.setPrefWidth(140);
        return spinner;
    }

    private void runInBackground(Stage stage, ReportConfig config, Label statusLabel,
                                 ProgressIndicator progressIndicator, TextArea outputArea, Button generateButton) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                runner.generateReports(config);
                return null;
            }
        };

        task.setOnRunning(e -> {
            progressIndicator.setVisible(true);
            generateButton.setDisable(true);
            statusLabel.setText("Generating...");
            outputArea.clear();
        });

        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            generateButton.setDisable(false);
            statusLabel.setText("Completed");
            outputArea.setText("""
                    Reports generated successfully.
                    Root folder: %s
                    Folders created: aac_reports, resident_reports, volunteer_attendance_reports,
                    event_reports, organization_reports, location_reports""".formatted(config.outputFolder()));
        });

        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            generateButton.setDisable(false);
            statusLabel.setText("Failed");
            Throwable ex = task.getException();
            String message = ex != null ? ex.getMessage() : "Unknown error";
            showError(stage, "Generation failed", message);
        });

        Thread thread = new Thread(task, "report-generator");
        thread.setDaemon(true);
        thread.start();
    }

    private void showError(Stage owner, String header, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(owner);
            alert.setTitle("KPI Tool");
            alert.setHeaderText(header);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void autoReadCounts(Stage stage,
                                TextField inputField,
                                Spinner<Integer> aacSpinner,
                                Spinner<Integer> residentSpinner,
                                Spinner<Integer> volunteerSpinner,
                                Spinner<Integer> eventSpinner,
                                Spinner<Integer> organizationSpinner,
                                Spinner<Integer> locationSpinner,
                                Label statusLabel,
                                TextArea outputArea) {
        String inputPath = inputField.getText();
        if (inputPath == null || inputPath.isBlank()) {
            showError(stage, "Input missing", "Select an Excel file before auto-reading counts.");
            return;
        }

        try {
            ExcelOperations ops = new ExcelOperations(inputPath);
            ReportCounts counts = ops.detectCountsFromCommonSheet();
            aacSpinner.getValueFactory().setValue(counts.aac());
            residentSpinner.getValueFactory().setValue(counts.resident());
            volunteerSpinner.getValueFactory().setValue(counts.volunteerAttendance());
            eventSpinner.getValueFactory().setValue(counts.eventReports());
            organizationSpinner.getValueFactory().setValue(counts.organization());
            locationSpinner.getValueFactory().setValue(counts.location());
            statusLabel.setText("Counts loaded");
            outputArea.setText("Detected rows per report table from the common sheet:\n" +
                    "AAC: %d, Resident: %d, Volunteer: %d, Event: %d, Organization: %d, Location: %d"
                            .formatted(counts.aac(), counts.resident(), counts.volunteerAttendance(),
                                    counts.eventReports(), counts.organization(), counts.location()));
        } catch (Exception ex) {
            showError(stage, "Auto-read failed", ex.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
