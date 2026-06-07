package com.tiers.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TiersConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("tiers");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");
    
    public static class Config {
        public boolean enabled = true;
        public boolean showIcons = true;
        public boolean dynamicSeparator = true;
        public String displayedTier = "selected";
        public String previewProfile = "TheRandomizer";
        public String leftGamemode = "all";
        public String rightGamemode = "duels";
        public String iconPreset = "classic";
        public boolean supportCrackedPlayers = true;
        public String portalApiUrl = "https://your-portal.com/api";
    }
    
    private static Config currentConfig = new Config();
    
    public static void load() {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            if (Files.exists(CONFIG_FILE)) {
                try (FileReader reader = new FileReader(CONFIG_FILE.toFile())) {
                    currentConfig = GSON.fromJson(reader, Config.class);
                    if (currentConfig == null) currentConfig = new Config();
                }
            } else save();
        } catch (IOException e) {
            System.err.println("[Tiers] Failed to load config: " + e.getMessage());
            currentConfig = new Config();
        }
    }
    
    public static void save() {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            try (FileWriter writer = new FileWriter(CONFIG_FILE.toFile())) {
                GSON.toJson(currentConfig, writer);
            }
        } catch (IOException e) {
            System.err.println("[Tiers] Failed to save config: " + e.getMessage());
        }
    }
    
    public static Config getConfig() { return currentConfig; }
    public static void setConfig(Config config) { currentConfig = config; save(); }
    public static boolean isEnabled() { return currentConfig.enabled; }
    public static boolean shouldShowIcons() { return currentConfig.showIcons; }
    public static boolean supportsCrackedPlayers() { return currentConfig.supportCrackedPlayers; }
    public static String getPortalApiUrl() { return currentConfig.portalApiUrl; }
}
