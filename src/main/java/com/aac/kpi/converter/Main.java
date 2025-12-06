package com.aac.kpi.converter;

public class Main {
    /**
     * CLI entry point maintained for backwards compatibility.
     *
     * @param args args[0] - File path to input Excel file.
     *             args[1] - File path to output folder.
     *             args[2] - Number of AAC reports.
     *             args[3] - Number of resident reports.
     *             args[4] - Number of volunteer attendance reports.
     *             args[5] - Number of event reports.
     *             args[6] - Number of organization reports.
     *             args[7] - Number of location reports.
     */
    public static void main(String[] args) throws Exception {
        ReportRunner runner = new ReportRunner();
        ReportConfig config = ReportConfig.fromArgs(args);
        runner.generateReports(config);
    }
}
