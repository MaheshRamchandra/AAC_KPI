package com.aac.kpi.controller;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public final class KpiRegistrationDialog {
    private static final int ROBUST_MIN = 2;
    private static final int ROBUST_MAX = 5;
    private static final int FRAIL_MIN = 0; // allow any non-negative value
    private static final int BUDDING_MIN = 6;
    private static final int BEFRIENDING_MIN = 12;

    public record RegistrationSelection(String selectedType, RegistrationConfig config) {}
    public record RegistrationConfig(int robust, int frail, int budding, int befriending) {}

    private enum KpiType {
        ROBUST("Robust", ROBUST_MIN, ROBUST_MAX),
        FRAIL("Frail", FRAIL_MIN, Integer.MAX_VALUE),
        BUDDING("Budding", BUDDING_MIN, Integer.MAX_VALUE),
        BEFRIENDING("Befriending", BEFRIENDING_MIN, Integer.MAX_VALUE);

        final String label;
        final int min;
        final int max;

        KpiType(String label, int min, int max) {
            this.label = label;
            this.min = min;
            this.max = max;
        }

        @Override public String toString() { return label; }
    }

    private KpiRegistrationDialog() {}

    public static Optional<RegistrationSelection> prompt(int currentRobust, int currentFrail, int currentBudding, int currentBefriending) {
        Dialog<RegistrationSelection> dialog = new Dialog<>();
        dialog.setTitle("Registration IDs per KPI Type");
        dialog.setHeaderText("Choose a KPI type and enter how many registration IDs to generate for it.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<KpiType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(KpiType.values());
        typeBox.getSelectionModel().select(KpiType.ROBUST);

        TextField countField = new TextField(defaultValue(currentRobust, ROBUST_MIN));
        Label hint = new Label(hintText(typeBox.getValue()));

        typeBox.setOnAction(e -> {
            KpiType sel = typeBox.getValue();
            if (sel == null) return;
            int preset = switch (sel) {
                case ROBUST -> defaultInt(currentRobust, ROBUST_MIN);
                case FRAIL -> defaultInt(currentFrail, FRAIL_MIN);
                case BUDDING -> defaultInt(currentBudding, BUDDING_MIN);
                case BEFRIENDING -> defaultInt(currentBefriending, BEFRIENDING_MIN);
            };
            countField.setText(String.valueOf(preset));
            hint.setText(hintText(sel));
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("KPI Type:"), 0, 0);
        grid.add(typeBox, 1, 0);
        grid.add(new Label("Registration IDs:"), 0, 1);
        grid.add(countField, 1, 1);
        grid.add(hint, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, evt -> {
            try {
                KpiType sel = typeBox.getValue();
                if (sel == null) throw new IllegalArgumentException("Please choose a KPI type.");
                int value = parseOrDefault(countField.getText(), sel.min);
                validate(sel, value);
            } catch (Exception ex) {
                evt.consume();
                Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
                alert.setHeaderText(null);
                alert.showAndWait();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            KpiType sel = typeBox.getValue();
            if (sel == null) return null;
            int value = parseOrDefault(countField.getText(), sel.min);
            validate(sel, value);
            int robust = currentRobust <= 0 ? ROBUST_MIN : currentRobust;
            int frail = Math.max(FRAIL_MIN, currentFrail);
            int budding = currentBudding <= 0 ? BUDDING_MIN : currentBudding;
            int befriending = currentBefriending <= 0 ? BEFRIENDING_MIN : currentBefriending;
            switch (sel) {
                case ROBUST -> robust = value;
                case FRAIL -> frail = value;
                case BUDDING -> budding = value;
                case BEFRIENDING -> befriending = value;
            }
            return new RegistrationSelection(sel.label, new RegistrationConfig(robust, frail, budding, befriending));
        });

        return dialog.showAndWait();
    }

    private static void validate(KpiType type, int value) {
        if (value < type.min) throw new IllegalArgumentException(type.label + " must be at least " + type.min);
        if (type.max != Integer.MAX_VALUE && value > type.max)
            throw new IllegalArgumentException(type.label + " can have only maximum " + type.max + " attended events.");
    }

    private static String hintText(KpiType type) {
        if (type == null) return "";
        if (type.max == Integer.MAX_VALUE) {
            return "(min " + type.min + ")";
        }
        return "(min " + type.min + ", max " + type.max + ")";
    }

    private static String defaultValue(int current, int minDefault) {
        int v = current <= 0 ? minDefault : current;
        return String.valueOf(v);
    }

    private static int defaultInt(int current, int minDefault) {
        return current <= 0 ? minDefault : current;
    }

    private static int parseOrDefault(String text, int fallback) {
        if (text == null || text.trim().isEmpty()) return fallback;
        return Integer.parseInt(text.trim());
    }
}
