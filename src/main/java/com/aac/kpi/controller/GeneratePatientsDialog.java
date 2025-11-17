package com.aac.kpi.controller;

import com.aac.kpi.service.NricMode;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

import java.util.*;

public class GeneratePatientsDialog extends Dialog<GeneratePatientsDialog.Config> {

    public static class Config {
        public int total;
        public NricMode nricMode;
        public Map<Integer, Integer> fixedAges = new LinkedHashMap<>();
        public int randomCount; // number of random-age patients
        public int randomMinAge = 60;
        public int randomMaxAge = 90;
    }

    public GeneratePatientsDialog() {
        setTitle("Generate Patients");
        setHeaderText("Configure patient generation");

        ButtonType okType = new ButtonType("Generate", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        Spinner<Integer> spTotal = new Spinner<>(1, 100000, 100);
        spTotal.setEditable(true);

        ComboBox<NricMode> cbMode = new ComboBox<>();
        cbMode.getItems().addAll(NricMode.DUMMY, NricMode.ONLINE);
        cbMode.setValue(NricMode.DUMMY);

        ComboBox<String> cbAges = new ComboBox<>();
        cbAges.setEditable(true);
        cbAges.setPromptText("Age 50=20, Age 60=30, random=50");
        cbAges.getItems().addAll(
                "Age 30=20, random=80",
                "Age 50=30, Age 60=20, random=50",
                "random=100"
        );

        TextField tfRandomRange = new TextField();
        tfRandomRange.setPromptText("random age range (min-max), default 60-90");

        grid.addRow(0, new Label("Total patients"), spTotal);
        grid.addRow(1, new Label("NRIC mode"), cbMode);
        grid.addRow(2, new Label("Age rules"), cbAges);
        grid.addRow(3, new Label("Random range"), tfRandomRange);

        getDialogPane().setContent(grid);

        Node ok = getDialogPane().lookupButton(okType);
        ok.setDisable(false);

        setResultConverter(new Callback<ButtonType, Config>() {
            @Override
            public Config call(ButtonType param) {
                if (param != okType) return null;
                int totalValue = spTotal.getValue();
                String ageSpec = cbAges.getEditor() != null ? cbAges.getEditor().getText() : cbAges.getValue();
                if (ageSpec == null) ageSpec = "";
                if (!validateAgeRules(ageSpec, totalValue)) return null;
                Config c = new Config();
                c.total = totalValue;
                c.nricMode = cbMode.getValue();
                parseAgeRules(ageSpec, c);
                parseRandomRange(tfRandomRange.getText(), c);
                return c;
            }
        });
    }

    private void parseAgeRules(String text, Config c) {
        if (text == null || text.isBlank()) {
            c.randomCount = c.total; // all random by default
            return;
        }
        int assigned = 0;
        String[] parts = text.split("[,\n]\s*");
        Integer pendingP = null; // for p=NN
        Integer pendingA = null; // for A=NN
        for (String part : parts) {
            if (part.isBlank()) continue;
            String[] kv = part.split("=");
            if (kv.length != 2) continue;
            String k = kv[0].trim().toLowerCase();
            String v = kv[1].trim();
            try {
                if ("random".equals(k)) {
                    int count = Math.max(0, Integer.parseInt(v));
                    c.randomCount += count;
                } else if ("p".equals(k)) { // p=number of patients
                    pendingP = Math.max(0, Integer.parseInt(v));
                } else if ("a".equals(k)) { // A=age
                    pendingA = Math.max(0, Integer.parseInt(v));
                } else {
                    // original style age=count (e.g., 50=20)
                    OptionalInt ageVal = extractInteger(k);
                    if (ageVal.isEmpty()) continue;
                    int age = ageVal.getAsInt();
                    int count = Math.max(0, Integer.parseInt(v));
                    if (count > 0) c.fixedAges.merge(age, count, Integer::sum);
                }
            } catch (NumberFormatException ignored) {}

            if (pendingP != null && pendingA != null) {
                if (pendingP > 0) c.fixedAges.merge(pendingA, pendingP, Integer::sum);
                pendingP = null; pendingA = null;
            }
        }
        for (int cnt : c.fixedAges.values()) assigned += cnt;
        assigned += c.randomCount;
        if (assigned < c.total) c.randomCount += (c.total - assigned);
        if (assigned > c.total) {
            // trim random first, then fixed ages if still overflow
            int over = assigned - c.total;
            int trim = Math.min(over, c.randomCount);
            c.randomCount -= trim;
            over -= trim;
            if (over > 0) {
                // reduce from fixed ages in insertion order
                for (Map.Entry<Integer, Integer> e : c.fixedAges.entrySet()) {
                    if (over <= 0) break;
                    int reduce = Math.min(over, e.getValue());
                    e.setValue(e.getValue() - reduce);
                    over -= reduce;
                }
                c.fixedAges.values().removeIf(v -> v <= 0);
            }
        }
    }

    private void parseRandomRange(String text, Config c) {
        if (text == null || text.isBlank()) return;
        String[] p = text.replaceAll("\\s+", "").split("-");
        if (p.length == 2) {
            try {
                c.randomMinAge = Integer.parseInt(p[0]);
                c.randomMaxAge = Integer.parseInt(p[1]);
            } catch (NumberFormatException ignored) {}
        }
    }

    private OptionalInt extractInteger(String raw) {
        String digits = raw.replaceAll("[^0-9-]", "");
        if (digits.isBlank()) return OptionalInt.empty();
        try {
            return OptionalInt.of(Integer.parseInt(digits));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    private boolean validateAgeRules(String text, int total) {
        if (text == null || text.isBlank()) return true;
        int assigned = 0;
        String[] parts = text.split("[,\\n]\\s*");
        Integer pendingP = null;
        Integer pendingA = null;
        for (String part : parts) {
            if (part.isBlank()) continue;
            String[] kv = part.split("=");
            if (kv.length != 2) continue;
            String k = kv[0].trim().toLowerCase();
            String v = kv[1].trim();
            try {
                if ("random".equals(k)) {
                    assigned += Math.max(0, Integer.parseInt(v));
                } else if ("p".equals(k)) {
                    pendingP = Math.max(0, Integer.parseInt(v));
                } else if ("a".equals(k)) {
                    pendingA = Math.max(0, Integer.parseInt(v));
                } else {
                    int count = Math.max(0, Integer.parseInt(v));
                    assigned += count;
                }
            } catch (NumberFormatException ignored) {}
            if (pendingP != null && pendingA != null) {
                if (pendingP > 0) {
                    assigned += pendingP;
                }
                pendingP = null;
                pendingA = null;
            }
        }
        if (assigned > total) {
            showAgeRuleError(total);
            return false;
        }
        return true;
    }

    private void showAgeRuleError(int total) {
        Alert alert = new Alert(Alert.AlertType.ERROR,
                String.format("Total patients is %d. The chosen age rules allocate more than that number; please reduce the rule counts or increase the total patients.", total),
                ButtonType.OK);
        alert.setHeaderText("Invalid age rules");
        alert.showAndWait();
    }
}
