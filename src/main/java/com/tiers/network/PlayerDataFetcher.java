package com.tiers.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.tiers.config.TiersConfig;
import com.tiers.utils.CrackedPlayerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerDataFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataFetcher.class);
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static String API_URL;
    
    public static void initialize() {
        API_URL = TiersConfig.getPortalApiUrl() + "/player";
        LOGGER.info("[Tiers] PlayerDataFetcher initialized with API URL: {}", API_URL);
    }
    
    /**
     * Fetch player data by UUID and name
     * CRITICAL: Proper error logging at every step
     */
    public static CompletableFuture<PlayerTierData> fetchPlayerData(String playerName, UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean isCracked = CrackedPlayerHandler.isCrackedPlayer(uuid);
                if (isCracked && !TiersConfig.supportsCrackedPlayers()) {
                    LOGGER.info("[Tiers] Cracked player not supported: {}", playerName);
                    return new PlayerTierData(playerName, null, "Cracked players not supported");
                }
                
                String url = API_URL + "?name=" + playerName + "&uuid=" + uuid + "&cracked=" + isCracked;
                LOGGER.info("[Tiers] Fetching player data: URL={}", url);
                
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Tiers-Mod/1.0");
                
                int responseCode = conn.getResponseCode();
                LOGGER.info("[Tiers] API Response Code: {}", responseCode);
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    
                    String body = response.toString();
                    LOGGER.info("[Tiers] Response body length: {} chars", body.length());
                    LOGGER.debug("[Tiers] Response (first 200 chars): {}", 
                        body.length() > 200 ? body.substring(0, 200) : body);
                    
                    return new PlayerTierData(playerName, JsonParser.parseString(body), null);
                } else if (responseCode == 404) {
                    LOGGER.warn("[Tiers] Player not found (404): {}", playerName);
                    return new PlayerTierData(playerName, null, "Player not found");
                } else {
                    LOGGER.error("[Tiers] API Error ({}): {}", responseCode, conn.getResponseMessage());
                    return new PlayerTierData(playerName, null, "API Error: " + responseCode);
                }
            } catch (Exception e) {
                LOGGER.error("[Tiers] Exception fetching player data for {}", playerName, e);
                return new PlayerTierData(playerName, null, e.getMessage());
            }
        }, EXECUTOR);
    }
    
    /**
     * Fetch player data by name only (for cracked players)
     */
    public static CompletableFuture<PlayerTierData> fetchPlayerDataByName(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = API_URL + "?name=" + playerName + "&crackedLookup=true";
                LOGGER.info("[Tiers] Fetching cracked player by name: URL={}", url);
                
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Tiers-Mod/1.0");
                
                int responseCode = conn.getResponseCode();
                LOGGER.info("[Tiers] API Response Code: {}", responseCode);
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    
                    String body = response.toString();
                    LOGGER.info("[Tiers] Cracked player lookup successful for: {}", playerName);
                    LOGGER.debug("[Tiers] Response (first 200 chars): {}", 
                        body.length() > 200 ? body.substring(0, 200) : body);
                    
                    return new PlayerTierData(playerName, JsonParser.parseString(body), null);
                } else {
                    LOGGER.warn("[Tiers] Cracked player lookup failed ({}): {}", responseCode, playerName);
                    return new PlayerTierData(playerName, null, "Player lookup failed");
                }
            } catch (Exception e) {
                LOGGER.error("[Tiers] Exception fetching cracked player by name: {}", playerName, e);
                return new PlayerTierData(playerName, null, e.getMessage());
            }
        }, EXECUTOR);
    }
    
    public static class PlayerTierData {
        public String playerName;
        public JsonElement tierData;
        public String error;
        
        public PlayerTierData(String playerName, JsonElement tierData, String error) {
            this.playerName = playerName;
            this.tierData = tierData;
            this.error = error;
        }
        
        public boolean isValid() { 
            return error == null && tierData != null; 
        }
    }
}
