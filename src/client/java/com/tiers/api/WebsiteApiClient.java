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
    
    private String apiUrl = "https://portal-tiers.netlify.app/api";
    private static final Map<String, CachedProfile> CACHE = new HashMap<>();
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
    
    public WebsiteApiClient() {}
    
    public WebsiteApiClient(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    /**
     * Fetch player tiers by UUID (for premium players)
     */
    public CompletableFuture<PlayerTierData> fetchPlayerTiers(String uuid) {
        // Check cache first
        if (CACHE.containsKey(uuid)) {
            CachedProfile cached = CACHE.get(uuid);
            if (System.currentTimeMillis() - cached.timestamp < CACHE_DURATION) {
                LOGGER.info("Using cached data for UUID: {}", uuid);
                return CompletableFuture.completedFuture(cached.data);
            } else {
                CACHE.remove(uuid);
            }
        }
        
        return fetchFromApi("uuid/" + uuid).thenApply(data -> {
            if (data != null) {
                CACHE.put(uuid, new CachedProfile(data, System.currentTimeMillis()));
                LOGGER.info("Cached player data for UUID: {}", uuid);
            }
            return data;
        });
    }
    
    /**
     * Fetch player tiers by username (for cracked players)
     */
    public CompletableFuture<PlayerTierData> fetchPlayerTiersByUsername(String username) {
        // Check cache first (use lowercase for consistency)
        String cacheKey = "username:" + username.toLowerCase();
        
        if (CACHE.containsKey(cacheKey)) {
            CachedProfile cached = CACHE.get(cacheKey);
            if (System.currentTimeMillis() - cached.timestamp < CACHE_DURATION) {
                LOGGER.info("Using cached data for username: {}", username);
                return CompletableFuture.completedFuture(cached.data);
            } else {
                CACHE.remove(cacheKey);
            }
        }
        
        return fetchFromApi("username/" + username).thenApply(data -> {
            if (data != null) {
                CACHE.put(cacheKey, new CachedProfile(data, System.currentTimeMillis()));
                LOGGER.info("Cached player data for username: {}", username);
            }
            return data;
        });
    }
    
    /**
     * Internal method to fetch from API
     */
    private CompletableFuture<PlayerTierData> fetchFromApi(String endpoint) {
        String url = apiUrl + "/player/" + endpoint;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Tiers-Mod")
                .GET()
                .build();
        
        LOGGER.info("Fetching player data from: {}", url);
        
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        if (response.statusCode() == 200) {
                            return parseResponse(response.body());
                        } else {
                            LOGGER.warn("API returned status {}: {}", response.statusCode(), response.body());
                            return null;
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error parsing API response", e);
                        return null;
                    }
                })
                .exceptionally(e -> {
                    LOGGER.error("Error fetching player tiers from API", e);
                    return null;
                });
    }
    
    /**
     * Parse JSON response from API
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
            
            LOGGER.info("Successfully parsed tier data for: {}", username);
            return new PlayerTierData(username, uuid, sword, axe, mace, uhc);
        } catch (Exception e) {
            LOGGER.error("Failed to parse tier data", e);
            return null;
        }
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache() {
        CACHE.clear();
        LOGGER.info("Cleared player cache");
    }
    
    /**
     * Update API URL
     */
    public void setApiUrl(String newUrl) {
        this.apiUrl = newUrl;
        LOGGER.info("API URL updated to: {}", newUrl);
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
