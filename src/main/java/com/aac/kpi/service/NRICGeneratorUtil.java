package com.aac.kpi.service;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * NRIC generator utility.
 * - generateNRIC(): tries Selenium (headless Chrome) against samliew.com; falls back to offline generator on failure.
 * - generateFakeNRIC(): offline deterministic NRIC (Singapore-like checksum) for local use, produced sequentially and cached to avoid repeats.
 */
public final class NRICGeneratorUtil {
    private static final Random RND = new Random();
    private static final AtomicLong SEQUENTIAL_FAKE_COUNTER = new AtomicLong(1_000_000L);
    private static final Set<String> SEEN_FAKE_NRIC = ConcurrentHashMap.newKeySet();

    private NRICGeneratorUtil() {}

    public static String generateNRIC() {
        try {
            // Use the provided Selenium headless code path with 'F' prefix by default
            return generateNRICViaSelenium("F");
        } catch (Throwable ignored) {
            // Any failure falls through to offline generation.
        }
        return generateFakeNRIC();
    }

    public static String generateByMode(NricMode mode) {
        if (mode == null) mode = NricMode.DUMMY;
        switch (mode) {
            case ONLINE:
                return generateNRIC();
            case DUMMY:
            default:
                return generateFakeNRIC();
        }
    }

    private static String generateNRICViaSelenium(String prefixVisibleText) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        // Optional hardening flags
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage");

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.get("https://samliew.com/nric-generator");

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            WebElement prefixSelectElement = driver.findElement(By.id("firstchar"));
            Select prefixSelect = new Select(prefixSelectElement);
            prefixSelect.selectByVisibleText(prefixVisibleText);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement generateButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("gen"))
            );
            generateButton.click();

            WebElement nricInput = driver.findElement(By.id("nric"));
            String generatedNric = nricInput.getAttribute("value");
            if (generatedNric == null || generatedNric.isBlank()) {
                throw new IllegalStateException("Empty NRIC from generator");
            }
            return generatedNric;
        } finally {
            driver.quit();
        }
    }

    /**
     * Offline NRIC-like generator using checksum rules for S/T.
     * This produces values that pass the checksum format but are synthetic.
     */
    public static String generateFakeNRIC() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = attempt < 10 ? nextSequentialFakeNric() : nextRandomFakeNric();
            if (SEEN_FAKE_NRIC.add(candidate)) {
                return candidate;
            }
        }
        String fallback = nextRandomFakeNric();
        SEEN_FAKE_NRIC.add(fallback);
        return fallback;
    }

    private static String nextSequentialFakeNric() {
        long seq = SEQUENTIAL_FAKE_COUNTER.getAndUpdate(cur -> cur >= 9_999_999L ? 1_000_000L : cur + 1);
        String digits = String.format("%07d", seq % 10_000_000L);
        char prefix = (seq % 2 == 0) ? 'S' : 'T';
        return withChecksum(prefix, digits);
    }

    private static String nextRandomFakeNric() {
        char prefix = RND.nextBoolean() ? 'S' : 'T';
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < 7; i++) digits.append(RND.nextInt(10));
        return withChecksum(prefix, digits.toString());
    }

    private static String withChecksum(char prefix, String digitString) {
        int[] weights = {2, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            int digit = Character.digit(digitString.charAt(i), 10);
            sum += digit * weights[i];
        }
        if (prefix == 'T') sum += 4; // offset for T

        int remainder = sum % 11;
        char[] map = {'A','B','C','D','E','F','G','H','I','Z','J'};
        char checksum = map[10 - remainder];
        return "" + prefix + digitString + checksum;
    }
}
