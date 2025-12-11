package com.aac.kpi.controller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class JsonCsvController {
    @FXML private TextField tfInputDir;
    @FXML private TextField tfOutputDir;
    @FXML private TextArea taLog;

    private final Gson gson = new Gson();
    private static final DateTimeFormatter NAME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final ReportDescriptor AAC_DESCRIPTOR = new ReportDescriptor("aac_report", "aac");
    private static final List<ReportDescriptor> REPORT_ORDER = List.of(
            new ReportDescriptor("volunteer_attendance_report", "volunteer_report", "volunteer"),
            new ReportDescriptor("organization_report", "organisation_report", "organization", "organisation"),
            new ReportDescriptor("location_report", "location"),
            new ReportDescriptor("resident_report", "resident"),
            new ReportDescriptor("event_report", "event"),
            AAC_DESCRIPTOR
    );
    private static final Pattern AAC_ROW_PATTERN = Pattern.compile("^\\\"(?i)aac\\d+");

    @FXML
    private void onBrowseInput() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select JSON folders root");
        File dir = chooser.showDialog(tfInputDir.getScene().getWindow());
        if (dir != null) tfInputDir.setText(dir.getAbsolutePath());
    }

    @FXML
    private void onBrowseOutput() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select CSV output folder");
        File dir = chooser.showDialog(tfOutputDir.getScene().getWindow());
        if (dir != null) tfOutputDir.setText(dir.getAbsolutePath());
    }

    @FXML
    private void onConvert() {
        taLog.clear();
        Path input = getDirectory(tfInputDir.getText(), "input");
        if (input == null) return;
        Path output = getDirectory(tfOutputDir.getText(), "output");
        if (output == null) return;
        try {
            List<Path> rawFolders = Files.list(input)
                    .filter(Files::isDirectory)
                    .toList();
            List<Path> folders = orderFolders(rawFolders);
            if (folders.isEmpty()) {
                log("No subdirectories found under " + input);
                return;
            }
            for (Path folder : folders) {
                processFolder(folder, output);
            }
            log("Conversion complete");
        } catch (IOException e) {
            showError("Failed to list folders", e);
        }
    }

    @FXML
    private void onFixAacCsv() {
        Path output = getDirectory(tfOutputDir.getText(), "output");
        if (output == null) return;
        try {
            List<Path> csvFiles = Files.list(output)
                    .filter(p -> p.toString().toLowerCase().endsWith(".csv"))
                    .sorted()
                    .toList();
            if (csvFiles.isEmpty()) {
                log("No CSV files found under " + output);
                return;
            }
            int fixed = 0;
            for (Path csv : csvFiles) {
                if (!isAacCsv(csv)) continue;
                fixMisbehavedAacCsv(csv);
                fixed++;
                log("Fixed AAC CSV: " + csv.getFileName());
            }
            if (fixed == 0) {
                log("No AAC CSV files detected in " + output);
            } else {
                log("Fixed " + fixed + " AAC CSV file(s)");
            }
        } catch (IOException e) {
            showError("Failed to fix AAC CSV files", e);
        }
    }

    private void processFolder(Path folder, Path output) throws IOException {
        List<Path> jsonFiles = Files.list(folder)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    return name.endsWith(".json") && !name.startsWith("all_");
                })
                .sorted()
                .toList();
        if (jsonFiles.isEmpty()) {
            log("Skipping " + folder.getFileName() + ": no JSON files");
            return;
        }
        String timestamp = NAME_FMT.format(LocalDateTime.now());
        String sanitizedName = folder.getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        String filePrefix = resolveFilePrefix(folder.getFileName().toString(), sanitizedName);
        String fileName = String.format("%s_%s_%d.csv", filePrefix, timestamp, System.currentTimeMillis());
        Path csv = output.resolve(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(csv)) {
            writer.write("AAC_ID,Raw_Resource\n");
            for (Path json : jsonFiles) {
                String content = Files.readString(json);
                JsonElement parsed = JsonParser.parseReader(lenientReader(content));
                String normalizedJson = gson.toJson(parsed);
                String aacId = findAacId(parsed).orElse("");
                writer.write(quote(aacId));
                writer.write(',');
                writer.write(quote(toCsvSafe(normalizedJson)));
                writer.write('\n');
            }
        }
        if (isAacFolder(folder)) {
            fixMisbehavedAacCsv(csv);
        }
        log("Generated " + csv.getFileName());
    }

    private String resolveFilePrefix(String folderName, String sanitizedFallback) {
        String lower = folderName.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "aac_report", "aac_reports" -> "hsar_aac_report";
            case "event_report", "event_reports" -> "hsar_event_report";
            case "location_report", "location_reports" -> "hsar_location";
            case "organization_report", "organisation_report", "organization_reports", "organisation_reports" -> "hsar_organization";
            case "resident_report", "resident_reports" -> "hsar_resident_report";
            case "volunteer_attendance_report", "volunteer_attendance_reports" -> "hsar_volunteer_attendance_report";
            default -> "hsar_" + sanitizedFallback;
        };
    }

    private Optional<String> findAacId(JsonElement element) {
        if (element == null) return Optional.empty();
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            Optional<String> direct = extractIdentifier(obj);
            if (direct.isPresent()) return direct;
            Optional<String> authorAac = extractFromAuthor(obj);
            if (authorAac.isPresent()) return authorAac;
            for (String key : obj.keySet()) {
                String lower = key.toLowerCase();
                if (lower.contains("aac") && obj.get(key).isJsonPrimitive()) {
                    Optional<String> maybe = normalizeAacId(obj.get(key).getAsString());
                    if (maybe.isPresent()) return maybe;
                }
            }
            for (JsonElement child : obj.entrySet().stream().map(Map.Entry::getValue).toList()) {
                Optional<String> found = findAacId(child);
                if (found.isPresent()) return found;
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                Optional<String> found = findAacId(child);
                if (found.isPresent()) return found;
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractIdentifier(JsonObject obj) {
        JsonElement system = obj.get("system");
        JsonElement value = obj.get("value");
        if (system != null && value != null &&
                "http://ihis.sg/identifier/aac-center-id".equals(system.getAsString())) {
            return normalizeAacId(value.getAsString());
        }
        JsonElement identifier = obj.get("identifier");
        if (identifier != null) {
            if (identifier.isJsonObject()) {
                Optional<String> maybe = extractIdentifier(identifier.getAsJsonObject());
                if (maybe.isPresent()) return maybe;
            } else if (identifier.isJsonArray()) {
                for (JsonElement idEntry : identifier.getAsJsonArray()) {
                    if (idEntry.isJsonObject()) {
                        Optional<String> maybe = extractIdentifier(idEntry.getAsJsonObject());
                        if (maybe.isPresent()) return maybe;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractFromAuthor(JsonObject obj) {
        JsonElement author = obj.get("author");
        if (author != null && author.isJsonArray()) {
            for (JsonElement profileEntry : author.getAsJsonArray()) {
                if (profileEntry.isJsonObject()) {
                    JsonObject entry = profileEntry.getAsJsonObject();
                    JsonElement identifier = entry.get("identifier");
                    if (identifier != null && identifier.isJsonObject()) {
                        JsonObject ident = identifier.getAsJsonObject();
                        JsonElement system = ident.get("system");
                        JsonElement value = ident.get("value");
                        if (system != null && value != null &&
                                system.getAsString().equals("http://ihis.sg/identifier/aac-center-id")) {
                            Optional<String> maybe = normalizeAacId(value.getAsString());
                            if (maybe.isPresent()) return maybe;
                        }
                    } else if (identifier != null && identifier.isJsonArray()) {
                        for (JsonElement idEntry : identifier.getAsJsonArray()) {
                            if (idEntry.isJsonObject()) {
                                JsonObject ident = idEntry.getAsJsonObject();
                                JsonElement system = ident.get("system");
                                JsonElement value = ident.get("value");
                                if (system != null && value != null &&
                                system.getAsString().equals("http://ihis.sg/identifier/aac-center-id")) {
                            Optional<String> maybe = normalizeAacId(value.getAsString());
                            if (maybe.isPresent()) return maybe;
                        }
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> normalizeAacId(String raw) {
        if (raw == null) return Optional.empty();
        String candidate = raw.trim();
        if (candidate.isEmpty()) return Optional.empty();
        String upper = candidate.toUpperCase(Locale.ROOT);
        if (upper.matches("AAC[A-Z0-9-]{1,}")) {
            return Optional.of(upper);
        }
        return Optional.empty();
    }

    private String quote(String value) {
        if (value == null)
            value = "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String toCsvSafe(String value) {
        if (value == null)
            return "";
        return escapeLineBreaks(value);
    }

    private String escapeLineBreaks(String value) {
        if (value.isEmpty())
            return value;
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\r') {
                if (i + 1 < value.length() && value.charAt(i + 1) == '\n') {
                    i++; // consume \n as part of CRLF
                }
                builder.append("\\n");
            } else if (ch == '\n' || ch == '\u000B' || ch == '\u000C' || ch == '\u0085'
                    || ch == '\u2028' || ch == '\u2029') {
                builder.append("\\n");
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private List<Path> orderFolders(List<Path> rawFolders) {
        List<Path> ordered = new ArrayList<>();
        List<Path> remaining = new ArrayList<>(rawFolders);
        for (ReportDescriptor descriptor : REPORT_ORDER) {
            Iterator<Path> iter = remaining.iterator();
            while (iter.hasNext()) {
                Path candidate = iter.next();
                if (descriptor.matches(candidate.getFileName().toString())) {
                    ordered.add(candidate);
                    iter.remove();
                }
            }
        }
        remaining.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)));
        ordered.addAll(remaining);
        return ordered;
    }

    private void fixMisbehavedAacCsv(Path csv) throws IOException {
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        if (lines.size() <= 1) {
            return;
        }
        List<String> rebuilt = new ArrayList<>();
        rebuilt.add(lines.get(0));
        String current = null;
        for (int i = 1; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw == null) continue;
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) continue;
            boolean hasAacId = parseAacIdFromLine(trimmed).isPresent();
            if (hasAacId) {
                if (current != null) {
                    rebuilt.add(current);
                }
                current = sanitizeCsvLine(trimmed);
            } else if (current != null) {
                current = current + "\\n" + sanitizeCsvLine(trimmed);
            } else {
                current = sanitizeCsvLine(trimmed);
            }
        }
        if (current != null) {
            rebuilt.add(current);
        }
        Files.write(csv, rebuilt, StandardCharsets.UTF_8);
    }

    private boolean isAacFolder(Path folder) {
        if (folder == null) return false;
        return AAC_DESCRIPTOR.matches(folder.getFileName().toString());
    }

    private Optional<String> parseAacIdFromLine(String line) {
        return parseFirstColumnValue(line).flatMap(this::normalizeAacId);
    }

    private Optional<String> parseFirstColumnValue(String line) {
        if (line == null || line.isEmpty()) {
            return Optional.empty();
        }
        boolean inQuotes = false;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (!inQuotes) {
                    inQuotes = true;
                    continue;
                }
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    builder.append('"');
                    i++;
                    continue;
                }
                inQuotes = false;
                continue;
            }
            if (!inQuotes && ch == ',') {
                return Optional.of(builder.toString());
            }
            builder.append(ch);
        }
        if (builder.length() > 0) {
            return Optional.of(builder.toString());
        }
        return Optional.empty();
    }

    private boolean isAacCsv(Path csv) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null || !header.toLowerCase(Locale.ROOT).contains("aac_id")) {
                return false;
            }
            return true;
        }
    }

    private static String normalizeFolderName(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }

    private String sanitizeCsvLine(String line) {
        if (line == null) return "";
        String withoutCarriage = line.replace("\r\n", "\n").replace("\r", "\n");
        return withoutCarriage.replace("\n", "\\n").trim();
    }

    private static final class ReportDescriptor {
        private final List<String> normalizedNames;

        private ReportDescriptor(String... names) {
            normalizedNames = Arrays.stream(names)
                    .filter(n -> n != null && !n.isBlank())
                    .map(JsonCsvController::normalizeFolderName)
                    .toList();
        }

        private boolean matches(String folderName) {
            String normalized = normalizeFolderName(folderName);
            return normalizedNames.contains(normalized);
        }
    }

    private Path getDirectory(String path, String label) {
        if (path == null || path.isBlank()) {
            showError("Missing " + label + " path", null);
            return null;
        }
        Path dir = Path.of(path);
        if (!Files.isDirectory(dir)) {
            showError("" + label + " path must be an existing directory", null);
            return null;
        }
        return dir;
    }

    private void showError(String message, Exception e) {
        new Alert(Alert.AlertType.ERROR, message + (e != null ? ": " + e.getMessage() : ""), ButtonType.OK).showAndWait();
        log("ERROR: " + message);
    }

    private void log(String message) {
        taLog.appendText(message + "\n");
    }

    @FXML
    private void onClearLog() {
        taLog.clear();
    }

    private JsonReader lenientReader(String json) {
        JsonReader reader = new JsonReader(new StringReader(json));
        reader.setLenient(true);
        return reader;
    }
}
