package me.lssupportteam.ipdynamic.managers;

import com.google.gson.reflect.TypeToken;
import me.lssupportteam.ipdynamic.IPDynamic;
import me.lssupportteam.ipdynamic.models.GeoLocation;
import me.lssupportteam.ipdynamic.models.PlayerData;
import me.lssupportteam.ipdynamic.utils.IPUtils;
import me.lssupportteam.ipdynamic.utils.JsonUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DataManager {

    private final IPDynamic plugin;


    private final File playerConnectionsFile;
    private final File playerRegisterFile;
    private final File playerDisconnectFile;
    private final File adminLoginFile;


    private final ConcurrentHashMap<UUID, PlayerData> playerDataMap;
    private final ConcurrentHashMap<String, Set<UUID>> ipToPlayersMap; // IP -> Set de UUIDs
    private final ConcurrentHashMap<UUID, ConnectionInfo> activeConnections;
    private final List<ConnectionLog> connectionHistory;
    private final List<AdminLoginLog> adminLoginHistory;


    private final ConcurrentHashMap<String, List<UUID>> altAccountsCache;

    public DataManager(IPDynamic plugin) {
        this.plugin = plugin;


        File dataDir = new File(plugin.getPluginDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        this.playerConnectionsFile = new File(dataDir, "playerConnections.json");
        this.playerRegisterFile = new File(dataDir, "player-register.json");
        this.playerDisconnectFile = new File(dataDir, "player-disconnect.json");
        this.adminLoginFile = new File(dataDir, "admin-login.json");


        this.playerDataMap = new ConcurrentHashMap<>();
        this.ipToPlayersMap = new ConcurrentHashMap<>();
        this.activeConnections = new ConcurrentHashMap<>();
        this.connectionHistory = Collections.synchronizedList(new ArrayList<>());
        this.adminLoginHistory = Collections.synchronizedList(new ArrayList<>());
        this.altAccountsCache = new ConcurrentHashMap<>();
    }

    public void loadAllData() {
        loadPlayerData();
        loadConnectionHistory();
        loadAdminLogins();
        rebuildCaches();

        plugin.getLogger().info(plugin.getLangManager().getMessage("data.loaded")
            .replace("{players}", String.valueOf(playerDataMap.size()))
            .replace("{ips}", String.valueOf(ipToPlayersMap.size()))
            .replace("{connections}", String.valueOf(connectionHistory.size())));
    }

    private void loadPlayerData() {
        Type type = JsonUtils.getTypeTokenMap(UUID.class, PlayerData.class);
        Map<UUID, PlayerData> loaded = JsonUtils.loadData(playerConnectionsFile, type, plugin.getLogger());

        if (loaded != null) {
            loaded.forEach((uuid, data) -> {

                if (data.getUuid() == null) data.setUuid(uuid);
                if (data.getIpHistory() == null) data.setIpHistory(new ArrayList<>());
                if (data.getIpTimestamps() == null) data.setIpTimestamps(new HashMap<>());
                if (data.getLinkedAccounts() == null) data.setLinkedAccounts(new ArrayList<>());

                playerDataMap.put(uuid, data);


                for (String ip : data.getIpHistory()) {
                    ipToPlayersMap.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(uuid);
                }
            });
        }
    }

    private void loadConnectionHistory() {
        Type type = new TypeToken<List<ConnectionLog>>(){}.getType();
        List<ConnectionLog> loaded = JsonUtils.loadData(playerRegisterFile, type, plugin.getLogger());

        if (loaded != null) {
            connectionHistory.addAll(loaded);
        }
    }

    private void loadAdminLogins() {
        Type type = new TypeToken<List<AdminLoginLog>>(){}.getType();
        List<AdminLoginLog> loaded = JsonUtils.loadData(adminLoginFile, type, plugin.getLogger());

        if (loaded != null) {
            adminLoginHistory.addAll(loaded);
        }
    }

    public void saveAllData() {
        savePlayerData();
        saveConnectionHistory();
        saveAdminLogins();
    }

    private void savePlayerData() {
        Map<UUID, PlayerData> dataToSave = new HashMap<>(playerDataMap);
        JsonUtils.saveData(playerConnectionsFile, dataToSave, plugin.getLogger());
    }

    private void saveConnectionHistory() {

        List<ConnectionLog> toSave = connectionHistory;
        if (connectionHistory.size() > 10000) {
            toSave = connectionHistory.subList(connectionHistory.size() - 10000, connectionHistory.size());
        }
        JsonUtils.saveData(playerRegisterFile, toSave, plugin.getLogger());


        List<ConnectionLog> disconnections = connectionHistory.stream()
            .filter(log -> log.type == ConnectionLog.Type.DISCONNECT)
            .collect(Collectors.toList());

        if (disconnections.size() > 5000) {
            disconnections = disconnections.subList(disconnections.size() - 5000, disconnections.size());
        }
        JsonUtils.saveData(playerDisconnectFile, disconnections, plugin.getLogger());
    }

    private void saveAdminLogins() {
        List<AdminLoginLog> toSave = adminLoginHistory;
        if (adminLoginHistory.size() > 5000) {
            toSave = adminLoginHistory.subList(adminLoginHistory.size() - 5000, adminLoginHistory.size());
        }
        JsonUtils.saveData(adminLoginFile, toSave, plugin.getLogger());
    }


    public PlayerData registerConnection(Player player) {
        UUID uuid = player.getUniqueId();
        String username = player.getName();
        String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "UNKNOWN";
        long timestamp = System.currentTimeMillis();


        PlayerData data = playerDataMap.computeIfAbsent(uuid, k -> {
            plugin.getLogger().info(String.format(
                "Primera conexión detectada: %s (UUID: %s) desde IP: %s",
                username, uuid, ip
            ));

            PlayerData newData = new PlayerData(uuid, username, ip, timestamp);


            if (plugin.getConfigManager().isGeoIpOnFirstLogin() && plugin.getGeoIPService() != null) {
                fetchGeoLocation(newData, ip);
            }

            return newData;
        });


        data.setUsername(username);
        data.setLastLogin(timestamp);
        data.incrementConnections();


        if (!ip.equals(data.getLastIp()) && !ip.equals("UNKNOWN")) {
            data.setLastIp(ip);
            data.addIpToHistory(ip, plugin.getConfigManager().getMaxIpHistory());


            ipToPlayersMap.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(uuid);


            if (plugin.getGeoIPService() != null) {
                fetchGeoLocation(data, ip);
            }
        }


        if (player.hasPermission("ipdynamic.admin") || player.isOp()) {
            data.setAdmin(true);
            adminLoginHistory.add(new AdminLoginLog(uuid, username, ip, timestamp));
        }


        activeConnections.put(uuid, new ConnectionInfo(uuid, username, ip, timestamp));


        connectionHistory.add(new ConnectionLog(
            ConnectionLog.Type.CONNECT,
            uuid,
            username,
            ip,
            timestamp
        ));


        detectAndCacheAlts(uuid, ip);

        return data;
    }


    public void registerDisconnection(Player player) {
        UUID uuid = player.getUniqueId();
        long timestamp = System.currentTimeMillis();

        ConnectionInfo connection = activeConnections.remove(uuid);
        if (connection != null) {

            long sessionDuration = timestamp - connection.loginTime;


            connectionHistory.add(new ConnectionLog(
                ConnectionLog.Type.DISCONNECT,
                uuid,
                player.getName(),
                connection.ip,
                timestamp,
                sessionDuration
            ));
        }
    }


    private void detectAndCacheAlts(UUID playerUuid, String ip) {
        Set<UUID> playersWithSameIp = ipToPlayersMap.get(ip);
        if (playersWithSameIp == null || playersWithSameIp.size() <= 1) {
            return;
        }

        List<UUID> alts = playersWithSameIp.stream()
            .filter(uuid -> !uuid.equals(playerUuid))
            .collect(Collectors.toList());

        if (!alts.isEmpty()) {
            // Update cache for current player
            altAccountsCache.put(playerUuid.toString(), alts);

            // Update bidirectional linking
            PlayerData playerData = playerDataMap.get(playerUuid);
            if (playerData != null) {
                for (UUID altUuid : alts) {
                    playerData.addLinkedAccount(altUuid.toString());

                    // Update the alt account's data
                    PlayerData altData = playerDataMap.get(altUuid);
                    if (altData != null) {
                        altData.addLinkedAccount(playerUuid.toString());

                        // IMPORTANT: Update the alt's cache to include this new connection
                        List<UUID> existingAltsForAlt = altAccountsCache.getOrDefault(altUuid.toString(), new ArrayList<>());
                        if (!existingAltsForAlt.contains(playerUuid)) {
                            existingAltsForAlt.add(playerUuid);

                            // Add all other alts that the current player has
                            for (UUID otherAlt : alts) {
                                if (!otherAlt.equals(altUuid) && !existingAltsForAlt.contains(otherAlt)) {
                                    existingAltsForAlt.add(otherAlt);
                                }
                            }
                            altAccountsCache.put(altUuid.toString(), existingAltsForAlt);
                        }
                    }
                }
            }
        }
    }


    public List<PlayerData> findAlts(UUID playerUuid) {
        PlayerData playerData = playerDataMap.get(playerUuid);
        if (playerData == null) return Collections.emptyList();

        Set<UUID> alts = new HashSet<>();


        for (String ip : playerData.getIpHistory()) {
            Set<UUID> playersWithIp = ipToPlayersMap.get(ip);
            if (playersWithIp != null) {
                alts.addAll(playersWithIp);
            }
        }


        alts.remove(playerUuid);


        return alts.stream()
            .map(playerDataMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<PlayerData> findAltsByIp(String ip) {
        if (!IPUtils.isValidIpAddress(ip)) return Collections.emptyList();

        Set<UUID> players = ipToPlayersMap.get(ip);
        if (players == null) return Collections.emptyList();

        return players.stream()
            .map(playerDataMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }


    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public PlayerData getPlayerData(String username) {
        return playerDataMap.values().stream()
            .filter(data -> data.getUsername().equalsIgnoreCase(username))
            .findFirst()
            .orElse(null);
    }

    public PlayerData getPlayerData(OfflinePlayer player) {
        return player != null ? getPlayerData(player.getUniqueId()) : null;
    }

    /**
     * Gets all usernames linked to a specific IP address
     */
    public List<String> getUsernamesLinkedToIP(String ip) {
        if (!IPUtils.isValidIpAddress(ip)) return Collections.emptyList();

        Set<UUID> playersWithIp = ipToPlayersMap.get(ip);
        if (playersWithIp == null || playersWithIp.isEmpty()) {
            return Collections.emptyList();
        }

        return playersWithIp.stream()
            .map(playerDataMap::get)
            .filter(Objects::nonNull)
            .map(PlayerData::getUsername)
            .collect(Collectors.toList());
    }


    private void fetchGeoLocation(PlayerData data, String ip) {
        if (plugin.getGeoIPService() == null || IPUtils.isLocalIp(ip)) {
            return;
        }

        plugin.getGeoIPService().getLocation(ip).thenAccept(geoLocation -> {
            if (geoLocation != null) {
                data.setGeoLocation(geoLocation);
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.WARNING, "Error obteniendo geolocalización para " + ip, throwable);
            return null;
        });
    }

    private void rebuildCaches() {

        altAccountsCache.clear();


        for (UUID uuid : playerDataMap.keySet()) {
            PlayerData data = playerDataMap.get(uuid);
            if (data.hasMultipleIps()) {
                for (String ip : data.getIpHistory()) {
                    detectAndCacheAlts(uuid, ip);
                }
            }
        }
    }


    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlayers", playerDataMap.size());
        stats.put("uniqueIps", ipToPlayersMap.size());
        stats.put("activeConnections", activeConnections.size());
        stats.put("connectionHistory", connectionHistory.size());
        stats.put("adminLogins", adminLoginHistory.size());
        stats.put("detectedAlts", altAccountsCache.size());


        Map.Entry<String, Set<UUID>> mostUsedIp = ipToPlayersMap.entrySet().stream()
            .max(Comparator.comparingInt(e -> e.getValue().size()))
            .orElse(null);

        if (mostUsedIp != null) {
            stats.put("mostUsedIp", mostUsedIp.getKey());
            stats.put("mostUsedIpAccounts", mostUsedIp.getValue().size());
        }

        return stats;
    }

    public Collection<PlayerData> getAllPlayers() {
        return new ArrayList<>(playerDataMap.values());
    }

    /**
     * Gets connection count for a specific player and IP combination
     */
    public int getConnectionCountForIP(UUID playerUuid, String ip) {
        return (int) connectionHistory.stream()
                .filter(log -> log.type == ConnectionLog.Type.CONNECT)
                .filter(log -> log.uuid.equals(playerUuid))
                .filter(log -> log.ip.equals(ip))
                .count();
    }

    /**
     * Gets connection history statistics by IP for a specific player
     */
    public Map<String, Integer> getConnectionStatsByIP(UUID playerUuid) {
        Map<String, Integer> ipConnectionCount = new HashMap<>();

        connectionHistory.stream()
                .filter(log -> log.type == ConnectionLog.Type.CONNECT)
                .filter(log -> log.uuid.equals(playerUuid))
                .forEach(log -> ipConnectionCount.merge(log.ip, 1, Integer::sum));

        return ipConnectionCount;
    }


    private static class ConnectionInfo {
        final UUID uuid;
        final String username;
        final String ip;
        final long loginTime;

        ConnectionInfo(UUID uuid, String username, String ip, long loginTime) {
            this.uuid = uuid;
            this.username = username;
            this.ip = ip;
            this.loginTime = loginTime;
        }
    }

    public static class ConnectionLog {
        enum Type { CONNECT, DISCONNECT }

        final Type type;
        final UUID uuid;
        final String username;
        final String ip;
        final long timestamp;
        final long sessionDuration; // Solo para desconexiones

        ConnectionLog(Type type, UUID uuid, String username, String ip, long timestamp) {
            this(type, uuid, username, ip, timestamp, 0);
        }

        ConnectionLog(Type type, UUID uuid, String username, String ip, long timestamp, long sessionDuration) {
            this.type = type;
            this.uuid = uuid;
            this.username = username;
            this.ip = ip;
            this.timestamp = timestamp;
            this.sessionDuration = sessionDuration;
        }
    }

    public static class AdminLoginLog {
        final UUID uuid;
        final String username;
        final String ip;
        final long timestamp;

        AdminLoginLog(UUID uuid, String username, String ip, long timestamp) {
            this.uuid = uuid;
            this.username = username;
            this.ip = ip;
            this.timestamp = timestamp;
        }
    }
}