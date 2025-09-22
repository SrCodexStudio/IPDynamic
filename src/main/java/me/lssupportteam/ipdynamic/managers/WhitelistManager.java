package me.lssupportteam.ipdynamic.managers;

import com.google.gson.reflect.TypeToken;
import me.lssupportteam.ipdynamic.IPDynamic;
import me.lssupportteam.ipdynamic.utils.JsonUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WhitelistManager {

    private final IPDynamic plugin;
    private final File whitelistFile;
    private final Set<String> whitelistedPlayers;

    public WhitelistManager(IPDynamic plugin) {
        this.plugin = plugin;
        this.whitelistFile = new File(plugin.getPluginDataFolder(), "data/whitelist.json");
        this.whitelistedPlayers = ConcurrentHashMap.newKeySet();

        ensureDataDirectory();
    }

    private void ensureDataDirectory() {
        File dataDir = new File(plugin.getPluginDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    public void loadWhitelist() {
        Type type = new TypeToken<List<String>>(){}.getType();
        List<String> loaded = JsonUtils.loadData(whitelistFile, type, plugin.getLogger());

        if (loaded != null) {
            whitelistedPlayers.clear();
            whitelistedPlayers.addAll(loaded);
            plugin.getLogger().info(plugin.getLangManager().getMessage("whitelist.loaded").replace("{count}", String.valueOf(whitelistedPlayers.size())));
        } else {
            plugin.getLogger().info(plugin.getLangManager().getMessage("whitelist.empty"));
        }
    }

    public void saveWhitelist() {
        List<String> dataToSave = new ArrayList<>(whitelistedPlayers);
        JsonUtils.saveData(whitelistFile, dataToSave, plugin.getLogger());
    }

    public boolean addPlayer(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }

        boolean added = whitelistedPlayers.add(playerName.toLowerCase());
        if (added) {
            // Save to JSON file immediately
            saveWhitelist();
            plugin.getLogger().info(plugin.getLangManager().getMessage("log.player-added-whitelist").replace("{player}", playerName).replace("{admin}", "System"));
        }
        return added;
    }

    public boolean removePlayer(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }

        boolean removed = whitelistedPlayers.remove(playerName.toLowerCase());
        if (removed) {
            // Save to JSON file immediately
            saveWhitelist();
            plugin.getLogger().info(plugin.getLangManager().getMessage("log.player-removed-whitelist").replace("{player}", playerName).replace("{admin}", "System"));
        }
        return removed;
    }

    public boolean isWhitelisted(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }
        return whitelistedPlayers.contains(playerName.toLowerCase());
    }

    public Set<String> getAllWhitelistedPlayers() {
        return new HashSet<>(whitelistedPlayers);
    }

    public int getWhitelistSize() {
        return whitelistedPlayers.size();
    }

    public void clearWhitelist() {
        whitelistedPlayers.clear();
        plugin.getLogger().info(plugin.getLangManager().getMessage("whitelist.cleared"));
    }
}