package com.aac.kpi.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Generates a Practitioner_Master.xlsx with realistic practitioner records.
 * Sheet name: Practitioner_Master
 */
public final class PractitionerMasterGenerator {
    private static final Random RND = new SecureRandom();

    private static final List<String> SYSTEMS = List.of(
            "http://ihis.sg/identifier/aac-staff-id",
            "http://ihis.sg/identifier/nric"
    );

    private static final List<String> POSITIONS = List.of(
            "Volunteer",
            "Volunteer Coordinator",
            "Admin Staff",
            "Event Organizer",
            "Social Worker",
            "Healthcare Assistant",
            "Programme Coordinator",
            "Event Coordinator"
    );

    private static final List<String> REMARKS = List.of(
            "Activities/Events Volunteer",
            "Support Staff",
            "Healthcare outreach",
            "Community engagement",
            "Programme logistics"
    );

    private static final String[] SURNAMES = {
            "Tan","Lim","Lee","Ng","Goh","Teo","Chua","Ong","Wong","Chan",
            "Koh","Toh","Chew","Chong","Foo","Quek","Yeo","Liu","Halim","Rahman",
            "Ismail","Kaur","Singh","Zhang","Chen","Huang","Wang","Zhao","Loh","Chee"
    };
    private static final String[] GIVEN = {
            "Grace","Sarah","Rebecca","Emily","Rachel","Kevin","Daniel","Dennis","Marcus","Ryan",
            "Wei","Ping","Hui","Xiao","Ying","Jun","Ah","Chong","Mei","Kai",
            "Arun","Vijay","Priya","Siti","Aisyah","Farah","Hana","Nur","Amir","Faizal"
    };

    private PractitionerMasterGenerator() {}

    public static File defaultOutputPath() {
        String home = System.getProperty("user.home");
        return new File(home + File.separator + "Documents" + File.separator + "Practitioner_Master.xlsx");
    }

    public static File generate(int count, File file) throws IOException {
        if (file == null) file = defaultOutputPath();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Practitioner_Master");

            String[] headers = new String[]{
                    "practitioner_id",
                    "practitioner_identifier_value",
                    "practitioner_identifier_system",
                    "practitioner_manpower_position",
                    "practitioner_volunteer_name",
                    "practitioner_manpower_capacity",
                    "practitioner_volunteer_age",
                    "Working Remarks"
            };
            createHeaderRow(sheet, headers);

            int startSuffix = 500 + RND.nextInt(400); // e.g., 500..899 for F-suffix
            for (int i = 0; i < count; i++) {
                Row row = sheet.createRow(i + 1);
                int c = 0;
                String practitionerId = RandomDataUtil.uuid32().toUpperCase(Locale.ROOT)
                        + "F" + String.format(Locale.ROOT, "%03d", (startSuffix + i));
                row.createCell(c++).setCellValue(practitionerId);

                String identifierVal = randomPractitionerIdentifier();
                row.createCell(c++).setCellValue(identifierVal);

                String idSystem = SYSTEMS.get(RND.nextInt(SYSTEMS.size()));
                row.createCell(c++).setCellValue(idSystem);

                String position = POSITIONS.get(RND.nextInt(POSITIONS.size()));
                row.createCell(c++).setCellValue(position);

                String name = randomName();
                row.createCell(c++).setCellValue(name);

                double capacity = (4 + RND.nextInt(7)) / 10.0; // 0.4 .. 1.0 step 0.1
                row.createCell(c++).setCellValue(capacity);

                int age = 20 + RND.nextInt(61); // 20 .. 80
                row.createCell(c++).setCellValue(age);

                String remark = (RND.nextBoolean() ? (RND.nextBoolean() ? "An " : "A ") : "")
                        + REMARKS.get(RND.nextInt(REMARKS.size()));
                row.createCell(c).setCellValue(remark);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            if (file.getParentFile() != null && !file.getParentFile().exists()) file.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(file)) { wb.write(out); }
            return file;
        }
    }

    private static void createHeaderRow(Sheet sheet, String[] headers) {
        Row header = sheet.createRow(0);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private static String randomName() {
        // 2–3 parts: Surname + Given [ + Given2 ]
        String surname = SURNAMES[RND.nextInt(SURNAMES.length)];
        String g1 = GIVEN[RND.nextInt(GIVEN.length)];
        if (RND.nextBoolean()) {
            return surname + " " + g1;
        } else {
            String g2 = GIVEN[RND.nextInt(GIVEN.length)];
            // Avoid duplicate parts
            if (g2.equals(g1)) g2 = "" + g2 + " Jr"; // ensure variation
            return surname + " " + g1 + " " + g2;
        }
    }

    private static String randomPractitionerIdentifier() {
        // NRIC-like or staff code: prefix S/T/SU + 6–7 digits + optional checksum letter
        int pick = RND.nextInt(3); // 0=S,1=T,2=SU
        String prefix = pick == 2 ? "SU" : (pick == 0 ? "S" : "T");
        int len = 6 + RND.nextInt(2); // 6 or 7
        if (prefix.equals("SU")) {
            // allow shorter occasionally to mimic staff codes
            len = RND.nextInt(100) < 25 ? 3 + RND.nextInt(3) : len; // 3..5 sometimes
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < len; i++) digits.append(RND.nextInt(10));
        String base = prefix + digits;
        if (!prefix.equals("SU") && RND.nextBoolean()) {
            // add checksum letter (fake but plausible)
            char checksum = (char) ('A' + RND.nextInt(26));
            base = base + checksum;
        }
        return base;
    }
}

