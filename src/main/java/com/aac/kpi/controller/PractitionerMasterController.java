package com.aac.kpi.controller;

import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.model.Practitioner;
import com.aac.kpi.service.AppState;
import com.aac.kpi.service.ExcelReader;
import com.aac.kpi.service.ExcelWriter;
import com.aac.kpi.service.NRICGeneratorUtil;
import com.aac.kpi.service.RandomDataUtil;
import com.aac.kpi.ui.TableHighlightSupport;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.util.*;

public class PractitionerMasterController {
    private ObservableList<Practitioner> practitioners;
    private ObservableList<Patient> patients;
    private ObservableList<EventSession> sessions;
    private Label statusLabel;
    private Runnable clearAllHandler;
    private final Set<Practitioner> highlightedGeneratedPractitioners = new HashSet<>();
    private static final Random ID_RANDOM = new Random();

    @FXML private TableView<Practitioner> table;
    @FXML private TableColumn<Practitioner, String> cId;
    @FXML private TableColumn<Practitioner, String> cIdent;
    @FXML private TableColumn<Practitioner, String> cSystem;
    @FXML private TableColumn<Practitioner, String> cPos;
    @FXML private TableColumn<Practitioner, String> cName;
    @FXML private TableColumn<Practitioner, Double> cCap;
    @FXML private TableColumn<Practitioner, Integer> cAge;
    @FXML private TableColumn<Practitioner, String> cRemarks;

    public void init(ObservableList<Practitioner> practitioners,
                     ObservableList<Patient> patients,
                     ObservableList<EventSession> sessions,
                     Label statusLabel) {
        this.practitioners = practitioners;
        this.patients = patients;
        this.sessions = sessions;
        this.statusLabel = statusLabel;
        table.setItems(this.practitioners);
        table.setEditable(true);
        TableHighlightSupport.install(table, highlightedGeneratedPractitioners);
    }

