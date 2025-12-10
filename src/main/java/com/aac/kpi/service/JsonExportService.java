package com.aac.kpi.service;

import com.aac.kpi.converter.ReportCounts;
import org.apache.poi.openxml4j.util.ZipSecureFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JsonExportService {
    private JsonExportService() {}

    // Allow larger compression ratios when reading XLSX files in the embedded converter
    private static final String MIN_INFLATE_RATIO = "0.0";

    public static class Result {
        private final int exitCode;
        private final String output;
        private final String command;

        public Result(int exitCode, String output, String command) {
            this.exitCode = exitCode;
            this.output = output;
            this.command = command;
        }

        public int getExitCode() { return exitCode; }
        public String getOutput() { return output; }
        public String getCommand() { return command; }
        public boolean isSuccess() { return exitCode == 0; }
    }

    /**
     * Runs the external KPI-to-JSON converter JAR using the provided inputs.
     */
    public static Result run(File excel,
                             File outputFolder,
                             int aacCount,
                             int residentCount,
                             int volunteerCount,
                             int eventCount,
                             int organizationCount,
                             int locationCount) {
        ZipSecureFile.setMinInflateRatio(Double.parseDouble(MIN_INFLATE_RATIO));
        File jarFile = resolveJarFile(AppState.getJsonConverterJarPath());
        if (jarFile == null || !jarFile.isFile()) {
            String msg = "Converter JAR not found at " + AppState.getJsonConverterJarPath();
            return new Result(1, msg, msg);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(resolveJavaExecutable());
        cmd.add("-jar");
        cmd.add(jarFile.getAbsolutePath());
        cmd.add(excel.getAbsolutePath());
        cmd.add(outputFolder.getAbsolutePath());
        cmd.add(String.valueOf(aacCount));
        cmd.add(String.valueOf(residentCount));
        cmd.add(String.valueOf(volunteerCount));
        cmd.add(String.valueOf(eventCount));
        cmd.add(String.valueOf(organizationCount));
        cmd.add(String.valueOf(locationCount));

        String commandString = toCommandString(cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        try {
            Process proc = pb.start();
            String output = readProcessOutput(proc);
            int exit = proc.waitFor();
            return new Result(exit, output, commandString);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            return new Result(1, message, commandString);
        }
    }

    /**
     * Attempts to detect report counts from the Common sheet if the user leaves fields blank.
     */
    public static ReportCounts detectCounts(File excel) {
        ZipSecureFile.setMinInflateRatio(Double.parseDouble(MIN_INFLATE_RATIO));
        try {
            com.aac.kpi.converter.ExcelOperations ops = new com.aac.kpi.converter.ExcelOperations(excel.getAbsolutePath());
            return ops.detectCountsFromCommonSheet();
        } catch (Exception ex) {
            return null;
        }
    }

    private static File resolveJarFile(String path) {
        if (path == null || path.isBlank()) return null;
        File jar = new File(path);
        if (!jar.isAbsolute()) {
            String cwd = System.getProperty("user.dir");
            jar = new File(cwd, path);
        }
        return jar;
    }

    private static String resolveJavaExecutable() {
        String javaHome = System.getProperty("java.home");
        File javaBin = new File(javaHome, "bin/java");
        if (File.separatorChar == '\\') {
            File exe = new File(javaHome, "bin/java.exe");
            if (exe.isFile()) {
                return exe.getAbsolutePath();
            }
        }
        if (javaBin.isFile()) {
            return javaBin.getAbsolutePath();
        }
        return "java";
    }

    private static String readProcessOutput(Process proc) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            return sb.toString();
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            return "Failed to read process output: " + message;
        }
    }

    private static String toCommandString(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(quoteArg(parts.get(i)));
        }
        return sb.toString();
    }

    private static String quoteArg(String value) {
        Objects.requireNonNull(value);
        if (value.contains(" ") || value.contains("\t")) {
            return "\"" + value + "\"";
        }
        return value;
    }
}
