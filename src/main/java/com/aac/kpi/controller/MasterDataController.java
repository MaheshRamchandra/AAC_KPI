package com.aac.kpi.controller;

import com.aac.kpi.service.AppState;
import com.aac.kpi.service.MasterDataService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MasterDataController {
    @FXML private TableView<MasterDataService.MasterRow> table;
    @FXML private TableColumn<MasterDataService.MasterRow, String> cAacId;
    @FXML private TableColumn<MasterDataService.MasterRow, String> cAacName;
    @FXML private TableColumn<MasterDataService.MasterRow, String> cOrganizationId;
    @FXML private TableColumn<MasterDataService.MasterRow, String> cOrganizationName;
    @FXML private TableColumn<MasterDataService.MasterRow, String> cOrganizationType;
    @FXML private TableColumn<MasterDataService.MasterRow, String> cLocationId;
    @FXML private TableColumn<MasterDataService.MasterRow, String> cLocationName;
    @FXML private TableColumn<MasterDataService.MasterRow, String> cPostalCode;
    @FXML private TableColumn<MasterDataService.MasterRow, String> cVolunteerId;
    @FXML private TableColumn<MasterDataService.MasterRow, String> cVolunteerName;
    @FXML private TableColumn<MasterDataService.MasterRow, String> cVolunteerRole;
    @FXML private TableColumn<MasterDataService.MasterRow, String> cActive;
    @FXML private TableColumn<MasterDataService.MasterRow, String> cRemarks;
    @FXML private Button regenerateButton;
    @FXML private Label statusLabel;

    private final ObservableList<MasterDataService.MasterRow> rows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        table.setItems(rows);
        cAacId.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().aacCenter().aacCenterId()));
        cAacName.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().aacCenter().aacCenterName()));
        cOrganizationId.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().organization().organizationId()));
        cOrganizationName.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().organization().name()));
        cOrganizationType.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().organization().organizationType()));
        cLocationId.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().location() != null ? row.getValue().location().locationId() : ""));
        cLocationName.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().location() != null ? row.getValue().location().locationName() : ""));
        cPostalCode.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().location() != null ? row.getValue().location().postalCode() : ""));
        cVolunteerId.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().volunteer().volunteerId()));
        cVolunteerName.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().volunteer().volunteerName()));
        cVolunteerRole.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().volunteer().volunteerRole()));
        cActive.setCellValueFactory(row -> new SimpleStringProperty("TRUE"));
        cRemarks.setCellValueFactory(row -> new SimpleStringProperty("Linked to Befriending KPI"));
        statusLabel.setText("Master data ready");
    }

    public void setMasterData(MasterDataService.MasterData data) {
        if (data == null) return;
        rows.setAll(data.getRows());
        statusLabel.setText("Master rows: " + data.getRows().size());
    }

    private Runnable onRegenerate;

    public void setOnRegenerate(Runnable callback) { this.onRegenerate = callback; }

    @FXML
    private void onRegenerateMasterData() {
        MasterDataService.MasterData data = MasterDataService.generate();
        AppState.setMasterData(data);
        setMasterData(data);
        if (onRegenerate != null) onRegenerate.run();
    }
}
