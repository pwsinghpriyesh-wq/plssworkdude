package com.tiers.profile.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tiers.api.WebsiteApiClient;
import com.tiers.profile.GameMode;
import com.tiers.profile.Status;
import com.tiers.misc.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class WebsiteProfile extends SuperProfile {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebsiteProfile.class);
    
    private final WebsiteApiClient apiClient;
    private final Map<String, String> tierMap = new HashMap<>();
    private String playerUuid;
    private String playerUsername;
    
    public WebsiteProfile(WebsiteApiClient apiClient) {
        super();
        this.apiClient = apiClient;
        addGamemodes();
    }
    
    /**
     * Constructor with UUID (for premium players)
     */
    public WebsiteProfile(WebsiteApiClient apiClient, String uuid) {
        super();
        this.apiClient = apiClient;
        this.playerUuid = uuid;
        addGamemodes();
        fetchPlayerTiersByUuid(uuid);
    }
    
    /**
     * Constructor with UUID and username (for fallback support)
     */
    public WebsiteProfile(WebsiteApiClient apiClient, String uuid, String username) {
        super();
        this.apiClient = apiClient;
        this.playerUuid = uuid;
        this.playerUsername = username;
        addGamemodes();
        fetchPlayerTiersWithFallback(uuid, username);
    }
    
    private void addGamemodes() {
        gameModes.add(new GameMode(Mode.PVPTIERS_SWORD, "sword"));
        gameModes.add(new GameMode(Mode.PVPTIERS_AXE, "axe"));
        gameModes.add(new GameMode(Mode.PVPTIERS_MACE, "mace"));
        gameModes.add(new GameMode(Mode.PVPTIERS_UHC, "uhc"));
    }
    
    /**
     * Fetch player tiers by UUID only (premium players)
     */
    private void fetchPlayerTiersByUuid(String uuid) {
        status = Status.LOADING;
        apiClient.fetchPlayerTiers(uuid).thenAccept(tierData -> {
            if (tierData != null) {
                parseApiResponse(tierData);
                status = Status.READY;
                LOGGER.info("Successfully fetched tiers for UUID: {}", uuid);
            } else {
                status = Status.ERROR;
                LOGGER.warn("Failed to fetch tiers for UUID: {}", uuid);
            }
        }).exceptionally(e -> {
            LOGGER.error("Exception fetching player tiers by UUID", e);
            status = Status.ERROR;
            return null;
        });
    }
    
    /**
     * Fetch player tiers with UUID->Username fallback (mixed servers)
     * This supports both premium and cracked players
     */
    private void fetchPlayerTiersWithFallback(String uuid, String username) {
        status = Status.LOADING;
        
        // Try UUID first (premium players)
        apiClient.fetchPlayerTiers(uuid).thenAccept(tierData -> {
            if (tierData != null) {
                parseApiResponse(tierData);
                status = Status.READY;
                LOGGER.info("Successfully fetched tiers by UUID for: {}", username);
            } else {
                // UUID failed, try username (cracked players)
                LOGGER.info("UUID lookup failed for {}, attempting username lookup...", username);
                fetchPlayerTiersByUsername(username);
            }
        }).exceptionally(e -> {
            LOGGER.warn("UUID lookup failed, trying username fallback: {}", username);
            fetchPlayerTiersByUsername(username);
            return null;
        });
    }
    
    /**
     * Fetch player tiers by username (cracked players)
     */
    private void fetchPlayerTiersByUsername(String username) {
        status = Status.LOADING;
        apiClient.fetchPlayerTiersByUsername(username).thenAccept(tierData -> {
            if (tierData != null) {
                parseApiResponse(tierData);
                status = Status.READY;
                LOGGER.info("Successfully fetched tiers by username: {}", username);
            } else {
                status = Status.ERROR;
                LOGGER.warn("Failed to fetch tiers for username: {}", username);
            }
        }).exceptionally(e -> {
            LOGGER.error("Exception fetching player tiers by username", e);
            status = Status.ERROR;
            return null;
        });
    }
    
    /**
     * Parse API response and populate tier data
     */
    private void parseApiResponse(WebsiteApiClient.PlayerTierData tierData) {
        try {
            // Map API response to internal tier system
            tierMap.put("sword", tierData.sword != null ? tierData.sword : "Unranked");
            tierMap.put("axe", tierData.axe != null ? tierData.axe : "Unranked");
            tierMap.put("mace", tierData.mace != null ? tierData.mace : "Unranked");
            tierMap.put("uhc", tierData.uhc != null ? tierData.uhc : "Unranked");
            
            playerUuid = tierData.uuid;
            playerUsername = tierData.username;
            
            // Convert simple tier strings (like "HT1", "LT2") to GameMode tier data
            parseTierStrings();
            
            originalJson = tierData.toString();
            LOGGER.info("Parsed tier data for player: {}", tierData.username);
        } catch (Exception e) {
            LOGGER.error("Error parsing API response", e);
            status = Status.ERROR;
        }
    }
    
    /**
     * Parse tier strings and update GameMode objects
     */
    private void parseTierStrings() {
        for (GameMode gameMode : gameModes) {
            String tierStr = tierMap.get(gameMode.parsingName);
            if (tierStr != null && !tierStr.equalsIgnoreCase("Unranked") && !tierStr.isEmpty()) {
                try {
                    // Create a JSON object mimicking the API response format
                    JsonObject tierJson = new JsonObject();
                    tierJson.addProperty("tier", extractTierNumber(tierStr));
                    tierJson.addProperty("pos", isHighTier(tierStr) ? "0" : "1");
                    tierJson.addProperty("peak_tier", extractTierNumber(tierStr));
                    tierJson.addProperty("peak_pos", isHighTier(tierStr) ? "0" : "1");
                    tierJson.addProperty("attained", String.valueOf(System.currentTimeMillis() / 1000));
                    tierJson.addProperty("retired", "false");
                    
                    gameMode.parseTiers(tierJson);
                    LOGGER.debug("Parsed tier for {}: {}", gameMode.parsingName, tierStr);
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse tier string: {}", tierStr, e);
                    gameMode.status = Status.NOT_EXISTING;
                }
            } else {
                gameMode.status = Status.NOT_EXISTING;
            }
        }
    }
    
    /**
     * Extract tier number from strings like "HT1", "LT2", "HT3"
     */
    private String extractTierNumber(String tierStr) {
        String number = tierStr.replaceAll("[^0-9]", "");
        return number.isEmpty() ? "5" : number; // Default to tier 5 if no number found
    }
    
    /**
     * Check if tier is high tier (HT) or low tier (LT)
     */
    private boolean isHighTier(String tierStr) {
        return tierStr.toUpperCase().contains("HT") || tierStr.toUpperCase().contains("H");
    }
    
    /**
     * Get tier for a specific gamemode
     */
    public String getTier(String gameMode) {
        return tierMap.getOrDefault(gameMode, "Unranked");
    }
    
    /**
     * Get player UUID
     */
    public String getPlayerUuid() {
        return playerUuid;
    }
    
    /**
     * Get player username
     */
    public String getPlayerUsername() {
        return playerUsername;
    }
    
    /**
     * Parse JSON (for compatibility with existing code)
     */
    @Override
    public void parseJson(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            parseApiResponse(new WebsiteApiClient.PlayerTierData(
                obj.has("username") ? obj.get("username").getAsString() : "Unknown",
                obj.has("uuid") ? obj.get("uuid").getAsString() : "Unknown",
                obj.has("sword") ? obj.get("sword").getAsString() : "Unranked",
                obj.has("axe") ? obj.get("axe").getAsString() : "Unranked",
                obj.has("mace") ? obj.get("mace").getAsString() : "Unranked",
                obj.has("uhc") ? obj.get("uhc").getAsString() : "Unranked"
            ));
        } catch (Exception e) {
            LOGGER.error("Error parsing JSON", e);
            status = Status.ERROR;
        }
    }
}
