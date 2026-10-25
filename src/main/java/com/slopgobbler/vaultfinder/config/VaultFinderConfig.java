package com.slopgobbler.vaultfinder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration manager for VaultFinder Pro.
 * Provides configuration options for highlighting range, 3D radar, shulker peek,
 * block X-Ray, matching colors, and JSON serialization to .minecraft/config/vaultfinder.json.
 */
public class VaultFinderConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILENAME = "vaultfinder.json";

    public static final int MIN_RANGE = 8;
    public static final int MAX_RANGE = 256;
    public static final int DEFAULT_RANGE = 64;

    // Default Colors (ARGB format)
    public static final int DEFAULT_MATCHING_COLOR = 0xFF00FF7F;       // Spring Green
    public static final int DEFAULT_PARTIAL_COLOR = 0xFFFFFF00;        // Bright Yellow

    // Singleton instance
    private static VaultFinderConfig INSTANCE = new VaultFinderConfig();
    private static File customConfigFile = null;

    // Configuration Properties
    public int highlightRange = DEFAULT_RANGE;
    public boolean radarEnabled = true;
    public boolean shulkerPeekEnabled = true;
    public boolean xrayEnabled = true;
    public int matchingColor = DEFAULT_MATCHING_COLOR;
    public int partialMatchingColor = DEFAULT_PARTIAL_COLOR;

    // Backwards-compatible aliases
    public int maxRadarDistance = DEFAULT_RANGE;
    public boolean xrayHighlights = true;
    public boolean autoRestockHotbar = false;
    public boolean showDistanceTags = true;
    public int highlightColorR = (DEFAULT_MATCHING_COLOR >> 16) & 0xFF;
    public int highlightColorG = (DEFAULT_MATCHING_COLOR >> 8) & 0xFF;
    public int highlightColorB = DEFAULT_MATCHING_COLOR & 0xFF;

    public VaultFinderConfig() {
    }

    /**
     * Returns the singleton config instance.
     */
    public static VaultFinderConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new VaultFinderConfig();
        }
        return INSTANCE;
    }

    public static VaultFinderConfig getInstance() {
        return get();
    }

    public static void setInstance(VaultFinderConfig config) {
        if (config != null) {
            INSTANCE = config;
        }
    }

    /**
     * Sets a custom config file path (useful for testing and custom environments).
     */
    public static void setCustomConfigFile(File file) {
        customConfigFile = file;
    }

    /**
     * Resolves the target configuration file located in .minecraft/config/vaultfinder.json.
     */
    public static File getConfigFile() {
        if (customConfigFile != null) {
            return customConfigFile;
        }

        // 1. Try FabricLoader configuration directory if available
        try {
            if (FabricLoader.getInstance() != null && FabricLoader.getInstance().getConfigDir() != null) {
                Path dir = FabricLoader.getInstance().getConfigDir();
                return dir.resolve(CONFIG_FILENAME).toFile();
            }
        } catch (Throwable ignored) {
            // Outside of active FabricLoader runtime (e.g., standalone CLI/JUnit runner)
        }

        // 2. Resolve standard .minecraft directory location
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            File mcConfig = Paths.get(appData, ".minecraft", "config", CONFIG_FILENAME).toFile();
            return mcConfig;
        }

        String userHome = System.getProperty("user.home", ".");
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", "minecraft", "config", CONFIG_FILENAME).toFile();
        } else if (os.contains("win")) {
            return Paths.get(userHome, "AppData", "Roaming", ".minecraft", "config", CONFIG_FILENAME).toFile();
        } else {
            return Paths.get(userHome, ".minecraft", "config", CONFIG_FILENAME).toFile();
        }
    }

    /**
     * Loads configuration from vaultfinder.json. If absent, saves defaults.
     */
    public synchronized void load() {
        File file = getConfigFile();
        if (!file.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            if (json.has("highlightRange")) {
                setHighlightRange(json.get("highlightRange").getAsInt());
            } else if (json.has("maxRadarDistance")) {
                setHighlightRange(json.get("maxRadarDistance").getAsInt());
            }

            if (json.has("radarEnabled")) {
                this.radarEnabled = json.get("radarEnabled").getAsBoolean();
            } else if (json.has("enable3dRadar")) {
                this.radarEnabled = json.get("enable3dRadar").getAsBoolean();
            }

            if (json.has("shulkerPeekEnabled")) {
                this.shulkerPeekEnabled = json.get("shulkerPeekEnabled").getAsBoolean();
            } else if (json.has("enableShulkerPeek")) {
                this.shulkerPeekEnabled = json.get("enableShulkerPeek").getAsBoolean();
            }

            if (json.has("xrayEnabled")) {
                this.xrayEnabled = json.get("xrayEnabled").getAsBoolean();
            } else if (json.has("xrayHighlights")) {
                this.xrayEnabled = json.get("xrayHighlights").getAsBoolean();
            } else if (json.has("xrayThroughBlocks")) {
                this.xrayEnabled = json.get("xrayThroughBlocks").getAsBoolean();
            }

            if (json.has("matchingColor")) {
                this.matchingColor = parseColor(json.get("matchingColor").getAsString(), DEFAULT_MATCHING_COLOR);
            }
            if (json.has("partialMatchingColor")) {
                this.partialMatchingColor = parseColor(json.get("partialMatchingColor").getAsString(), DEFAULT_PARTIAL_COLOR);
            }

            if (json.has("autoRestockHotbar")) {
                this.autoRestockHotbar = json.get("autoRestockHotbar").getAsBoolean();
            }
            if (json.has("showDistanceTags")) {
                this.showDistanceTags = json.get("showDistanceTags").getAsBoolean();
            }

            syncFields();
            System.out.println("[VaultFinder Pro] Configuration loaded successfully from " + file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[VaultFinder Pro] Error loading configuration: " + e.getMessage());
        }
    }

    /**
     * Saves configuration to vaultfinder.json.
     */
    public synchronized void save() {
        File file = getConfigFile();
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            syncFields();

            JsonObject json = new JsonObject();
            json.addProperty("highlightRange", this.highlightRange);
            json.addProperty("radarEnabled", this.radarEnabled);
            json.addProperty("shulkerPeekEnabled", this.shulkerPeekEnabled);
            json.addProperty("xrayEnabled", this.xrayEnabled);
            json.addProperty("matchingColor", toHexColor(this.matchingColor));
            json.addProperty("partialMatchingColor", toHexColor(this.partialMatchingColor));
            json.addProperty("autoRestockHotbar", this.autoRestockHotbar);
            json.addProperty("showDistanceTags", this.showDistanceTags);

            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
            System.out.println("[VaultFinder Pro] Configuration saved successfully to " + file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[VaultFinder Pro] Error saving configuration: " + e.getMessage());
        }
    }

    public static VaultFinderConfig loadConfig() {
        VaultFinderConfig cfg = get();
        cfg.load();
        return cfg;
    }

    public static void saveConfig() {
        get().save();
    }

    private void syncFields() {
        this.maxRadarDistance = this.highlightRange;
        this.xrayHighlights = this.xrayEnabled;
        this.highlightColorR = (this.matchingColor >> 16) & 0xFF;
        this.highlightColorG = (this.matchingColor >> 8) & 0xFF;
        this.highlightColorB = this.matchingColor & 0xFF;
    }

    // Getters and Setters with Range Clamping
    public int getHighlightRange() {
        return highlightRange;
    }

    public void setHighlightRange(int highlightRange) {
        this.highlightRange = Math.max(MIN_RANGE, Math.min(MAX_RANGE, highlightRange));
        this.maxRadarDistance = this.highlightRange;
    }

    public int getMaxRadarDistance() {
        return getHighlightRange();
    }

    public void setMaxRadarDistance(int distance) {
        setHighlightRange(distance);
    }

    public boolean isRadarEnabled() {
        return radarEnabled;
    }

    public void setRadarEnabled(boolean radarEnabled) {
        this.radarEnabled = radarEnabled;
    }

    public boolean isShulkerPeekEnabled() {
        return shulkerPeekEnabled;
    }

    public void setShulkerPeekEnabled(boolean shulkerPeekEnabled) {
        this.shulkerPeekEnabled = shulkerPeekEnabled;
    }

    public boolean isXrayEnabled() {
        return xrayEnabled;
    }

    public void setXrayEnabled(boolean xrayEnabled) {
        this.xrayEnabled = xrayEnabled;
        this.xrayHighlights = xrayEnabled;
    }

    public int getMatchingColor() {
        return matchingColor;
    }

    public void setMatchingColor(int matchingColor) {
        this.matchingColor = matchingColor;
        syncFields();
    }

    public int getPartialMatchingColor() {
        return partialMatchingColor;
    }

    public void setPartialMatchingColor(int partialMatchingColor) {
        this.partialMatchingColor = partialMatchingColor;
    }

    public String getMatchingColorHex() {
        return toHexColor(matchingColor);
    }

    public void setMatchingColorHex(String hex) {
        this.matchingColor = parseColor(hex, DEFAULT_MATCHING_COLOR);
        syncFields();
    }

    public String getPartialMatchingColorHex() {
        return toHexColor(partialMatchingColor);
    }

    public void setPartialMatchingColorHex(String hex) {
        this.partialMatchingColor = parseColor(hex, DEFAULT_PARTIAL_COLOR);
    }

    // Color channel extraction helpers
    public int getMatchingColorA() { return (matchingColor >> 24) & 0xFF; }
    public int getMatchingColorR() { return (matchingColor >> 16) & 0xFF; }
    public int getMatchingColorG() { return (matchingColor >> 8) & 0xFF; }
    public int getMatchingColorB() { return matchingColor & 0xFF; }

    public int getPartialMatchingColorA() { return (partialMatchingColor >> 24) & 0xFF; }
    public int getPartialMatchingColorR() { return (partialMatchingColor >> 16) & 0xFF; }
    public int getPartialMatchingColorG() { return (partialMatchingColor >> 8) & 0xFF; }
    public int getPartialMatchingColorB() { return partialMatchingColor & 0xFF; }

    public float[] getMatchingColorFloats() {
        return new float[]{
                getMatchingColorR() / 255.0f,
                getMatchingColorG() / 255.0f,
                getMatchingColorB() / 255.0f,
                getMatchingColorA() / 255.0f
        };
    }

    public float[] getPartialMatchingColorFloats() {
        return new float[]{
                getPartialMatchingColorR() / 255.0f,
                getPartialMatchingColorG() / 255.0f,
                getPartialMatchingColorB() / 255.0f,
                getPartialMatchingColorA() / 255.0f
        };
    }

    public static String toHexColor(int color) {
        return String.format("#%08X", color);
    }

    public static int parseColor(String str, int defaultVal) {
        if (str == null || str.isBlank()) {
            return defaultVal;
        }
        try {
            String clean = str.trim();
            if (clean.startsWith("#")) {
                clean = clean.substring(1);
            } else if (clean.startsWith("0x") || clean.startsWith("0X")) {
                clean = clean.substring(2);
            }
            if (clean.length() == 6) {
                // Prepend full alpha FF
                return (int) (0xFF000000L | Long.parseLong(clean, 16));
            } else if (clean.length() == 8) {
                return (int) Long.parseLong(clean, 16);
            }
        } catch (Exception ignored) {
        }
        return defaultVal;
    }
}
