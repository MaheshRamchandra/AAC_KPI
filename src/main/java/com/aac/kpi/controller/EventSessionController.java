package com.aac.kpi.controller;

import com.aac.kpi.model.EventSession;
import com.aac.kpi.model.Patient;
import com.aac.kpi.service.AppState;
import com.aac.kpi.service.ExcelReader;
import com.aac.kpi.service.RandomDataUtil;
import com.aac.kpi.service.LinkService;
import com.aac.kpi.service.ValidatorService;
import com.aac.kpi.ui.TableHighlightSupport;
import com.aac.kpi.util.StringUtils;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class EventSessionController {
    private ObservableList<Patient> patients;
    private ObservableList<EventSession> sessions;
    private Label statusLabel;
    private Runnable clearAllHandler;
    private final Set<EventSession> highlightedGeneratedSessions = new HashSet<>();
    private final Set<EventSession> issueHighlights = new HashSet<>();

    @FXML private TableView<EventSession> table;
    @FXML private TableColumn<EventSession, String> cCompId;
    @FXML private TableColumn<EventSession, Integer> cNum;
    @FXML private TableColumn<EventSession, String> cESId;
    @FXML private TableColumn<EventSession, String> cMode;
    @FXML private TableColumn<EventSession, String> cStart;
    @FXML private TableColumn<EventSession, String> cEnd;
    @FXML private TableColumn<EventSession, Integer> cDur;
    @FXML private TableColumn<EventSession, String> cVenue;
    @FXML private TableColumn<EventSession, Integer> cCap;
    @FXML private TableColumn<EventSession, String> cPatientRef;
    @FXML private TableColumn<EventSession, Boolean> cAttended;
    @FXML private TableColumn<EventSession, String> cPurpose;

    public void init(ObservableList<Patient> patients, ObservableList<EventSession> sessions, Label statusLabel) {
        this.patients = patients;
        this.sessions = sessions;
        this.statusLabel = statusLabel;
        table.setItems(this.sessions);
        table.setEditable(true);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        installCopyHandler();
        TableHighlightSupport.install(table, highlightedGeneratedSessions, issueHighlights);
    }

    @FXML
    private void initialize() {
        if (cCompId != null) cCompId.setCellValueFactory(new PropertyValueFactory<>("compositionId"));
        if (cNum != null) cNum.setCellValueFactory(new PropertyValueFactory<>("numberOfEventSessions"));
        if (cESId != null) cESId.setCellValueFactory(new PropertyValueFactory<>("eventSessionId1"));
        if (cMode != null) cMode.setCellValueFactory(new PropertyValueFactory<>("eventSessionMode1"));
        if (cStart != null) cStart.setCellValueFactory(new PropertyValueFactory<>("eventSessionStartDate1"));
        if (cEnd != null) cEnd.setCellValueFactory(new PropertyValueFactory<>("eventSessionEndDate1"));
        if (cDur != null) cDur.setCellValueFactory(new PropertyValueFactory<>("eventSessionDuration1"));
        if (cVenue != null) cVenue.setCellValueFactory(new PropertyValueFactory<>("eventSessionVenue1"));
        if (cCap != null) cCap.setCellValueFactory(new PropertyValueFactory<>("eventSessionCapacity1"));
        if (cPatientRef != null) cPatientRef.setCellValueFactory(new PropertyValueFactory<>("eventSessionPatientReferences1"));
        if (cAttended != null) cAttended.setCellValueFactory(new PropertyValueFactory<>("attendedIndicator"));
        if (cPurpose != null) cPurpose.setCellValueFactory(new PropertyValueFactory<>("purposeOfContact"));

        // Editable columns
        if (cCompId != null) {
            cCompId.setCellFactory(TextFieldTableCell.forTableColumn());
            cCompId.setOnEditCommit(e -> { e.getRowValue().setCompositionId(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true); com.aac.kpi.service.LinkService.fillPatientAttendedRefs(patients, sessions);} );
        }
        if (cNum != null) {
            cNum.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            cNum.setOnEditCommit(e -> { e.getRowValue().setNumberOfEventSessions(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cESId != null) {
            cESId.setCellFactory(TextFieldTableCell.forTableColumn());
            cESId.setOnEditCommit(e -> { e.getRowValue().setEventSessionId1(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cMode != null) {
            cMode.setCellFactory(TextFieldTableCell.forTableColumn());
            cMode.setOnEditCommit(e -> { e.getRowValue().setEventSessionMode1(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cStart != null) {
            cStart.setCellFactory(TextFieldTableCell.forTableColumn());
            cStart.setOnEditCommit(e -> { e.getRowValue().setEventSessionStartDate1(toIsoOffset(e.getNewValue())); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cEnd != null) {
            cEnd.setCellFactory(TextFieldTableCell.forTableColumn());
            cEnd.setOnEditCommit(e -> { e.getRowValue().setEventSessionEndDate1(toIsoOffset(e.getNewValue())); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cDur != null) {
            cDur.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            cDur.setOnEditCommit(e -> { e.getRowValue().setEventSessionDuration1(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cVenue != null) {
            cVenue.setCellFactory(TextFieldTableCell.forTableColumn());
            cVenue.setOnEditCommit(e -> { e.getRowValue().setEventSessionVenue1(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cCap != null) {
            cCap.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            cCap.setOnEditCommit(e -> { e.getRowValue().setEventSessionCapacity1(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cPatientRef != null) {
            cPatientRef.setCellFactory(TextFieldTableCell.forTableColumn());
            cPatientRef.setOnEditCommit(e -> { e.getRowValue().setEventSessionPatientReferences1(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true); com.aac.kpi.service.LinkService.fillPatientAttendedRefs(patients, sessions);} );
        }
        if (cAttended != null) {
            cAttended.setCellFactory(col -> new CheckBoxTableCell<>());
            cAttended.setOnEditCommit(e -> { e.getRowValue().setAttendedIndicator(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
        if (cPurpose != null) {
            cPurpose.setCellFactory(TextFieldTableCell.forTableColumn());
            cPurpose.setOnEditCommit(e -> { e.getRowValue().setPurposeOfContact(e.getNewValue()); com.aac.kpi.service.AppState.setDirty(true);} );
        }
    }

    @FXML
    private void onGenerateSessions() {
        Optional<ModeSelection> mode = promptForModeSelection();
        if (mode.isEmpty()) return;
        if (patients.isEmpty()) {
            showAlert("Please generate or load patients first.");
            return;
        }

        ChoiceDialog<String> cd = new ChoiceDialog<>("Robust", Arrays.asList("Robust", "Budding", "Befriending", "Frail"));
        cd.setTitle("KPI Type");
        cd.setHeaderText(null);
        cd.setContentText("Select KPI type:");
        Optional<String> type = cd.showAndWait();
        if (type.isEmpty()) return;

        String kpi = type.get();
        int baseCount;
        switch (kpi) {
            case "Befriending" -> baseCount = 52; // >=52
            case "Budding" -> baseCount = 12; // >=12
            case "Frail" -> baseCount = 6; // fixed in-person count
            default -> baseCount = 1; // Robust
        }

        Optional<DateRange> dateRange = promptForDateRangeSelection();
        if (dateRange.isEmpty()) return;
        DateRange range = dateRange.get();

        // Ask whether to append or clear
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Append to existing sessions? (Cancel = Clear and regenerate)", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        Optional<ButtonType> ans = confirm.showAndWait();
        boolean append = ans.isPresent() && ans.get() == ButtonType.OK;
        if (!append) {
            sessions.clear();
            clearSessionHighlights();
        }

        List<EventSession> newSessions = new ArrayList<>();
        Random rnd = new Random();
        LocalDateTime fixedStart = range.startDate().atTime(9, 0);
        LocalDateTime fixedEnd = range.endDate().atTime(17, 0);
        if (!fixedEnd.isAfter(fixedStart)) fixedEnd = fixedStart.plusHours(1);
        long durationMinutes = Duration.between(fixedStart, fixedEnd).toMinutes();
        int durationValue = (int) Math.max(1, durationMinutes);
        String startText = RandomDataUtil.formatEventDateTime(fixedStart);
        String endText = RandomDataUtil.formatEventDateTime(fixedEnd);
        for (Patient p : patients) {
            int count = baseCount;
            if ("Befriending".equals(kpi)) count += rnd.nextInt(10); // 52..61
            if ("Budding".equals(kpi)) count += rnd.nextInt(10); // 12..21
            for (int i = 0; i < count; i++) {
                EventSession s = new EventSession();
                s.setCompositionId(com.aac.kpi.service.RandomDataUtil.uuid32());
                s.setNumberOfEventSessions(1);
                s.setEventSessionId1(RandomDataUtil.randomEventId());
                s.setEventSessionMode1(mode.get().getValue());
                s.setEventSessionStartDate1(startText);
                s.setEventSessionEndDate1(endText);
                s.setEventSessionDuration1(durationValue);
                s.setEventSessionVenue1(RandomDataUtil.randomVenue());
                s.setEventSessionCapacity1(RandomDataUtil.randomCapacity());
                s.setEventSessionPatientReferences1(p.getPatientId());
                newSessions.add(s);
            }
        }
        sessions.addAll(newSessions);
        LinkService.fillPatientAttendedRefs(patients, sessions);
        markGeneratedSessions(newSessions);
        updateStatus();
    }

    @FXML
    private void onUploadExcel() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        File f = fc.showOpenDialog(table.getScene() != null ? table.getScene().getWindow() : null);
        if (f == null) return;
        try {
            List<EventSession> list = ExcelReader.readEventSessions(f);
            sessions.setAll(list);
            LinkService.fillPatientAttendedRefs(patients, sessions);
            updateStatus();
            clearSessionHighlights();
        } catch (Exception ex) {
            showAlert("Failed to load sessions: " + ex.getMessage());
        }
    }

    @FXML
    private void onAnalyze() {
        List<String> issues = ValidatorService.validateSessions(sessions, patients);
        if (issues.isEmpty()) {
            showInfo("Event Session OK");
        } else {
            showInfo(String.join("\n", issues));
        }
    }

    private void updateStatus() {
        if (statusLabel != null) {
            statusLabel.setText(String.format("Generated %d patients | %d sessions", patients.size(), sessions.size()));
        }
    }

    private Optional<ModeSelection> promptForModeSelection() {
        Dialog<ModeSelection> dialog = new Dialog<>();
        dialog.setTitle("Event session mode");
        dialog.setHeaderText("Choose one of the allowed session modes before generating new rows:");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<ModeSelection> combo = new ComboBox<>();
        combo.getItems().addAll(ModeSelection.values());
        combo.getSelectionModel().select(ModeSelection.IN_PERSON);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Mode:"), 0, 0);
        grid.add(combo, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> button == ButtonType.OK ? combo.getValue() : null);
        dialog.getDialogPane().setPrefWidth(360);
        return dialog.showAndWait();
    }

    private Optional<DateRange> promptForDateRangeSelection() {
        Dialog<DateRange> dialog = new Dialog<>();
        dialog.setTitle("Event session date range");
        dialog.setHeaderText("Choose the inclusive range for generated session dates:");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        LocalDate defaultStart = LocalDate.of(2025, 4, 1);
        LocalDate defaultEnd = LocalDate.of(2026, 3, 31);
        DatePicker startPicker = new DatePicker(defaultStart);
        DatePicker endPicker = new DatePicker(defaultEnd);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Start date:"), 0, 0);
        grid.add(startPicker, 1, 0);
        grid.add(new Label("End date:"), 0, 1);
        grid.add(endPicker, 1, 1);

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        Runnable validate = () -> {
            LocalDate start = startPicker.getValue();
            LocalDate end = endPicker.getValue();
            okButton.setDisable(start == null || end == null);
        };
        startPicker.valueProperty().addListener((obs, old, val) -> validate.run());
        endPicker.valueProperty().addListener((obs, old, val) -> validate.run());
        validate.run();

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(360);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                LocalDate start = startPicker.getValue();
                LocalDate end = endPicker.getValue();
                if (start == null || end == null) return null;
                if (end.isBefore(start)) {
                    LocalDate tmp = start;
                    start = end;
                    end = tmp;
                }
                return new DateRange(start, end);
            }
            return null;
        });
        return dialog.showAndWait();
    }

    public enum ModeSelection {
        IN_PERSON("In-person"),
        FACE_TO_FACE("Face-to-Face"),
        F2F("F2F"),
        IN_PERSON_LOWER("in-person");

        private final String value;

        ModeSelection(String value) { this.value = value; }

        public String getValue() { return value; }

        @Override
        public String toString() { return value; }
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {}

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public void refreshTable() {
        if (table != null) table.refresh();
    }

    public void highlightIssues(Collection<EventSession> issues) {
        TableHighlightSupport.replace(table, issues, issueHighlights);
    }

    public void setClearAllHandler(Runnable handler) {
        this.clearAllHandler = handler;
    }

    private void markGeneratedSessions(Collection<EventSession> newSessions) {
        if (newSessions == null || newSessions.isEmpty()) return;
        EventSession first = newSessions.iterator().next();
        TableHighlightSupport.add(table, first, highlightedGeneratedSessions);
        String safeId = StringUtils.sanitizeAlphaNum(first.getCompositionId());
        AppState.addHighlightedEventSessionCompositionId(safeId);
    }

    private void clearSessionHighlights() {
        TableHighlightSupport.clear(table, highlightedGeneratedSessions);
        AppState.clearHighlightedEventSessionCompositionIds();
    }

    @FXML
    private void onClearSheet() {
        sessions.clear();
        clearSessionHighlights();
        LinkService.fillPatientAttendedRefs(patients, sessions);
        AppState.setDirty(true);
        updateStatus();
        refreshTable();
    }

    @FXML
    private void onClearAll() {
        if (clearAllHandler != null) clearAllHandler.run();
    }

    // Utilities
    private static String toIsoOffset(String s) {
        if (s == null || s.isBlank()) return s;
        java.time.ZoneOffset off = java.time.ZoneOffset.of("+08:00");
        try {
            // yyyy-MM-dd'T'HH:mm:ssXXX
            return java.time.OffsetDateTime.parse(s, java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .withOffsetSameInstant(off)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        } catch (Exception ignored) {}
        try {
            // yyyy-MM-dd HH:mm:ss
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return ldt.atOffset(off).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        } catch (Exception ignored) {}
        try {
            // yyyy-MM-dd HH:mm
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return ldt.atOffset(off).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        } catch (Exception ignored) {}
        try {
            // yyyy-MM-dd
            java.time.LocalDate d = java.time.LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return d.atStartOfDay().atOffset(off).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        } catch (Exception ignored) {}
        return s;
    }

    private void installCopyHandler() {
        table.setOnKeyPressed(ev -> {
            if (ev.isShortcutDown() && ev.getCode() == KeyCode.C) {
                copySelectionToClipboard();
                ev.consume();
            }
        });
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(e -> copySelectionToClipboard());
        table.setContextMenu(new ContextMenu(copyItem));
    }

    private void copySelectionToClipboard() {
        StringBuilder sb = new StringBuilder();
        var model = table.getSelectionModel();
        var cells = new ArrayList<>(model.getSelectedCells());
        cells.sort((p1, p2) -> {
            int c = Integer.compare(p1.getRow(), p2.getRow());
            if (c != 0) return c;
            return Integer.compare(p1.getColumn(), p2.getColumn());
        });
        int prevRow = -1;
        for (TablePosition<?, ?> pos : cells) {
            int row = pos.getRow();
            if (prevRow == row) sb.append('\t');
            else if (prevRow != -1) sb.append('\n');
            Object val = pos.getTableColumn().getCellData(row);
            sb.append(val == null ? "" : val.toString());
            prevRow = row;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }
}
