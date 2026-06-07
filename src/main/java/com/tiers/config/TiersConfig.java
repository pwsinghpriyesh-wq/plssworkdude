package com.tiers.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TiersConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("tiers");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");
    private static final Logger LOGGER = LoggerFactory.getLogger(TiersConfig.class);
    
    // CRITICAL: This is the ONLY API URL in the entire mod
    // The website at portal-tiers.netlify.app is FRONTEND ONLY
    // Use a proper backend endpoint that returns JSON tier data
    // Expected format: GET /api/player?name=PlayerName OR /api/player/uuid/UUID
    // Response: {"player":"Name","uuid":"uuid","sword":"HT1","axe":"LT2",...}
    public static final String DEFAULT_PORTAL_API_URL = "https://api.portal-tiers.example.com";
    
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
        // NOTE: Replace with actual backend API URL
        // This must point to a backend that serves JSON tier data
        public String portalApiUrl = DEFAULT_PORTAL_API_URL;
    }
    
    private static Config currentConfig = new Config();
    
    public static void load() {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            if (Files.exists(CONFIG_FILE)) {
                try (FileReader reader = new FileReader(CONFIG_FILE.toFile())) {
                    currentConfig = GSON.fromJson(reader, Config.class);
                    if (currentConfig == null) currentConfig = new Config();
                    LOGGER.info("[Tiers] Config loaded. Portal API URL: {}", currentConfig.portalApiUrl);
                    validateApiUrl();
                }
            } else {
                LOGGER.info("[Tiers] No config found, creating default config");
                save();
            }
        } catch (IOException e) {
            LOGGER.error("[Tiers] Failed to load config: {}", e.getMessage(), e);
            currentConfig = new Config();
        }
    }
    
    private static void validateApiUrl() {
        if (currentConfig.portalApiUrl == null || currentConfig.portalApiUrl.contains("your-portal") || currentConfig.portalApiUrl.contains("example.com")) {
            LOGGER.warn("[Tiers] ⚠️ CRITICAL: Portal API URL is not set! Using: {}", currentConfig.portalApiUrl);
            LOGGER.warn("[Tiers] ⚠️ Player tiers will NOT be fetched until you configure a valid API endpoint");
            LOGGER.warn("[Tiers] ⚠️ Edit: .minecraft/config/tiers/config.json and set 'portalApiUrl' to your backend API");
        }
    }
    
    public static void save() {
        try {
            if (!Files.exists(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            try (FileWriter writer = new FileWriter(CONFIG_FILE.toFile())) {
                GSON.toJson(currentConfig, writer);
                LOGGER.info("[Tiers] Config saved to: {}", CONFIG_FILE);
            }
        } catch (IOException e) {
            LOGGER.error("[Tiers] Failed to save config: {}", e.getMessage(), e);
        }
    }
    
    public static Config getConfig() { return currentConfig; }
    public static void setConfig(Config config) { currentConfig = config; save(); }
    public static boolean isEnabled() { return currentConfig.enabled; }
    public static boolean shouldShowIcons() { return currentConfig.showIcons; }
    public static boolean supportsCrackedPlayers() { return currentConfig.supportCrackedPlayers; }
    public static String getPortalApiUrl() { return currentConfig.portalApiUrl; }
}
