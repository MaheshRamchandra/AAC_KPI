package com.aac.kpi.service;

import com.aac.kpi.model.RulesConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;

/**
 * Loads and saves user-editable KPI rules/config to a JSON file. Defaults are generated
 * on first use so non-coders can tweak thresholds without touching the rule engine.
 */
public final class RulesConfigService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_PATH = "config/rules-config.json";

    private RulesConfigService() {}

    public static File defaultFile() {
        return new File(DEFAULT_PATH);
    }

    public static RulesConfig load() {
        return load(defaultFile());
    }

    public static RulesConfig load(File file) {
        if (file == null) {
            return RulesConfig.defaults();
        }
        try {
            if (!file.exists()) {
                return RulesConfig.defaults();
            }
            try (FileReader reader = new FileReader(file)) {
                RulesConfig cfg = GSON.fromJson(reader, RulesConfig.class);
                return cfg == null ? RulesConfig.defaults() : cfg;
            }
        } catch (Exception ex) {
            return RulesConfig.defaults();
        }
    }

    public static void save(RulesConfig cfg) throws Exception {
        save(cfg, defaultFile());
    }

    public static void save(RulesConfig cfg, File file) throws Exception {
        if (cfg == null) {
            cfg = RulesConfig.defaults();
        }
        if (file == null) {
            file = defaultFile();
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            Files.createDirectories(parent.toPath());
        }
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(cfg, writer);
        }
    }

    public static RulesConfig ensureFile() {
        File f = defaultFile();
        if (!f.exists()) {
            try {
                save(RulesConfig.defaults(), f);
            } catch (Exception ignored) {}
        }
        return load(f);
    }
}
