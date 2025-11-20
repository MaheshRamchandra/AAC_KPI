package com.aac.kpi.service;

import com.aac.kpi.model.RuleGraph;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;

/**
 * Persistence for the rule graph that drives the Dynamic Rule-Based Data Mapping UI.
 * Stores only rule metadata (no real data values).
 */
public final class RuleGraphService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_PATH = "config/rule-graph.json";

    private RuleGraphService() {}

    public static File defaultFile() {
        return new File(DEFAULT_PATH);
    }

    public static RuleGraph load() {
        return load(defaultFile());
    }

    public static RuleGraph load(File file) {
        if (file == null) {
            return RuleGraph.defaults();
        }
        try {
            if (!file.exists()) {
                return RuleGraph.defaults();
            }
            try (FileReader reader = new FileReader(file)) {
                RuleGraph g = GSON.fromJson(reader, RuleGraph.class);
                return g == null ? RuleGraph.defaults() : g;
            }
        } catch (Exception ex) {
            return RuleGraph.defaults();
        }
    }

    public static void save(RuleGraph graph) throws Exception {
        save(graph, defaultFile());
    }

    public static void save(RuleGraph graph, File file) throws Exception {
        if (graph == null) graph = RuleGraph.defaults();
        if (file == null) file = defaultFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            Files.createDirectories(parent.toPath());
        }
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(graph, writer);
        }
    }

    public static RuleGraph ensureFile() {
        File f = defaultFile();
        if (!f.exists()) {
            try {
                save(RuleGraph.defaults(), f);
            } catch (Exception ignored) {}
        }
        return load(f);
    }
}
