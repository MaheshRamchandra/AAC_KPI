package com.aac.kpi.controller;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

import java.time.LocalDate;

public class GenerateEncountersDialog extends Dialog<GenerateEncountersDialog.Config> {
    public static class Config {
        public int total;
        public LocalDate start = LocalDate.of(2025,4,1);
        public LocalDate end = LocalDate.of(2026,3,31);
    }

    public GenerateEncountersDialog() {
        setTitle("Generate Encounters");
        setHeaderText("Choose count and date range (FY)");
        ButtonType okType = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        Spinner<Integer> spTotal = new Spinner<>(1, 100000, 200);
        spTotal.setEditable(true);
        DatePicker dpStart = new DatePicker(LocalDate.of(2025,4,1));
        DatePicker dpEnd = new DatePicker(LocalDate.of(2026,3,31));

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8); grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Total encounters"), spTotal);
        grid.addRow(1, new Label("Start (FY)"), dpStart);
        grid.addRow(2, new Label("End (FY)"), dpEnd);
        getDialogPane().setContent(grid);

        setResultConverter(new Callback<ButtonType, Config>() {
            @Override public Config call(ButtonType param) {
                if (param != okType) return null;
                Config c = new Config();
                c.total = spTotal.getValue();
                c.start = dpStart.getValue();
                c.end = dpEnd.getValue();
                return c;
            }
        });
    }
}

