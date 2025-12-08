package com.aac.kpi;

import com.aac.kpi.controller.EventSessionController;
import com.aac.kpi.controller.JsonExportController;
import com.aac.kpi.controller.JsonCsvController;
import com.aac.kpi.controller.KpiRegistrationDialog;
import com.aac.kpi.controller.MasterDataController;
import com.aac.kpi.controller.PatientMasterController;
import com.aac.kpi.controller.ReportingFieldsDialog;
import com.aac.kpi.controller.ScenarioBuilderController;
import com.aac.kpi.model.CommonRow;
import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.ScenarioTestCase;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.Practitioner;
import com.aac.kpi.service.ExcelReader;
import com.aac.kpi.service.ExcelWriter;
import com.aac.kpi.service.LinkService;
import com.aac.kpi.service.MasterDataService;
import com.aac.kpi.service.AppState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainController {
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab patientTab;
    @FXML
    private Tab eventTab;
    @FXML
    private Tab practitionerTab;
    @FXML
    private Tab encounterTab;
    @FXML
    private Tab questionnaireTab;
    @FXML
    private Tab commonTab;
    @FXML
    private Tab scenarioTab;
    @FXML
    private Tab jsonTab;
    @FXML
    private Tab jsonCsvTab;
    @FXML
    private Tab userGuideTab;
    @FXML
    private Tab masterDataTab;
    @FXML
    private Tab aiTab;
    @FXML
    private Label statusLabel;

    private final ObservableList<Patient> patients = FXCollections.observableArrayList();
    private final ObservableList<EventSession> sessions = FXCollections.observableArrayList();
    private final ObservableList<com.aac.kpi.model.Practitioner> practitioners = FXCollections.observableArrayList();
    private final ObservableList<com.aac.kpi.model.Encounter> encounters = FXCollections.observableArrayList();
    private final ObservableList<com.aac.kpi.model.QuestionnaireResponse> questionnaires = FXCollections
            .observableArrayList();
    private final ObservableList<CommonRow> commonRows = FXCollections.observableArrayList();
    private final ObservableList<ScenarioTestCase> scenarios = FXCollections.observableArrayList();

    private PatientMasterController patientController;
    private EventSessionController eventController;
    private com.aac.kpi.controller.PractitionerMasterController practitionerController;
    private com.aac.kpi.controller.EncounterMasterController encounterController;
    private com.aac.kpi.controller.QuestionnaireResponseMasterController questionnaireController;
    private com.aac.kpi.controller.CommonController commonController;
    private JsonExportController jsonController;
    private JsonCsvController jsonCsvController;
    private MasterDataController masterDataController;
    private ScenarioBuilderController scenarioController;
    private com.aac.kpi.controller.AiOverlayController aiOverlayController;

    @FXML
    private void initialize() throws IOException {
        MasterDataService.MasterData masterData = ensureMasterData();
        updatePractitionersFromMaster(masterData);
        // Load Patient Master view
        FXMLLoader pLoader = new FXMLLoader(getClass().getResource("/com/aac/kpi/PatientMasterView.fxml"));
        Node pRoot = pLoader.load();
        patientController = pLoader.getController();
        patientController.init(patients, sessions, practitioners, statusLabel);
        patientController.setClearAllHandler(this::clearAllSheets);
        patientTab.setContent(pRoot);

        // Load Event Session view
        FXMLLoader eLoader = new FXMLLoader(getClass().getResource("/com/aac/kpi/EventSessionView.fxml"));
        Node eRoot = eLoader.load();
        eventController = eLoader.getController();
        eventController.init(patients, sessions, statusLabel);
        eventController.setClearAllHandler(this::clearAllSheets);
        eventTab.setContent(eRoot);

        // Load Practitioner Master view
        FXMLLoader prLoader = new FXMLLoader(getClass().getResource("/com/aac/kpi/PractitionerMasterView.fxml"));
        Node prRoot = prLoader.load();
        practitionerController = prLoader.getController();
        practitionerController.init(practitioners, patients, sessions, statusLabel);
        practitionerController.setClearAllHandler(this::clearAllSheets);
        practitionerTab.setContent(prRoot);

        // Load Encounter Master view
        FXMLLoader enLoader = new FXMLLoader(getClass().getResource("/com/aac/kpi/EncounterMasterView.fxml"));
        Node enRoot = enLoader.load();
        encounterController = enLoader.getController();
        encounterController.init(encounters, patients, sessions, practitioners, statusLabel);
        encounterController.setClearAllHandler(this::clearAllSheets);
        encounterTab.setContent(enRoot);

        // Load QuestionnaireResponse Master view
        FXMLLoader qLoader = new FXMLLoader(
                getClass().getResource("/com/aac/kpi/QuestionnaireResponseMasterView.fxml"));
        Node qRoot = qLoader.load();
        questionnaireController = qLoader.getController();
        questionnaireController.init(questionnaires, patients, sessions, practitioners, encounters, statusLabel);
        questionnaireController.setClearAllHandler(this::clearAllSheets);
        questionnaireTab.setContent(qRoot);

        // Load Common view
        FXMLLoader cLoader = new FXMLLoader(getClass().getResource("/com/aac/kpi/CommonView.fxml"));
        Node cRoot = cLoader.load();
        commonController = cLoader.getController();
        commonController.init(commonRows, patients, sessions, encounters, practitioners, questionnaires, statusLabel);
        commonController.setClearAllHandler(this::clearAllSheets);
        commonTab.setContent(cRoot);

        FXMLLoader scenarioLoader = new FXMLLoader(getClass().getResource("/com/aac/kpi/ScenarioBuilderView.fxml"));
        Node scenarioRoot = scenarioLoader.load();
        scenarioController = scenarioLoader.getController();
        scenarioController.init(scenarios);
        scenarioController.setOnGenerateExcel(this::onGenerateFromScenariosAndExport);
        scenarioTab.setContent(scenarioRoot);

        FXMLLoader jsonLoader = new FXMLLoader(getClass().getResource("/com/aac/kpi/JsonExportView.fxml"));
        Node jsonRoot = jsonLoader.load();
        jsonController = jsonLoader.getController();
        jsonController.init(patients, sessions, practitioners, encounters, questionnaires, commonRows);
        jsonTab.setContent(jsonRoot);
        FXMLLoader jsonCsvLoader = new FXMLLoader(getClass().getResource("/com/aac/kpi/JsonCsvView.fxml"));
        Node jsonCsvRoot = jsonCsvLoader.load();
        jsonCsvController = jsonCsvLoader.getController();
        jsonCsvTab.setContent(jsonCsvRoot);

        FXMLLoader aiLoader = new FXMLLoader(getClass().getResource("/com/aac/kpi/AiOverlayView.fxml"));
        Node aiRoot = aiLoader.load();
        aiOverlayController = aiLoader.getController();
        aiOverlayController.init(patients, sessions, encounters, questionnaires, commonRows, scenarios);
        aiTab.setContent(aiRoot);

        FXMLLoader mdLoader = new FXMLLoader(getClass().getResource("/com/aac/kpi/MasterDataView.fxml"));
        Node mdRoot = mdLoader.load();
        masterDataController = mdLoader.getController();
        masterDataController.setMasterData(AppState.getMasterData());
        masterDataController.setOnRegenerate(() -> updatePractitionersFromMaster(AppState.getMasterData()));
        masterDataTab.setContent(mdRoot);
        userGuideTab.setContent(createUserGuideContent());

        updateStatus();
    }

    private File ensureFileName(File file) {
        if (file == null) return null;
        String name = file.getName();
        if (!name.toLowerCase().endsWith(".xlsx")) {
            return new File(file.getParentFile(), name + ".xlsx");
        }
        return file;
    }

    private String suggestedFileName() {
        String kpiType = AppState.getScenarioSheetKpiType();
        if (kpiType == null || kpiType.isBlank()) {
            kpiType = AppState.getRegistrationOverrideType();
        }
        if (kpiType == null || kpiType.isBlank()) {
            kpiType = "Robust";
        }
        String safeKpi = kpiType.replaceAll("[^A-Za-z0-9_-]", "");
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_DATE);
        return "KPI_" + safeKpi + "_" + date + ".xlsx";
    }

    @FXML
    private void onUploadExcel() {
        Window owner = tabPane.getScene() != null ? tabPane.getScene().getWindow() : null;
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File f = fc.showOpenDialog(owner);
        if (f == null)
            return;
        try {
            List<Patient> p = ExcelReader.readPatients(f);
            List<EventSession> s = ExcelReader.readEventSessions(f);
            List<com.aac.kpi.model.Practitioner> pr = ExcelReader.readPractitioners(f);
            List<com.aac.kpi.model.Encounter> en = ExcelReader.readEncounters(f);
            List<com.aac.kpi.model.QuestionnaireResponse> qs = ExcelReader.readQuestionnaires(f);
            if (!p.isEmpty())
                patients.setAll(p);
            if (!s.isEmpty())
                sessions.setAll(s);
            if (!pr.isEmpty())
                practitioners.setAll(pr);
            if (!en.isEmpty())
                encounters.setAll(en);
            if (!qs.isEmpty())
                questionnaires.setAll(qs);
            AppState.setCurrentExcelFile(f);
            AppState.setDirty(false);
            LinkService.fillPatientAttendedRefs(patients, sessions);
            patientController.refreshTable();
            eventController.refreshTable();
            practitionerController.refreshTable();
            encounterController.refreshTable();
            questionnaireController.refreshTable();
            updateStatus();
            commonController.refreshTable();
            if (jsonController != null)
                jsonController.setExcelPath(f);
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to load: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void onExportExcelAs() {
        try {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
            fc.setInitialFileName(suggestedFileName());
            File chosen = fc.showSaveDialog(tabPane.getScene().getWindow());
            if (chosen == null)
                return;
            var regCfg = promptRegistrationConfig();
            if (regCfg.isEmpty() && !AppState.isScenarioSkipPrompts())
                return;
            var reporting = promptReportingFields();
            if (reporting.isEmpty() && !AppState.isScenarioSkipPrompts())
                return;
            regCfg.ifPresent(this::applyRegistrationConfig);
            // Prompt for volunteer_attendance_report practitioner count
            int total = practitioners != null ? practitioners.size() : 0;
            if (total > 0) {
                TextInputDialog dlg = new TextInputDialog(String.valueOf(Math.min(total, 10)));
                dlg.setTitle("Volunteer Attendance");
                dlg.setHeaderText(null);
                dlg.setContentText(String.format("Enter number of practitioners (max %d):", total));
                java.util.Optional<String> res = dlg.showAndWait();
                if (res.isEmpty())
                    return; // cancelled
                int n;
                try {
                    n = Integer.parseInt(res.get().trim());
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Please enter a valid integer.", ButtonType.OK).showAndWait();
                    return;
                }
                if (n < 1 || n > total) {
                    new Alert(Alert.AlertType.ERROR, "Total number of practitioners is " + total
                            + ". Please choose a number between 1 and " + total + ".", ButtonType.OK).showAndWait();
                    return;
                }
                AppState.setVolunteerPractitionerCount(n);
            } else {
                AppState.setVolunteerPractitionerCount(0);
            }
            java.util.List<com.aac.kpi.model.CommonRow> commons = (questionnaireController != null
                    && commonController != null) ? commonControllerItems() : java.util.List.of();
            reporting.ifPresent(fields -> applyReportingFields(commons, fields));
            regCfg.ifPresent(this::applyRegistrationConfig);
            File file = ExcelWriter.saveToExcel(patients, sessions, practitioners, encounters, questionnaires, commons,
                    chosen);
            AppState.setCurrentExcelFile(file);
            AppState.setDirty(false);
            new Alert(Alert.AlertType.INFORMATION, "Exported to: " + file.getAbsolutePath(), ButtonType.OK)
                    .showAndWait();
            statusLabel.setText(String.format(
                    "Generated %d patients | %d sessions | %d practitioners | %d encounters | %d questionnaires | Last export: %s",
                    patients.size(), sessions.size(), practitioners.size(), encounters.size(), questionnaires.size(),
                    ExcelWriter.nowStamp()));
            if (jsonController != null)
                jsonController.setExcelPath(file);
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    /**
     * Scenario Builder entry point: generate all masters from the configured
     * scenarios, then export to Excel in one shot.
     */
    private void onGenerateFromScenariosAndExport() {
        if (scenarios.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No scenarios defined. Please add at least one scenario first.",
                    ButtonType.OK).showAndWait();
            return;
        }
        try {
            MasterDataService.MasterData masterData = ensureMasterData();
            var result = com.aac.kpi.service.ScenarioGenerationService.generate(new ArrayList<>(scenarios));

            patients.setAll(result.patients);
            sessions.setAll(result.sessions);
            practitioners.setAll(result.practitioners);
            encounters.setAll(result.encounters);
            questionnaires.setAll(result.questionnaires);
            commonRows.setAll(result.commonRows);

            LinkService.fillPatientAttendedRefs(patients, sessions);

            patientController.refreshTable();
            eventController.refreshTable();
            practitionerController.refreshTable();
            encounterController.refreshTable();
            questionnaireController.refreshTable();
            commonController.refreshTable();

            // After data is in place, run the usual export flow (Save As)
            onExportExcelAs();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Scenario generation failed: " + ex.getMessage(), ButtonType.OK)
                    .showAndWait();
        }
    }

    @FXML
    private void onSaveExcel() {
        try {
            File dest = AppState.getCurrentExcelFile();
            if (dest == null) {
                onExportExcelAs();
                return;
            }
            var regCfg = promptRegistrationConfig();
            if (regCfg.isEmpty() && !AppState.isScenarioSkipPrompts())
                return;
            var reporting = promptReportingFields();
            if (reporting.isEmpty() && !AppState.isScenarioSkipPrompts())
                return;
            regCfg.ifPresent(this::applyRegistrationConfig);
            // Prompt for volunteer_attendance_report practitioner count
            int total = practitioners != null ? practitioners.size() : 0;
            if (total > 0) {
                TextInputDialog dlg = new TextInputDialog(String.valueOf(Math.min(total, 10)));
                dlg.setTitle("Volunteer Attendance");
                dlg.setHeaderText(null);
                dlg.setContentText(String.format("Enter number of practitioners (max %d):", total));
                java.util.Optional<String> res = dlg.showAndWait();
                if (res.isEmpty())
                    return; // cancelled
                int n;
                try {
                    n = Integer.parseInt(res.get().trim());
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Please enter a valid integer.", ButtonType.OK).showAndWait();
                    return;
                }
                if (n < 1 || n > total) {
                    new Alert(Alert.AlertType.ERROR, "Total number of practitioners is " + total
                            + ". Please choose a number between 1 and " + total + ".", ButtonType.OK).showAndWait();
                    return;
                }
                AppState.setVolunteerPractitionerCount(n);
            } else {
                AppState.setVolunteerPractitionerCount(0);
            }
            java.util.List<com.aac.kpi.model.CommonRow> commons = (questionnaireController != null
                    && commonController != null) ? commonControllerItems() : java.util.List.of();
            reporting.ifPresent(fields -> applyReportingFields(commons, fields));
            regCfg.ifPresent(this::applyRegistrationConfig);
            File file = ExcelWriter.saveToExcel(patients, sessions, practitioners, encounters, questionnaires, commons,
                    ensureFileName(dest));
            AppState.setDirty(false);
            new Alert(Alert.AlertType.INFORMATION, "Saved: " + file.getAbsolutePath(), ButtonType.OK).showAndWait();
            statusLabel.setText(String.format(
                    "Generated %d patients | %d sessions | %d practitioners | %d encounters | %d questionnaires | Last export: %s",
                    patients.size(), sessions.size(), practitioners.size(), encounters.size(), questionnaires.size(),
                    ExcelWriter.nowStamp()));
            if (jsonController != null)
                jsonController.setExcelPath(file);
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Save failed: " + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private java.util.List<com.aac.kpi.model.CommonRow> commonControllerItems() {
        // ask controller for its current items; if controller ever null, return empty
        try {
            java.lang.reflect.Method m = commonController.getClass().getMethod("getItems");
            Object v = m.invoke(commonController);
            if (v instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<com.aac.kpi.model.CommonRow> list = (java.util.List<com.aac.kpi.model.CommonRow>) v;
                return list;
            }
        } catch (Exception ignored) {
        }
        return java.util.List.of();
    }

    private java.util.Optional<ReportingFieldsDialog.ReportingFields> promptReportingFields() {
        if (AppState.isScenarioSkipPrompts()) {
            return java.util.Optional.empty();
        }
        return ReportingFieldsDialog.prompt(AppState.getReportingMonthOverride(), AppState.getReportDateOverride());
    }

    private void applyReportingFields(List<CommonRow> rows, ReportingFieldsDialog.ReportingFields fields) {
        if (rows == null || fields == null) return;
        for (CommonRow row : rows) {
            if (row == null) continue;
            row.setReportingMonth(fields.reportingMonth());
            row.setLastUpdated(fields.reportDate());
        }
        AppState.setReportingMonthOverride(fields.reportingMonth());
        AppState.setReportDateOverride(fields.reportDate());
        if (commonController != null) commonController.refreshTable();
    }

    private java.util.Optional<KpiRegistrationDialog.RegistrationSelection> promptRegistrationConfig() {
        if (AppState.isScenarioSkipPrompts()) {
            return defaultRegistrationSelection();
        }
        return KpiRegistrationDialog.prompt(
                AppState.getRobustRegistrationCount(),
                AppState.getFrailRegistrationCount(),
                AppState.getBuddingRegistrationCount(),
                AppState.getBefriendingRegistrationCount()
        );
    }

    private void applyRegistrationConfig(KpiRegistrationDialog.RegistrationSelection sel) {
        if (sel == null || sel.config() == null) return;
        var cfg = sel.config();
        AppState.setRobustRegistrationCount(cfg.robust());
        AppState.setFrailRegistrationCount(cfg.frail());
        AppState.setBuddingRegistrationCount(cfg.budding());
        AppState.setBefriendingRegistrationCount(cfg.befriending());
        AppState.setRegistrationOverrideType(sel.selectedType() == null ? "" : sel.selectedType());
    }

    @FXML
    private void onOpenKpiSettings() {
        var dlg = new com.aac.kpi.controller.KpiSettingsDialog();
        var res = dlg.showAndWait();
        res.ifPresent(cfg -> {
            com.aac.kpi.service.AppState.setKpiConfig(cfg);
            // Optionally trigger recompute so UI reflects new thresholds
            // Do not mark dirty; thresholds are not data edits
        });
    }

    private java.util.Optional<KpiRegistrationDialog.RegistrationSelection> defaultRegistrationSelection() {
        var cfg = new KpiRegistrationDialog.RegistrationConfig(
                AppState.getRobustRegistrationCount(),
                AppState.getFrailRegistrationCount(),
                AppState.getBuddingRegistrationCount(),
                AppState.getBefriendingRegistrationCount()
        );
        return java.util.Optional.of(new KpiRegistrationDialog.RegistrationSelection(
                AppState.getRegistrationOverrideType(), cfg));
    }

    @FXML
    private void onExit() {
        statusLabel.getScene().getWindow().hide();
    }

    private void updateStatus() {
        String dirty = AppState.isDirty() ? " (unsaved)" : "";
        statusLabel.setText(String.format(
                "Generated %d patients | %d sessions | %d practitioners | %d encounters | %d questionnaires%s | Last export: ",
                patients.size(), sessions.size(), practitioners.size(), encounters.size(), questionnaires.size(),
                dirty));
    }

    private void clearAllSheets() {
        patients.clear();
        sessions.clear();
        practitioners.clear();
        encounters.clear();
        questionnaires.clear();
        commonRows.clear();
        scenarios.clear();
        AppState.setCurrentExcelFile(null);
        AppState.setDirty(true);
        AppState.setScenarioSkipPrompts(false);
        AppState.clearSkipBuddyingDerive();
        AppState.setScenarioSheetName("");
        AppState.setScenarioSheetKpiType("");
        patientController.refreshTable();
        eventController.refreshTable();
        practitionerController.refreshTable();
        encounterController.refreshTable();
        questionnaireController.refreshTable();
        commonController.refreshTable();
        if (scenarioController != null)
            scenarioController.resetForm();
        updateStatus();
    }

    private void updatePractitionersFromMaster(MasterDataService.MasterData masterData) {
        if (masterData == null)
            return;
        int target = patients != null ? patients.size() : 0;
        if (target == 0) {
            practitioners.clear();
            if (practitionerController != null)
                practitionerController.refreshTable();
            return;
        }
        List<Practitioner> list = new ArrayList<>();
        List<MasterDataService.Volunteer> vols = masterData.getVolunteers();
        for (int i = 0; i < target; i++) {
            Practitioner p = new Practitioner();
            if (i < vols.size()) {
                MasterDataService.Volunteer vol = vols.get(i);
                p.setPractitionerId(vol.volunteerId());
                p.setPractitionerIdentifierValue(vol.volunteerId());
                p.setPractitionerIdentifierSystem("http://ihis.sg/identifier/aac-staff-id");
                p.setPractitionerManpowerPosition(vol.volunteerRole());
                p.setPractitionerVolunteerName(vol.volunteerName());
                p.setPractitionerManpowerCapacity(0.8);
                p.setPractitionerVolunteerAge(30);
                p.setWorkingRemarks("Synced from master");
            } else {
                // Fallback generation when master data has fewer volunteers than patients
                p.setPractitionerId(com.aac.kpi.service.RandomDataUtil.uuid32());
                p.setPractitionerIdentifierValue(com.aac.kpi.service.RandomDataUtil.uuid32());
                p.setPractitionerIdentifierSystem("http://ihis.sg/identifier/aac-staff-id");
                p.setPractitionerManpowerPosition("Volunteer");
                p.setPractitionerVolunteerName(com.aac.kpi.service.RandomDataUtil.randomVolunteerName());
                p.setPractitionerManpowerCapacity(0.8);
                p.setPractitionerVolunteerAge(com.aac.kpi.service.RandomDataUtil.randomInt(25, 65));
                p.setWorkingRemarks("Auto-generated to match patient count");
            }
            list.add(p);
        }
        practitioners.setAll(list);
        if (practitionerController != null)
            practitionerController.refreshTable();
    }

    private MasterDataService.MasterData ensureMasterData() {
        MasterDataService.MasterData data = AppState.getMasterData();
        if (data == null) {
            data = MasterDataService.generate();
            AppState.setMasterData(data);
        }
        return data;
    }

    private Node createUserGuideContent() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);
        Label heading = new Label("User Guide & Tab Flow");
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label intro = new Label(
                "Each tab maps to a specific sheet or export path. This guide explains what logic runs there, whether the data is dummy or sourced from uploaded Excel, and how the sheets feed the final exports.");
        intro.setWrapText(true);
        intro.setMaxWidth(980);
        root.getChildren().addAll(heading, intro);
        root.getChildren().addAll(
                createGuideSection(
                        "Patient Master",
                        "Primary patient roster. Each row holds demographics, postal codes, KPI buckets, and the attended session references that mirror Event Session rows.",
                        "Data comes from the Patient Master sheet of any uploaded workbook (ExcelReader.readPatients) or from dummy generation (Generate Patients dialog, RandomDataUtil + NRICGeneratorUtil).",
                        "Edits mark AppState dirty, Analyze uses KpiService.computeForFY, and LinkService keeps attendedEventReferences synced with sessions."),
                createGuideSection(
                        "Event Session",
                        "Event sessions detail mode, venue, capacity, timestamps, and attended flags tied to patients.",
                        "Read from the Event Session sheet of an uploaded Excel workbook or generated by the KPI-type-aware generator (RandomDataUtil) with Befriending/Buddying rules.",
                        "Mutations call LinkService.fillPatientAttendedRefs so patients always show the most recent attendance data."),
                createGuideSection(
                        "Practitioner Master",
                        "Volunteer/practitioner metadata used in exports and attendance reports.",
                        "Initially seeded from MasterDataService.generate() (visible in the Master Data tab), but you can regenerate, upload a Practitioner sheet, or append dummy volunteers via RandomDataUtil.",
                        "Edited rows are exported directly, and Analyze validates duplicate IDs, age, and capacity."),
                createGuideSection(
                        "Encounter Master",
                        "Encounters cross-reference patients, staff, and referral context. Start times must follow ISO+08:00 and status defaults to finished.",
                        "Uploaded from the Encounter Master sheet or produced by the Generate Encounters dialog (uses RandomDataUtil-derived staff, venues, and masks).",
                        "Validation enforces 32-char IDs, finished status, and time formatting while generation pulls IDs from patients and practitioners."),
                createGuideSection(
                        "QuestionnaireResponse Master",
                        "Holds the 10-question KPIs per resident along with status and patient linkage.",
                        "Loaded from the QuestionnaireResponse Master sheet or seeded through Generate Questionnaires (RandomDataUtil).",
                        "Analyze ensures 32-char IDs, completed status, alternating date/score answers, and references back to the relevant patient."),
                createGuideSection(
                        "Common",
                        "Aggregates all other sheets into CommonRow compositions that the Excel writer and JSON exporter consume.",
                        "Built on demand via CommonBuilderService using Patient, Event, Encounter, Questionnaire, and Practitioner lists; you may also upload a pre-made Common sheet for overrides.",
                        "Dialogs prompt for CFS bucket, social risk label, and event report label before building; exported rows keep patient/encounter/questionnaire references aligned."),
                createGuideSection(
                        "KPI JSON",
                        "Runs the external JSON converter to turn the current KPI workbook into FHIR bundles.",
                        "Points at the current Excel file (AppState keeps the most recently exported path) or any user-selected workbook; counts can be auto-filled from that sheet.",
                        "You supply the converter JAR and output folder, then JsonExportService.run is executed in a background task while the log area captures stdout/stderr."),
                createGuideSection(
                        "JSON → CSV",
                        "Post-processes exported JSON folders into a compact CSV of AAC IDs and raw resources.",
                        "Reads directories produced by the KPI JSON export (or any other JSON bundles) with Gson.",
                        "Traverses each JSON to find AAC identifiers, quotes both values, normalizes newline characters, and logs every generated CSV file."),
                createGuideSection(
                        "Master Data",
                        "Read-only view of AAC center → organization → volunteer rows.",
                        "MasterDataService.generate() supplies fake AAC/organization/location/volunteer entries; MainController copies volunteers into Practitioner Master automatically.",
                        "Use Regenerate to refresh the cached master data and rerun updatePractitionersFromMaster."));
        root.getChildren().add(new Separator());
        Label diagramHeading = new Label("Mind map of sheet connections");
        diagramHeading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        root.getChildren().addAll(diagramHeading, createMindMapDiagram());
        root.getChildren().add(createExplanationSection());
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent;");
        return scroll;
    }

    private Node createGuideSection(String title, String description, String dataNote, String logicNote) {
        VBox section = new VBox(6);
        section.setPadding(new Insets(10));
        section.setStyle(
                "-fx-background-color: #f7f7f7; -fx-border-color: #dfe3ea; -fx-border-radius: 8; -fx-background-radius: 8;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        Label descriptionLabel = new Label(description);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(940);
        Label dataLabel = new Label("Data: " + dataNote);
        dataLabel.setWrapText(true);
        dataLabel.setMaxWidth(940);
        dataLabel.setStyle("-fx-text-fill: #444;");
        Label logicLabel = new Label("Logic: " + logicNote);
        logicLabel.setWrapText(true);
        logicLabel.setMaxWidth(940);
        logicLabel.setStyle("-fx-text-fill: #444;");
        section.getChildren().addAll(titleLabel, descriptionLabel, dataLabel, logicLabel);
        return section;
    }

    private Node createMindMapDiagram() {
        Canvas canvas = new Canvas(820, 320);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFont(Font.font("System", 12));
        drawMindMap(gc);
        StackPane wrapper = new StackPane(canvas);
        wrapper.setPadding(new Insets(12));
        wrapper.setStyle(
                "-fx-background-color: white; -fx-border-color: #c5cdd5; -fx-border-radius: 12; -fx-background-radius: 12;");
        return wrapper;
    }

    private void drawMindMap(GraphicsContext gc) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        gc.setStroke(Color.web("#607d8b"));
        gc.setLineWidth(1.6);
        List<DiagramNode> nodes = List.of(
                new DiagramNode("Master Data", 120, 250),
                new DiagramNode("Patient Master", 170, 80),
                new DiagramNode("Event Session", 340, 40),
                new DiagramNode("Practitioner Master", 520, 40),
                new DiagramNode("Encounter Master", 340, 160),
                new DiagramNode("Questionnaire\nResponse Master", 520, 160),
                new DiagramNode("Common\nAggregator", 660, 110),
                new DiagramNode("KPI JSON\nExport", 340, 260),
                new DiagramNode("JSON → CSV\nConverter", 520, 260));
        int[][] edges = {
                { 0, 3 },
                { 1, 6 },
                { 2, 6 },
                { 3, 6 },
                { 4, 6 },
                { 5, 6 },
                { 6, 7 },
                { 7, 8 }
        };
        for (int[] edge : edges) {
            DiagramNode from = nodes.get(edge[0]);
            DiagramNode to = nodes.get(edge[1]);
            gc.strokeLine(from.x, from.y, to.x, to.y);
        }
        double boxWidth = 150;
        double boxHeight = 44;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        for (DiagramNode node : nodes) {
            double x0 = node.x - boxWidth / 2.0;
            double y0 = node.y - boxHeight / 2.0;
            gc.setFill(Color.web("#ffffff"));
            gc.fillRoundRect(x0, y0, boxWidth, boxHeight, 14, 14);
            gc.setStroke(Color.web("#3f51b5"));
            gc.setLineWidth(1.4);
            gc.strokeRoundRect(x0, y0, boxWidth, boxHeight, 14, 14);
            String[] lines = node.label.split("\n");
            for (int i = 0; i < lines.length; i++) {
                double lineY = node.y + (i - (lines.length - 1) / 2.0) * 14;
                gc.setFill(Color.web("#1f2933"));
                gc.fillText(lines[i], node.x, lineY);
            }
        }
    }

    private static final class DiagramNode {
        private final String label;
        private final double x;
        private final double y;

        private DiagramNode(String label, double x, double y) {
            this.label = label;
            this.x = x;
            this.y = y;
        }
    }

    private Node createExplanationSection() {
        VBox section = new VBox(6);
        section.setPadding(new Insets(16));
        section.setStyle(
                "-fx-background-color: #fafafa; -fx-border-color: #cfd8df; -fx-border-radius: 10; -fx-background-radius: 10;");
        Label heading = new Label("Explanation");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        String details = """
                1. Patient Master – User chooses the number of patients when generating; columns cover patient_id, patient_identifier_value, patient_birthdate, patient_postal_code, group, type, aac, cfs, social_risk, kpi_type, kpi_group, working_remarks, and attended_event_references. Dummy rows use RandomDataUtil + NRICGeneratorUtil, while uploaded files populate the same columns from the Patient Master sheet; the attended_event_references column is kept in sync with Event Session rows via LinkService.
                2. Event Session – Columns include composition_id, number_of_event_sessions, event_session_id1, event_session_mode1, event_session_start_date1/end_date1/duration1, venue1, capacity1, event_session_patient_references1, attended_indicator, and purpose_of_contact. Data comes from the Event Session workbook sheet or the KPI-type-aware generator; patient references pull patient_id values so the Patient Master tab shows matching attendance data.
                3. Practitioner Master – Columns such as practitioner_id, practitioner_identifier_value/system, manpower_position, volunteer_name, capacity, age, and working_remarks stem from MasterDataService.generate() (auto-copied from Master Data tab) or uploaded Practitioner sheets; you can also call the generator to append random volunteers when standing up test data.
                4. Encounter Master – Fields like encounter_id, status, display, start, purpose, contacted_staff_name, referred_by, and encounter_patient_reference originate from the Encounter Master sheet or Generate Encounters dialog. Generated rows reuse patient, session, and practitioner IDs and enforce ISO+08:00 timestamps plus “finished” status before export.
                5. QuestionnaireResponse Master – Tracks questionnaire_id, questionnaire_status, q1..q10, and questionnaire_patient_reference. Uploads come from QuestionnaireResponse Master sheet, while the generator fills alternating date/score answers through RandomDataUtil; patient references resolve to Patient Master IDs.
                6. Common – Built by CommonBuilderService, this sheet aggregates Patient, Event Session, Encounter, Questionnaire, and Practitioner rows into CommonRow compositions, capturing reporting_month, total_operating_days, total_clients, status, author info, and cross-sheet references. Uploading a Common sheet bypasses generation and preserves user edits.
                7. KPI JSON – The converter targets the current Excel file (AppState tracks the most recent export) or any selected workbook. After supplying the converter JAR and output folder, JsonExportService.run executes and builds FHIR bundles based on all active sheets; counts can be auto-filled from the selected Excel.
                8. JSON → CSV – Reads JSON directories produced by KPI JSON export (or any compatible output), scans each file with Gson to extract AAC identifiers plus raw resource strings, and writes a quoted CSV summary of AAC_ID and Raw_Resource.
                9. Master Data – Read-only view of AAC center → organization → location → volunteer rows generated by MasterDataService.generate(); regenerating refreshes the cache and repopulates Practitioner Master via updatePractitionersFromMaster.
                """;
        Label detailLabel = new Label(details);
        detailLabel.setWrapText(true);
        detailLabel.setMaxWidth(940);
        detailLabel.setStyle("-fx-text-fill: #3c4a54;");
        section.getChildren().addAll(heading, detailLabel);
        return section;
    }
}
