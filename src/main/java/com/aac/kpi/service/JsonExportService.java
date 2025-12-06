package com.aac.kpi.service;

import com.aac.kpi.converter.ReportConfig;
import com.aac.kpi.converter.ReportCounts;
import com.aac.kpi.converter.ReportRunner;
import org.apache.poi.openxml4j.util.ZipSecureFile;

import java.io.File;

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
     * Runs the embedded KPI-to-JSON converter directly (no external JAR).
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
        String command = "embedded: ReportRunner.generateReports";
        try {
            ReportRunner runner = new ReportRunner();
            ReportConfig config = new ReportConfig(
                    excel.getAbsolutePath(),
                    outputFolder.getAbsolutePath(),
                    aacCount,
                    residentCount,
                    volunteerCount,
                    eventCount,
                    organizationCount,
                    locationCount
            );
            runner.generateReports(config);
            return new Result(0,
                    "Reports generated to " + outputFolder.getAbsolutePath(),
                    command);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            return new Result(1, message, command);
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
}
