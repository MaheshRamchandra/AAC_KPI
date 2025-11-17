package com.aac.kpi.controller;

import com.aac.kpi.service.AppState;
import com.aac.kpi.service.KpiConfig;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

public class KpiSettingsDialog extends Dialog<KpiConfig> {
    public KpiSettingsDialog() {
        setTitle("KPI Settings");
        setHeaderText("Customize minimum in-person event counts used in KPI computation");

        ButtonType okType = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        KpiConfig cfg = AppState.getKpiConfig().copy();

        Spinner<Integer> spRobust = spinner(0, 1000, cfg.robustMinInPerson);
        Spinner<Integer> spFrail = spinner(0, 1000, cfg.frailMinInPerson);
        Spinner<Integer> spBuddy = spinner(0, 1000, cfg.buddyingMinInPerson);
        Spinner<Integer> spBef = spinner(0, 1000, cfg.befriendingMinInPerson);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Robust min in-person"), spRobust);
        grid.addRow(1, new Label("Frail min in-person"), spFrail);
        grid.addRow(2, new Label("Buddying min in-person"), spBuddy);
        grid.addRow(3, new Label("Befriending min in-person"), spBef);

        getDialogPane().setContent(grid);

        setResultConverter(new Callback<ButtonType, KpiConfig>() {
            @Override
            public KpiConfig call(ButtonType param) {
                if (param != okType) return null;
                KpiConfig out = new KpiConfig();
                out.robustMinInPerson = spRobust.getValue();
                out.frailMinInPerson = spFrail.getValue();
                out.buddyingMinInPerson = spBuddy.getValue();
                out.befriendingMinInPerson = spBef.getValue();
                return out;
            }
        });
    }

    private Spinner<Integer> spinner(int min, int max, int value) {
        Spinner<Integer> sp = new Spinner<>(min, max, value);
        sp.setEditable(true);
        return sp;
    }
}

