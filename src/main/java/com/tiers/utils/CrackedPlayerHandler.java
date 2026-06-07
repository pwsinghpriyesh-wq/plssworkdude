package com.tiers.utils;

import java.util.UUID;
import java.security.MessageDigest;

public class CrackedPlayerHandler {
    private static final String OFFLINE_UUID_NAMESPACE = "OfflinePlayer:";
    
    public static void initialize() {
        System.out.println("[Tiers] Cracked player handler initialized");
    }
    
    public static boolean isCrackedPlayer(UUID uuid) {
        return uuid != null && uuid.version() == 3;
    }
    
    public static UUID generateOfflineUUID(String playerName) {
        try {
            String data = OFFLINE_UUID_NAMESPACE + playerName;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(data.getBytes());
            bytes[6] = (byte) (0x30 | (bytes[6] & 0x0f));
            bytes[8] = (byte) (0x80 | (bytes[8] & 0x3f));
            long msb = 0, lsb = 0;
            for (int i = 0; i < 8; i++) msb = (msb << 8) | (bytes[i] & 0xff);
            for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (bytes[i] & 0xff);
            return new UUID(msb, lsb);
        } catch (Exception e) {
            System.err.println("[Tiers] Error generating offline UUID: " + e.getMessage());
            return null;
        }
    }
    
    public static String getDisplayName(String playerName, UUID uuid, String tier) {
        String indicator = isCrackedPlayer(uuid) ? "[C]" : "";
        return indicator + playerName + (tier != null ? " - " + tier : "");
    }
    
    public static boolean canLookupPlayer(UUID uuid) { return uuid != null; }
    
    public static String getSafeLookupIdentifier(String playerName, UUID uuid) {
        return isCrackedPlayer(uuid) ? "cracked:" + playerName : "premium:" + uuid.toString();
    }
}
