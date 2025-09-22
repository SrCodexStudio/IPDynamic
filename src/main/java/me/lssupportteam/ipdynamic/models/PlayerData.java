package me.lssupportteam.ipdynamic.models;

import java.util.*;

public class PlayerData {

    private UUID uuid;
    private String username;
    private String lastIp;
    private long firstLogin;
    private long lastLogin;
    private List<String> ipHistory;
    private Map<String, Long> ipTimestamps; // IP -> timestamp de primera vez vista
    private GeoLocation geoLocation;
    private int totalConnections;
    private boolean isAdmin;
    private List<String> linkedAccounts; // UUIDs de cuentas vinculadas (alts)

    public PlayerData(UUID uuid, String username, String ip, long timestamp) {
        this.uuid = uuid;
        this.username = username;
        this.lastIp = ip;
        this.firstLogin = timestamp;
        this.lastLogin = timestamp;
        this.ipHistory = new ArrayList<>();
        this.ipTimestamps = new HashMap<>();
        this.linkedAccounts = new ArrayList<>();
        this.totalConnections = 1;
        this.isAdmin = false;

        if (ip != null && !ip.equals("UNKNOWN")) {
            this.ipHistory.add(ip);
            this.ipTimestamps.put(ip, timestamp);
        }
    }


    public PlayerData() {
        this.ipHistory = new ArrayList<>();
        this.ipTimestamps = new HashMap<>();
        this.linkedAccounts = new ArrayList<>();
    }

    public void addIpToHistory(String ip, int maxHistory) {
        if (ip == null || ip.equals("UNKNOWN")) return;

        if (!ipHistory.contains(ip)) {
            ipHistory.add(ip);
            ipTimestamps.putIfAbsent(ip, System.currentTimeMillis());


            if (maxHistory > 0 && ipHistory.size() > maxHistory) {
                String oldestIp = ipHistory.remove(0);
                ipTimestamps.remove(oldestIp);
            }
        }
    }

    public void incrementConnections() {
        this.totalConnections++;
    }

    public void addLinkedAccount(String uuid) {
        if (!linkedAccounts.contains(uuid)) {
            linkedAccounts.add(uuid);
        }
    }

    public void removeLinkedAccount(String uuid) {
        linkedAccounts.remove(uuid);
    }

    public boolean hasMultipleIps() {
        return ipHistory.size() > 1;
    }

    public long getIpFirstSeen(String ip) {
        return ipTimestamps.getOrDefault(ip, 0L);
    }


    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getLastIp() { return lastIp; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }

    public long getFirstLogin() { return firstLogin; }
    public void setFirstLogin(long firstLogin) { this.firstLogin = firstLogin; }

    public long getLastLogin() { return lastLogin; }
    public void setLastLogin(long lastLogin) { this.lastLogin = lastLogin; }

    public List<String> getIpHistory() { return ipHistory; }
    public void setIpHistory(List<String> ipHistory) { this.ipHistory = ipHistory; }

    public Map<String, Long> getIpTimestamps() { return ipTimestamps; }
    public void setIpTimestamps(Map<String, Long> ipTimestamps) { this.ipTimestamps = ipTimestamps; }

    public GeoLocation getGeoLocation() { return geoLocation; }
    public void setGeoLocation(GeoLocation geoLocation) { this.geoLocation = geoLocation; }

    public int getTotalConnections() { return totalConnections; }
    public void setTotalConnections(int totalConnections) { this.totalConnections = totalConnections; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public List<String> getLinkedAccounts() { return linkedAccounts; }
    public void setLinkedAccounts(List<String> linkedAccounts) { this.linkedAccounts = linkedAccounts; }
}