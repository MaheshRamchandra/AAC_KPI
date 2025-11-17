package com.aac.kpi.controller;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.time.LocalDate;

public class GenerateQuestionnairesDialog extends Dialog<GenerateQuestionnairesDialog.Config> {
    public static class Config {
        public int total = 200;
        public LocalDate start = LocalDate.of(2025,4,1);
        public LocalDate end = LocalDate.of(2026,3,31);
        public int numericMin = 1;
        public int numericMax = 5;
    }

    public GenerateQuestionnairesDialog() {
        setTitle("Generate Questionnaire Responses");
        setHeaderText("Choose count and date range (FY)");
        ButtonType okType = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        Spinner<Integer> spTotal = new Spinner<>(1, 100000, 200);
        spTotal.setEditable(true);
        DatePicker dpStart = new DatePicker(LocalDate.of(2025,4,1));
        DatePicker dpEnd = new DatePicker(LocalDate.of(2026,3,31));
        Spinner<Integer> spMin = new Spinner<>(0, 10, 1);
        Spinner<Integer> spMax = new Spinner<>(0, 10, 5);
        spMin.setEditable(true);
        spMax.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8); grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Total questionnaires"), spTotal);
        grid.addRow(1, new Label("Start (FY)"), dpStart);
        grid.addRow(2, new Label("End (FY)"), dpEnd);
        grid.addRow(3, new Label("Numeric range (min/max)"), new HBox(8, spMin, new Label("to"), spMax));
        getDialogPane().setContent(grid);

        setResultConverter(new Callback<ButtonType, Config>() {
            @Override public Config call(ButtonType param) {
                if (param != okType) return null;
                Config c = new Config();
                c.total = spTotal.getValue();
                c.start = dpStart.getValue();
                c.end = dpEnd.getValue();
                c.numericMin = spMin.getValue();
                c.numericMax = spMax.getValue();
                if (c.numericMin > c.numericMax) {
                    int t = c.numericMin; c.numericMin = c.numericMax; c.numericMax = t;
                }
                return c;
            }
        });
    }
}
