package com.aac.kpi.controller;

import com.aac.kpi.service.AppState;
import com.aac.kpi.service.MasterDataService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Optional;
public class MasterDataController {
    @FXML private TableView<MasterCenterRow> table;
    @FXML private TableColumn<MasterCenterRow, String> cAacId;
    @FXML private TableColumn<MasterCenterRow, String> cAacName;
    @FXML private TableColumn<MasterCenterRow, String> cOrganizationId;
    @FXML private TableColumn<MasterCenterRow, String> cOrganizationName;
    @FXML private TableColumn<MasterCenterRow, String> cOrganizationType;
    @FXML private TableColumn<MasterCenterRow, String> cLocationId;
    @FXML private TableColumn<MasterCenterRow, String> cLocationName;
    @FXML private TableColumn<MasterCenterRow, String> cPostalCode;
    @FXML private TableColumn<MasterCenterRow, String> cVolunteerId;
    @FXML private TableColumn<MasterCenterRow, String> cVolunteerName;
    @FXML private TableColumn<MasterCenterRow, String> cVolunteerRole;
    @FXML private TableColumn<MasterCenterRow, String> cActive;
    @FXML private TableColumn<MasterCenterRow, String> cRemarks;
    @FXML private Label statusLabel;

    private final ObservableList<MasterCenterRow> rows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        table.setItems(rows);
        cAacId.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().center().aacCenterId()));
        cAacName.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().center().aacCenterName()));
        cOrganizationId.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().organization() != null ? row.getValue().organization().organizationId() : ""));
        cOrganizationName.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().organization() != null ? row.getValue().organization().name() : ""));
        cOrganizationType.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().organization() != null ? row.getValue().organization().organizationType() : ""));
        cLocationId.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().location() != null ? row.getValue().location().locationId() : ""));
        cLocationName.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().location() != null ? row.getValue().location().locationName() : ""));
        cPostalCode.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().location() != null ? row.getValue().location().postalCode() : ""));
        cVolunteerId.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().volunteerIdList()));
        cVolunteerName.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().volunteerCount() + " volunteer ID" + (row.getValue().volunteerCount() == 1 ? "" : "s")));
        cVolunteerRole.setCellValueFactory(row -> new SimpleStringProperty("Administrative Support"));
        cActive.setCellValueFactory(row -> new SimpleStringProperty("TRUE"));
        cRemarks.setCellValueFactory(row -> new SimpleStringProperty("Linked to Befriending KPI"));
        statusLabel.setText("Master data ready");
    }

    public void setMasterData(MasterDataService.MasterData data) {
        if (data == null) return;
        rows.setAll(buildCenterRows(data));
        statusLabel.setText("AAC centers: " + data.getAacCenters().size());
    }

    private ObservableList<MasterCenterRow> buildCenterRows(MasterDataService.MasterData data) {
        ObservableList<MasterCenterRow> list = FXCollections.observableArrayList();
        for (MasterDataService.AacCenter center : data.getAacCenters()) {
            list.add(new MasterCenterRow(
                    center,
                    data.getOrganization(center.organizationId()),
                    data.getPrimaryLocation(center.organizationId()),
                    data.getVolunteerIds(center.aacCenterId())));
        }
        return list;
    }

    private Runnable onRegenerate;

    public void setOnRegenerate(Runnable callback) { this.onRegenerate = callback; }

    @FXML
    private void onRegenerateMasterData() {
        int volunteersPerCenter = promptVolunteerCount();
        AppState.setVolunteersPerCenter(volunteersPerCenter);
        MasterDataService.MasterData data = MasterDataService.generate(volunteersPerCenter);
        AppState.setMasterData(data);
        setMasterData(data);
        if (onRegenerate != null) onRegenerate.run();
    }

    private int promptVolunteerCount() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(AppState.getVolunteersPerCenter()));
        dialog.setTitle("Volunteer IDs per AAC center");
        dialog.setHeaderText("Specify how many volunteer IDs to generate for each AAC center.");
        dialog.setContentText("Number of volunteers:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int parsed = Integer.parseInt(result.get().trim());
                return Math.max(0, parsed);
            } catch (NumberFormatException ignored) {
                statusLabel.setText("Invalid number – using previous value.");
            }
        }
        return AppState.getVolunteersPerCenter();
    }

    private static final class MasterCenterRow {
        private final MasterDataService.AacCenter center;
        private final MasterDataService.Organization organization;
        private final MasterDataService.Location location;
        private final String volunteerIdList;
        private final int volunteerCount;

        MasterCenterRow(MasterDataService.AacCenter center,
                        MasterDataService.Organization organization,
                        MasterDataService.Location location,
                        java.util.List<String> volunteerIds) {
            this.center = center;
            this.organization = organization;
            this.location = location;
            this.volunteerCount = volunteerIds.size();
            this.volunteerIdList = String.join("##", volunteerIds);
        }

        MasterDataService.AacCenter center() { return center; }
        MasterDataService.Organization organization() { return organization; }
        MasterDataService.Location location() { return location; }
        String volunteerIdList() { return volunteerIdList; }
        int volunteerCount() { return volunteerCount; }
    }
}
