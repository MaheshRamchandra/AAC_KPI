package com.aac.kpi.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class JsonExportService {
    private JsonExportService() {}

    // Allow larger compression ratios when reading XLSX files in the external converter JAR
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

    public static Result run(File jar,
                             File excel,
                             File outputFolder,
                             int aacCount,
                             int residentCount,
                             int volunteerCount,
                             int eventCount,
                             int organizationCount,
                             int locationCount) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        command.add(javaBin);
        command.add("-Dpoi.min.inflate.ratio=" + MIN_INFLATE_RATIO);
        command.add("-Dorg.apache.poi.openxml4j.util.ZipSecureFile.minInflateRatio=" + MIN_INFLATE_RATIO);
        command.add("-jar");
        command.add(jar.getAbsolutePath());
        command.add(excel.getAbsolutePath());
        command.add(outputFolder.getAbsolutePath());
        command.add(String.valueOf(aacCount));
        command.add(String.valueOf(residentCount));
        command.add(String.valueOf(volunteerCount));
        command.add(String.valueOf(eventCount));
        command.add(String.valueOf(organizationCount));
        command.add(String.valueOf(locationCount));

        ProcessBuilder pb = new ProcessBuilder(command);
        // Also set via JAVA_TOOL_OPTIONS in case the JVM ignores command-line args (defensive)
        String javaToolOptions = pb.environment().getOrDefault("JAVA_TOOL_OPTIONS", "");
        String overrideProps = "-Dpoi.min.inflate.ratio=" + MIN_INFLATE_RATIO + " "
                + "-Dorg.apache.poi.openxml4j.util.ZipSecureFile.minInflateRatio=" + MIN_INFLATE_RATIO;
        pb.environment().put("JAVA_TOOL_OPTIONS",
                (javaToolOptions.isBlank() ? overrideProps : javaToolOptions + " " + overrideProps).trim());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = readStream(process.getInputStream());
        int exitCode = process.waitFor();
        return new Result(exitCode, output, String.join(" ", command));
    }

    private static String readStream(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            return sb.toString();
        }
    }
}
