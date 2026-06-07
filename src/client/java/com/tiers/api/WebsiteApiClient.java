package com.tiers.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WebsiteApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebsiteApiClient.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    
    // CRITICAL FIX: Use backend API, NOT the Netlify frontend page
    // The frontend URL (portal-tiers.netlify.app) returns HTML, not JSON
    // This MUST be set by user to point to actual backend serving JSON
    private String apiUrl = "https://api.portal-tiers.example.com";
    private static final Map<String, CachedProfile> CACHE = new HashMap<>();
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
    
    public WebsiteApiClient() {
        LOGGER.warn("[Tiers] WebsiteApiClient initialized with default URL: {}", apiUrl);
    }
    
    public WebsiteApiClient(String apiUrl) {
        this.apiUrl = apiUrl;
        LOGGER.info("[Tiers] WebsiteApiClient initialized with URL: {}", apiUrl);
    }
    
    /**
     * Fetch player tiers by UUID (for premium players)
     * MUST call backend API, never HTML page
     */
    public CompletableFuture<PlayerTierData> fetchPlayerTiers(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            LOGGER.error("[Tiers] UUID is null or empty");
            return CompletableFuture.completedFuture(null);
        }
        
        // Check cache first
        if (CACHE.containsKey(uuid)) {
            CachedProfile cached = CACHE.get(uuid);
            if (System.currentTimeMillis() - cached.timestamp < CACHE_DURATION) {
                LOGGER.info("[Tiers] Using cached data for UUID: {}", uuid);
                return CompletableFuture.completedFuture(cached.data);
            } else {
                LOGGER.info("[Tiers] Cache expired for UUID: {}, refreshing...", uuid);
                CACHE.remove(uuid);
            }
        }
        
        return fetchFromApi("uuid/" + uuid).thenApply(data -> {
            if (data != null) {
                CACHE.put(uuid, new CachedProfile(data, System.currentTimeMillis()));
                LOGGER.info("[Tiers] Cached player data for UUID: {}", uuid);
            }
            return data;
        });
    }
    
    /**
     * Fetch player tiers by username (for cracked players)
     */
    public CompletableFuture<PlayerTierData> fetchPlayerTiersByUsername(String username) {
        if (username == null || username.isEmpty()) {
            LOGGER.error("[Tiers] Username is null or empty");
            return CompletableFuture.completedFuture(null);
        }
        
        // Check cache first (use lowercase for consistency)
        String cacheKey = "username:" + username.toLowerCase();
        
        if (CACHE.containsKey(cacheKey)) {
            CachedProfile cached = CACHE.get(cacheKey);
            if (System.currentTimeMillis() - cached.timestamp < CACHE_DURATION) {
                LOGGER.info("[Tiers] Using cached data for username: {}", username);
                return CompletableFuture.completedFuture(cached.data);
            } else {
                LOGGER.info("[Tiers] Cache expired for username: {}, refreshing...", username);
                CACHE.remove(cacheKey);
            }
        }
        
        return fetchFromApi("username/" + username).thenApply(data -> {
            if (data != null) {
                CACHE.put(cacheKey, new CachedProfile(data, System.currentTimeMillis()));
                LOGGER.info("[Tiers] Cached player data for username: {}", username);
            }
            return data;
        });
    }
    
    /**
     * Internal method to fetch from API
     * CRITICAL: Proper error logging before and after request
     */
    private CompletableFuture<PlayerTierData> fetchFromApi(String endpoint) {
        String url = apiUrl + "/player/" + endpoint;
        
        LOGGER.info("[Tiers] ========== TIER REQUEST START ==========");
        LOGGER.info("[Tiers] Fetching player data from URL: {}", url);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Tiers-Mod/1.0")
                .timeout(java.time.Duration.ofSeconds(10))
                .GET()
                .build();
        
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    LOGGER.info("[Tiers] HTTP Response Code: {}", response.statusCode());
                    
                    try {
                        if (response.statusCode() == 200) {
                            String body = response.body();
                            LOGGER.info("[Tiers] Response body length: {} chars", body.length());
                            LOGGER.debug("[Tiers] Response body (first 500 chars): {}", 
                                body.length() > 500 ? body.substring(0, 500) : body);
                            
                            PlayerTierData parsed = parseResponse(body);
                            if (parsed != null) {
                                LOGGER.info("[Tiers] Successfully parsed tier data");
                            } else {
                                LOGGER.error("[Tiers] Failed to parse response - null result");
                            }
                            return parsed;
                        } else {
                            LOGGER.warn("[Tiers] API returned non-200 status: {}", response.statusCode());
                            LOGGER.warn("[Tiers] Response body: {}", response.body());
                            return null;
                        }
                    } catch (Exception e) {
                        LOGGER.error("[Tiers] Error parsing API response", e);
                        return null;
                    } finally {
                        LOGGER.info("[Tiers] ========== TIER REQUEST END ==========");
                    }
                })
                .exceptionally(e -> {
                    LOGGER.error("[Tiers] Network error fetching player tiers from API: {}", e.getMessage(), e);
                    LOGGER.info("[Tiers] ========== TIER REQUEST END (ERROR) ==========");
                    return null;
                });
    }
    
    /**
     * Parse JSON response from API
     * Expects format: {"username":"name","uuid":"uuid","sword":"HT1","axe":"LT2","mace":"Unranked","uhc":"HT3"}
     */
    private PlayerTierData parseResponse(String jsonBody) {
        try {
            JsonObject json = JsonParser.parseString(jsonBody).getAsJsonObject();
            
            String username = json.has("username") ? json.get("username").getAsString() : "Unknown";
            String uuid = json.has("uuid") ? json.get("uuid").getAsString() : "Unknown";
            String sword = json.has("sword") ? json.get("sword").getAsString() : "Unranked";
            String axe = json.has("axe") ? json.get("axe").getAsString() : "Unranked";
            String mace = json.has("mace") ? json.get("mace").getAsString() : "Unranked";
            String uhc = json.has("uhc") ? json.get("uhc").getAsString() : "Unranked";
            
            LOGGER.info("[Tiers] Parsed tier data - Player: {}, Sword: {}, Axe: {}, Mace: {}, UHC: {}", 
                username, sword, axe, mace, uhc);
            
            return new PlayerTierData(username, uuid, sword, axe, mace, uhc);
        } catch (Exception e) {
            LOGGER.error("[Tiers] Failed to parse tier data from JSON: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache() {
        CACHE.clear();
        LOGGER.info("[Tiers] Cleared player cache");
    }
    
    /**
     * Update API URL at runtime
     */
    public void setApiUrl(String newUrl) {
        this.apiUrl = newUrl;
        LOGGER.warn("[Tiers] API URL updated to: {}", newUrl);
    }
    
    /**
     * Inner class for tier data
     */
    public static class PlayerTierData {
        public String username;
        public String uuid;
        public String sword;
        public String axe;
        public String mace;
        public String uhc;
        
        public PlayerTierData(String username, String uuid, String sword, String axe, String mace, String uhc) {
            this.username = username;
            this.uuid = uuid;
            this.sword = sword;
            this.axe = axe;
            this.mace = mace;
            this.uhc = uhc;
        }
        
        @Override
        public String toString() {
            return "PlayerTierData{" +
                    "username='" + username + '\'' +
                    ", uuid='" + uuid + '\'' +
                    ", sword='" + sword + '\'' +
                    ", axe='" + axe + '\'' +
                    ", mace='" + mace + '\'' +
                    ", uhc='" + uhc + '\'' +
                    '}';
        }
    }
    
    /**
     * Inner class for cached profiles
     */
    private static class CachedProfile {
        PlayerTierData data;
        long timestamp;
        
        CachedProfile(PlayerTierData data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }
}