    @FXML private void initialize() {
        if (cId != null) cId.setCellValueFactory(new PropertyValueFactory<>("practitionerId"));
        if (cIdent != null) cIdent.setCellValueFactory(new PropertyValueFactory<>("practitionerIdentifierValue"));
        if (cSystem != null) cSystem.setCellValueFactory(new PropertyValueFactory<>("practitionerIdentifierSystem"));
        if (cPos != null) cPos.setCellValueFactory(new PropertyValueFactory<>("practitionerManpowerPosition"));
        if (cName != null) cName.setCellValueFactory(new PropertyValueFactory<>("practitionerVolunteerName"));
        if (cCap != null) cCap.setCellValueFactory(new PropertyValueFactory<>("practitionerManpowerCapacity"));
        if (cAge != null) cAge.setCellValueFactory(new PropertyValueFactory<>("practitionerVolunteerAge"));
        if (cRemarks != null) cRemarks.setCellValueFactory(new PropertyValueFactory<>("workingRemarks"));

        if (cId != null) { cId.setCellFactory(TextFieldTableCell.forTableColumn()); cId.setOnEditCommit(e -> e.getRowValue().setPractitionerId(e.getNewValue())); }
        if (cIdent != null) { cIdent.setCellFactory(TextFieldTableCell.forTableColumn()); cIdent.setOnEditCommit(e -> e.getRowValue().setPractitionerIdentifierValue(e.getNewValue())); }
        if (cSystem != null) { cSystem.setCellFactory(TextFieldTableCell.forTableColumn()); cSystem.setOnEditCommit(e -> e.getRowValue().setPractitionerIdentifierSystem(e.getNewValue())); }
        if (cPos != null) { cPos.setCellFactory(TextFieldTableCell.forTableColumn()); cPos.setOnEditCommit(e -> e.getRowValue().setPractitionerManpowerPosition(e.getNewValue())); }
        if (cName != null) { cName.setCellFactory(TextFieldTableCell.forTableColumn()); cName.setOnEditCommit(e -> e.getRowValue().setPractitionerVolunteerName(e.getNewValue())); }
        if (cCap != null) { cCap.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter())); cCap.setOnEditCommit(e -> e.getRowValue().setPractitionerManpowerCapacity(e.getNewValue())); }
        if (cAge != null) { cAge.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter())); cAge.setOnEditCommit(e -> e.getRowValue().setPractitionerVolunteerAge(e.getNewValue())); }
        if (cRemarks != null) { cRemarks.setCellFactory(TextFieldTableCell.forTableColumn()); cRemarks.setOnEditCommit(e -> e.getRowValue().setWorkingRemarks(e.getNewValue())); }
    }

    @FXML private void onGenerate() {
        TextInputDialog dialog = new TextInputDialog("50");
        dialog.setTitle("Generate Practitioners");
        dialog.setHeaderText(null);
        dialog.setContentText("How many practitioners to generate?");
        Optional<String> res = dialog.showAndWait();
        if (res.isEmpty()) return;
        int n; try { n = Integer.parseInt(res.get()); } catch (Exception ex) { showAlert("Invalid number"); return; }

        List<Practitioner> list = new ArrayList<>();
        int startSuffix = 500 + new java.util.Random().nextInt(400);
        for (int i = 0; i < n; i++) list.add(randomPractitioner(startSuffix + i));
        practitioners.addAll(list);
        markGeneratedPractitioners(list);
        updateStatus();
    }

    private Practitioner randomPractitioner(int suffix) {
        Practitioner p = new Practitioner();
        p.setPractitionerId(RandomDataUtil.uuid32().toUpperCase(java.util.Locale.ROOT) + "F" + String.format(java.util.Locale.ROOT, "%03d", suffix));
        Identifier identifier = randomIdentifier();
        p.setPractitionerIdentifierValue(identifier.value());
        p.setPractitionerIdentifierSystem(identifier.system());
        String[] pos = {"Volunteer","Volunteer Coordinator","Admin Staff","Event Organizer","Social Worker","Healthcare Assistant","Programme Coordinator","Event Coordinator"};
        p.setPractitionerManpowerPosition(pos[new java.util.Random().nextInt(pos.length)]);
        p.setPractitionerVolunteerName(randomName());
        p.setPractitionerManpowerCapacity((4 + new java.util.Random().nextInt(7)) / 10.0);
        p.setPractitionerVolunteerAge(20 + new java.util.Random().nextInt(61));
        String[] remarks = {"Activities/Events Volunteer","Support Staff","Healthcare outreach","Community engagement","Programme logistics"};
        String remark = (new java.util.Random().nextBoolean() ? ((new java.util.Random().nextBoolean()) ? "An " : "A ") : "") + remarks[new java.util.Random().nextInt(remarks.length)];
        p.setWorkingRemarks(remark);
        return p;
    }

    private String randomName() {
        String[] sur = {"Tan","Lim","Lee","Ng","Goh","Teo","Chua","Ong","Wong","Chan","Koh","Toh","Chew","Chong","Foo","Quek","Yeo","Liu","Halim","Rahman","Ismail","Kaur","Singh","Zhang","Chen","Huang","Wang","Zhao","Loh","Chee"};
        String[] giv = {"Grace","Sarah","Rebecca","Emily","Rachel","Kevin","Daniel","Dennis","Marcus","Ryan","Wei","Ping","Hui","Xiao","Ying","Jun","Ah","Chong","Mei","Kai","Arun","Vijay","Priya","Siti","Aisyah","Farah","Hana","Nur","Amir","Faizal"};
        java.util.Random r = new java.util.Random();
        String surname = sur[r.nextInt(sur.length)];
        String g1 = giv[r.nextInt(giv.length)];
        if (r.nextBoolean()) return surname + " " + g1;
        String g2 = giv[r.nextInt(giv.length)];
        if (g2.equals(g1)) g2 = g2 + " Jr";
        return surname + " " + g1 + " " + g2;
    }

    private Identifier randomIdentifier() {
        if (ID_RANDOM.nextDouble() < 0.35) {
            int num = ID_RANDOM.nextInt(900) + 1;
            return new Identifier(String.format(java.util.Locale.ROOT, "SU%03d", num),
                    "http://ihis.sg/identifier/aac-staff-id");
        }
        return new Identifier(NRICGeneratorUtil.generateFakeNRIC(), "http://ihis.sg/identifier/nric");
    }

    private record Identifier(String value, String system) {}

    @FXML private void onUploadExcel() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File f = fc.showOpenDialog(table.getScene() != null ? table.getScene().getWindow() : null);
        if (f == null) return;
        try {
            java.util.List<Practitioner> list = ExcelReader.readPractitioners(f);
            if (!list.isEmpty()) practitioners.setAll(list);
            updateStatus();
            clearGeneratedPractitionersHighlight();
        } catch (Exception ex) {
            showAlert("Failed to load practitioners: " + ex.getMessage());
        }
    }

    @FXML private void onAnalyze() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < practitioners.size(); i++) {
            var p = practitioners.get(i);
            if (p.getPractitionerId() == null || p.getPractitionerId().isBlank()) sb.append("Row ").append(i+1).append(": Missing practitioner_id\n");
            else if (!ids.add(p.getPractitionerId())) sb.append("Row ").append(i+1).append(": Duplicate practitioner_id\n");
            double cap = p.getPractitionerManpowerCapacity();
            if (cap < 0.0 || cap > 1.0) sb.append("Row ").append(i+1).append(": Capacity out of range\n");
            int age = p.getPractitionerVolunteerAge();
            if (age < 18 || age > 85) sb.append("Row ").append(i+1).append(": Age out of range\n");
        }
        if (sb.length() == 0) showInfo("Practitioner Master OK"); else showInfo(sb.toString());
    }

    @FXML private void onExport() {
        try {
            com.aac.kpi.service.LinkService.fillPatientAttendedRefs(patients, sessions);
            java.io.File dest = com.aac.kpi.service.AppState.getCurrentExcelFile();
            if (dest == null) {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
                fc.setInitialFileName("KPI_Data.xlsx");
                dest = fc.showSaveDialog(table.getScene().getWindow());
                if (dest == null) return;
            }
            File file = ExcelWriter.saveToExcel(patients, sessions, practitioners, java.util.List.of(), java.util.List.of(), java.util.List.of(), dest);
            showInfo("Exported to: " + file.getAbsolutePath());
            statusLabel.setText(String.format("Generated %d patients | %d sessions | %d practitioners | Last export: %s",
                    patients.size(), sessions.size(), practitioners.size(), ExcelWriter.nowStamp()));
            com.aac.kpi.service.AppState.setDirty(false);
        } catch (Exception ex) {
            showAlert("Export failed: " + ex.getMessage());
        }
    }

    public void refreshTable() { if (table != null) table.refresh(); }

    private void updateStatus() {
        if (statusLabel != null) {
            statusLabel.setText(String.format("Generated %d patients | %d sessions | %d practitioners",
                    patients.size(), sessions.size(), practitioners.size()));
        }
    }

    private void showAlert(String msg) { Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK); a.setHeaderText(null); a.showAndWait(); }
    private void showInfo(String msg) { Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK); a.setHeaderText(null); a.showAndWait(); }

    public void setClearAllHandler(Runnable handler) {
        this.clearAllHandler = handler;
    }

    private void markGeneratedPractitioners(Collection<Practitioner> newItems) {
        if (newItems == null || newItems.isEmpty()) return;
        Practitioner first = newItems.iterator().next();
        TableHighlightSupport.add(table, first, highlightedGeneratedPractitioners);
        AppState.addHighlightedPractitionerId(first.getPractitionerId());
    }

    private void clearGeneratedPractitionersHighlight() {
        TableHighlightSupport.clear(table, highlightedGeneratedPractitioners);
        AppState.clearHighlightedPractitionerIds();
    }

    @FXML
    private void onClearSheet() {
        practitioners.clear();
        clearGeneratedPractitionersHighlight();
        AppState.setDirty(true);
        updateStatus();
        refreshTable();
    }

    @FXML
    private void onClearAll() {
        if (clearAllHandler != null) clearAllHandler.run();
    }
}
