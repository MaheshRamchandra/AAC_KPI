package com.aac.kpi.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class RandomDataUtil {
    private static final Random RND = new Random();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ISO_ZONED = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private static final List<String> VENUES = List.of(
            "Community Hall A",
            "Community Hall B",
            "Wellness Centre 1",
            "Wellness Centre 2",
            "Activity Room 3",
            "Zoom Room 1"
    );

    private static final List<String> AAC_CODES = List.of("AAC110", "AAC220", "AAC330", "AAC440", "AAC550");
    private static final List<String> EVENT_TYPES = List.of("Zumba", "Pilates", "Yoga", "TaiChi", "Befriending");
    private static final List<String> FIRST_NAMES = List.of(
            "John","Grace","Siti","Ahmad","Ming","Priya","Daniel","Ling","Rahul","Wei",
            "Jia","Mei","Hui","Kumar","Jasmine","Amelia","David","Nur","Farah","Aiden"
    );
    private static final List<String> LAST_NAMES = List.of(
            "Tan","Lim","Lee","Ng","Goh","Chua","Ong","Wong","Chan","Koh",
            "Foo","Quek","Yeo","Liu","Halim","Rahman","Ismail","Kaur","Singh","Zhang"
    );
    private static final List<String> PRACTITIONER_IDENTIFIER_SYSTEMS = List.of(
            "http://ihis.sg/identifier/nric",
            "http://ihis.sg/identifier/aac-staff-id"
    );
    private static final List<String> GENDERS = List.of("male","female");
    private static final List<String[]> RESIDENTIAL_STATUSES = List.of(
            new String[]{"C", "Citizen"},
            new String[]{"PR", "Permanent Resident"},
            new String[]{"FR", "Foreigner"}
    );
    private static final List<String[]> RACE_CODES = List.of(
            new String[]{"MY", "Malay"},
            new String[]{"CH", "Chinese"},
            new String[]{"IN", "Indian"},
            new String[]{"OT", "Others"}
    );

    private RandomDataUtil() {}

    public static String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String randomPostal6() {
        int n = 100000 + RND.nextInt(900000);
        return String.valueOf(n);
    }

    public static String randomPhone8() {
        int first = RND.nextBoolean() ? 8 : 9; // SG style starting digit
        int rest = RND.nextInt(1_000_0000); // 7 remaining digits (0..9999999)
        return String.format("%d%07d", first, rest);
    }

    public static String randomAAC() {
        return AAC_CODES.get(RND.nextInt(AAC_CODES.size()));
    }

    public static int randomGroup() {
        return 1 + RND.nextInt(3);
    }

    public static String randomType() {
        return RND.nextBoolean() ? "Within" : "Outside";
    }

    public static String randomDOB60Plus() {
        LocalDate today = LocalDate.now();
        LocalDate max = today.minusYears(60); // latest date to be >=60
        LocalDate min = today.minusYears(90); // up to 90 years old
        long days = max.toEpochDay() - min.toEpochDay();
        long offset = (long) (RND.nextDouble() * days);
        return LocalDate.ofEpochDay(min.toEpochDay() + offset).format(DATE);
    }

    public static String dobForExactAge(int ageYears) {
        // Pick a random birthdate such that floor(age) == ageYears
        LocalDate today = LocalDate.now();
        LocalDate max = today.minusYears(ageYears); // youngest date for this age (just turned age)
        LocalDate min = today.minusYears(ageYears + 1).plusDays(1); // oldest date for this age
        if (min.isAfter(max)) { // handle edge cases
            min = max.minusYears(1).plusDays(1);
        }
        long days = Math.max(1, max.toEpochDay() - min.toEpochDay());
        long offset = (long) (RND.nextDouble() * days);
        return LocalDate.ofEpochDay(min.toEpochDay() + offset).format(DATE);
    }

    public static String randomDOBBetweenAges(int minAge, int maxAge) {
        if (minAge > maxAge) { int t = minAge; minAge = maxAge; maxAge = t; }
        int age = minAge + RND.nextInt((maxAge - minAge) + 1);
        return dobForExactAge(age);
    }

    public static String randomEventId() {
        String type = EVENT_TYPES.get(RND.nextInt(EVENT_TYPES.size()));
        int grp = 1 + RND.nextInt(12);
        int idx = 1 + RND.nextInt(99);
        return String.format("%s%02d-%02d", type, grp, idx);
    }

    public static String randomVenue() {
        return VENUES.get(RND.nextInt(VENUES.size()));
    }

    public static int randomCapacity() {
        return 20 + RND.nextInt(81);
    }

    public static String randomVolunteerName() {
        String first = FIRST_NAMES.get(RND.nextInt(FIRST_NAMES.size()));
        String last = LAST_NAMES.get(RND.nextInt(LAST_NAMES.size()));
        return first + " " + last;
    }
    
    public static String randomGender() {
        return GENDERS.get(RND.nextInt(GENDERS.size()));
    }
    
    public static String randomPractitionerIdentifierSystem() {
        return PRACTITIONER_IDENTIFIER_SYSTEMS.get(RND.nextInt(PRACTITIONER_IDENTIFIER_SYSTEMS.size()));
    }

    public static String[] randomResidentialStatus() {
        return RESIDENTIAL_STATUSES.get(RND.nextInt(RESIDENTIAL_STATUSES.size())).clone();
    }

    public static String[] randomRace() {
        return RACE_CODES.get(RND.nextInt(RACE_CODES.size())).clone();
    }

    public static String randomUen() {
        int suffixDigits = RND.nextInt(100_000);
        char suffixLetter = (char) ('A' + RND.nextInt(26));
        return String.format("2025%05d%c", suffixDigits, suffixLetter);
    }

    public static String[] randomEventDateTimeFY2025_26AndDuration() {
        return randomEventDateTimeBetween(LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31));
    }

    public static String[] randomEventDateTimeBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return randomEventDateTimeFY2025_26AndDuration();
        }
        if (endDate.isBefore(startDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }
        long days = endDate.toEpochDay() - startDate.toEpochDay();
        long startOffset = (long) (RND.nextDouble() * (days + 1));

        LocalDate day = LocalDate.ofEpochDay(startDate.toEpochDay() + startOffset);
        int hour = 8 + RND.nextInt(10); // 8..17
        int minute = List.of(0, 15, 30, 45).get(RND.nextInt(4));
        LocalDateTime start = LocalDateTime.of(day, LocalTime.of(hour, minute));

        int durationMin = 30 + RND.nextInt(121); // 30..150
        LocalDateTime end = start.plusMinutes(durationMin);

        return new String[]{start.format(DATE_TIME), end.format(DATE_TIME), String.valueOf(durationMin)};
    }

    public static String randomDateBetween(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) { LocalDate t = start; start = end; end = t; }
        long days = Math.max(1, end.toEpochDay() - start.toEpochDay());
        long offset = (long) (RND.nextDouble() * (days + 1));
        return LocalDate.ofEpochDay(start.toEpochDay() + offset).format(DATE);
    }

    public static int randomInt(int min, int max) {
        if (min > max) { int t = min; min = max; max = t; }
        return min + RND.nextInt((max - min) + 1);
    }

    public static String randomIsoTimestampBetweenWithOffset(java.time.LocalDateTime start,
                                                             java.time.LocalDateTime end,
                                                             String zoneOffset) {
        java.time.ZoneOffset off = java.time.ZoneOffset.of(zoneOffset);
        long startSec = start.toEpochSecond(off);
        long endSec = end.toEpochSecond(off);
        if (endSec <= startSec) endSec = startSec + 3600; // at least 1h
        long randomSec = startSec + (long) ((endSec - startSec) * RND.nextDouble());
        java.time.LocalDateTime dt = java.time.LocalDateTime.ofEpochSecond(randomSec, 0, off);
        return dt.atOffset(off).format(ISO_ZONED);
    }
}
