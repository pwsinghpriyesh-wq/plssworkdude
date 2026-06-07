package com.tiers.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.tiers.config.TiersConfig;
import com.tiers.utils.CrackedPlayerHandler;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerDataFetcher {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static String API_URL;
    
    public static void initialize() {
        API_URL = TiersConfig.getPortalApiUrl() + "/player";
    }
    
    public static CompletableFuture<PlayerTierData> fetchPlayerData(String playerName, UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean isCracked = CrackedPlayerHandler.isCrackedPlayer(uuid);
                if (isCracked && !TiersConfig.supportsCrackedPlayers()) {
                    return new PlayerTierData(playerName, null, "Cracked players not supported");
                }
                
                String url = API_URL + "?name=" + playerName + "&uuid=" + uuid + "&cracked=" + isCracked;
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Tiers-Mod/1.0");
                
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    return new PlayerTierData(playerName, JsonParser.parseString(response.toString()), null);
                } else if (conn.getResponseCode() == 404) {
                    return new PlayerTierData(playerName, null, "Player not found");
                } else {
                    return new PlayerTierData(playerName, null, "API Error: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                return new PlayerTierData(playerName, null, e.getMessage());
            }
        }, EXECUTOR);
    }
    
    public static CompletableFuture<PlayerTierData> fetchPlayerDataByName(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = API_URL + "?name=" + playerName + "&crackedLookup=true";
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Tiers-Mod/1.0");
                
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    return new PlayerTierData(playerName, JsonParser.parseString(response.toString()), null);
                }
                return new PlayerTierData(playerName, null, "Player lookup failed");
            } catch (Exception e) {
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
        public boolean isValid() { return error == null && tierData != null; }
    }
}
