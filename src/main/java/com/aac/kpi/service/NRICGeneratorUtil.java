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

/**
 * NRIC generator utility.
 * - generateNRIC(): tries Selenium (headless Chrome) against samliew.com; falls back to offline generator on failure.
 * - generateFakeNRIC(): offline deterministic NRIC (Singapore-like checksum) for local use.
 */
public final class NRICGeneratorUtil {
    private static final Random RND = new Random();

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
        char prefix = RND.nextBoolean() ? 'S' : 'T';
        int[] digits = new int[7];
        for (int i = 0; i < 7; i++) digits[i] = RND.nextInt(10);

        int[] weights = {2, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 7; i++) sum += digits[i] * weights[i];
        if (prefix == 'T') sum += 4; // offset for T

        int remainder = sum % 11;
        // Mapping per spec for S/T
        char[] map = {'A','B','C','D','E','F','G','H','I','Z','J'};
        char checksum = map[10 - remainder];

        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        for (int d : digits) sb.append(d);
        sb.append(checksum);
        return sb.toString();
    }
}
