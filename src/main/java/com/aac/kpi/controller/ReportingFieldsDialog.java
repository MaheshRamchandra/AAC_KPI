package com.aac.kpi.controller;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

public final class ReportingFieldsDialog {
    private ReportingFieldsDialog() {}

    public record ReportingFields(String reportingMonth, String reportDate) {}

    public static Optional<ReportingFields> prompt(String existingMonth, String existingDate) {
        Dialog<ReportingFields> dialog = new Dialog<>();
        dialog.setTitle("Reporting month and date");
        dialog.setHeaderText("Fill the extension_reporting_month and date values used across report tables.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField monthField = new TextField(defaultMonth(existingMonth));
        TextField dateField = new TextField(defaultDate(existingDate));

        monthField.setPromptText("yyyy-MM");
        dateField.setPromptText("yyyy-MM-dd'T'HH:mm:ssXXX");

        Label monthHint = new Label("Example: 2025-05");
        Label dateHint = new Label("Example: 2025-05-15T10:00:00+08:00 (editable)");
        dateHint.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("extension_reporting_month:"), 0, 0);
        grid.add(monthField, 1, 0);
        grid.add(monthHint, 1, 1);
        grid.add(new Label("date:"), 0, 2);
        grid.add(dateField, 1, 2);
        grid.add(dateHint, 1, 3);

        dialog.getDialogPane().setContent(grid);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, evt -> {
            if (!isValidMonth(monthField.getText())) {
                showError("Please enter extension_reporting_month as yyyy-MM (e.g., 2025-05).");
                evt.consume();
                return;
            }
            if (!isValidDate(dateField.getText())) {
                showError("Please enter a date (e.g., 2025-05-15T10:00:00+08:00 or 2025-05-15 10:00:00).");
                evt.consume();
            }
        });

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) return null;
            return new ReportingFields(monthField.getText().trim(), dateField.getText().trim());
        });

        return dialog.showAndWait();
    }

    private static boolean isValidMonth(String value) {
        if (value == null) return false;
        String v = value.trim();
        if (!v.matches("\\d{4}-\\d{2}")) return false;
        try {
            YearMonth.parse(v, DateTimeFormatter.ofPattern("yyyy-MM"));
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private static boolean isValidDate(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        String v = value.trim();
        for (DateTimeFormatter fmt : new DateTimeFormatter[] {
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        }) {
            try {
                if (fmt == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
                    OffsetDateTime.parse(v, fmt);
                } else {
                    LocalDateTime.parse(v, fmt);
                }
                return true;
            } catch (Exception ignored) { }
        }
        try {
            LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (Exception ignored) { }
        return false;
    }

    private static String defaultMonth(String existing) {
        if (existing != null && !existing.isBlank()) return existing.trim();
        return YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private static String defaultDate(String existing) {
        if (existing != null && !existing.isBlank()) return existing.trim();
        OffsetDateTime odt = LocalDate.now().atTime(10, 0).atOffset(ZoneOffset.of("+08:00"));
        return odt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
    }

    private static void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
