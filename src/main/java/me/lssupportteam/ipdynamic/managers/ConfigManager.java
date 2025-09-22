package me.lssupportteam.ipdynamic.managers;

import me.lssupportteam.ipdynamic.IPDynamic;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.List;

public class ConfigManager {

    private final IPDynamic plugin;
    private final File configFile;
    private YamlConfiguration config;

    public ConfigManager(IPDynamic plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);


        addDefaultValues();

        plugin.getLogger().info(plugin.getLangManager() != null ? plugin.getLangManager().getMessage("config.loaded") : "Configuración cargada correctamente.");
    }

    private void saveDefaultConfig() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            try (InputStream inputStream = plugin.getResource("config.yml")) {
                if (inputStream != null) {
                    Files.copy(inputStream, configFile.toPath());
                } else {

                    createDefaultConfig();
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe((plugin.getLangManager() != null ? plugin.getLangManager().getMessage("config.create-error") : "Error creando archivo de configuración por defecto: {error}").replace("{error}", e.getMessage()));
            createDefaultConfig();
        }
    }

    private void createDefaultConfig() {
        config = new YamlConfiguration();


        config.set("general.language", "spanish");
        config.set("general.debug-mode", false);
        config.set("general.autosave-interval", 10);
        config.set("general.date-format", "dd/MM/yyyy HH:mm:ss");
        config.set("general.timezone", "America/Mexico_City");


        config.set("data.max-ip-history", 50);
        config.set("data.player-data-file", "playerConnections.json");
        config.set("data.single-bans-file", "single-bans.json");
        config.set("data.op1-bans-file", "op1-bans.json");
        config.set("data.op2-bans-file", "op2-bans.json");


        config.set("bans.default-reason", "Comportamiento sospechoso");
        config.set("bans.op1-process-delay", 0); // Inmediato
        config.set("bans.op2-process-delay", 300); // 5 minutos
        config.set("bans.unban-process-delay", 300); // 5 minutos
        config.set("bans.max-ips-per-cycle", 1000);
        config.set("bans.kick-message", "&c&lIPDynamic\n\n&fTu IP ha sido baneada\n&7Razón: &e{reason}\n\n&7Si crees que esto es un error,\n&7contacta con un administrador.");


        config.set("alt-detection.enabled", true);
        config.set("alt-detection.notify-admins", true);
        config.set("alt-detection.min-shared-ips", 1);
        config.set("alt-detection.ignore-local-ips", true);
        config.set("alt-detection.whitelist-immune", true);


        config.set("geoip.enabled", true);
        config.set("geoip.provider", "ip-api.com");
        config.set("geoip.on-first-login", true);
        config.set("geoip.cache-duration", 3600); // 1 hora
        config.set("geoip.alert-on-proxy", true);
        config.set("geoip.alert-on-hosting", true);


        config.set("performance.async-processing", true);
        config.set("performance.thread-pool-size", 4);
        config.set("performance.cache-enabled", true);
        config.set("performance.cache-size", 10000);


        config.set("notifications.in-game.enabled", true);
        config.set("notifications.in-game.alt-detection", true);
        config.set("notifications.in-game.admin-login", true);
        config.set("notifications.in-game.suspicious-activity", true);


        config.set("commands.aliases", List.of("ipd", "ipdynamic"));
        config.set("commands.cooldown", 3);

        try {
            config.save(configFile);
            plugin.getLogger().info("Archivo de configuración creado con valores por defecto.");
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando configuración por defecto: " + e.getMessage());
        }
    }

    private void addDefaultValues() {
        boolean modified = false;


        if (!config.contains("general.timezone")) {
            config.set("general.timezone", "America/Mexico_City");
            modified = true;
        }

        if (!config.contains("performance.thread-pool-size")) {
            config.set("performance.thread-pool-size", 4);
            modified = true;
        }

        if (!config.contains("bans.max-ips-per-cycle")) {
            config.set("bans.max-ips-per-cycle", 1000);
            modified = true;
        }

        if (!config.contains("alt-detection.whitelist-immune")) {
            config.set("alt-detection.whitelist-immune", true);
            modified = true;
        }

        if (modified) {
            try {
                config.save(configFile);
                plugin.getLogger().info("Configuración actualizada con nuevos valores.");
            } catch (IOException e) {
                plugin.getLogger().severe("Error actualizando configuración: " + e.getMessage());
            }
        }
    }


    public String getLangFileName() {
        return config.getString("general.language", "spanish") + ".yml";
    }

    public boolean isDebugMode() {
        return config.getBoolean("general.debug-mode", false);
    }

    public int getAutosaveInterval() {
        return config.getInt("general.autosave-interval", 10);
    }

    public SimpleDateFormat getDateFormat() {
        String pattern = config.getString("general.date-format", "dd/MM/yyyy HH:mm:ss");
        return new SimpleDateFormat(pattern);
    }

    public String getTimezone() {
        return config.getString("general.timezone", "America/Mexico_City");
    }


    public int getMaxIpHistory() {
        return config.getInt("data.max-ip-history", 50);
    }

    public String getPlayerDataFileName() {
        return config.getString("data.player-data-file", "playerConnections.json");
    }

    public String getSingleBansFileName() {
        return config.getString("data.single-bans-file", "single-bans.json");
    }

    public String getOp1BansFileName() {
        return config.getString("data.op1-bans-file", "op1-bans.json");
    }

    public String getOp2BansFileName() {
        return config.getString("data.op2-bans-file", "op2-bans.json");
    }


    public String getDefaultBanReason() {
        return config.getString("bans.default-reason", "Comportamiento sospechoso");
    }

    public int getOp1ProcessDelay() {
        return config.getInt("bans.op1-process-delay", 0);
    }

    public int getOp2ProcessDelay() {
        return config.getInt("bans.op2-process-delay", 300);
    }

    public int getUnbanProcessDelay() {
        return config.getInt("bans.unban-process-delay", 300);
    }

    public int getMaxIpsPerCycle() {
        return config.getInt("bans.max-ips-per-cycle", 1000);
    }

    public String getKickMessage() {
        return config.getString("bans.kick-message",
            "&c&lIPDynamic\n\n&fTu IP ha sido baneada\n&7Razón: &e{reason}\n\n&7Si crees que esto es un error,\n&7contacta con un administrador.");
    }


    public boolean isAltDetectionEnabled() {
        return config.getBoolean("alt-detection.enabled", true);
    }

    public boolean isNotifyAdminsOnAlt() {
        return config.getBoolean("alt-detection.notify-admins", true);
    }

    public int getMinSharedIps() {
        return config.getInt("alt-detection.min-shared-ips", 1);
    }

    public boolean isIgnoreLocalIps() {
        return config.getBoolean("alt-detection.ignore-local-ips", true);
    }

    public boolean isWhitelistImmune() {
        return config.getBoolean("alt-detection.whitelist-immune", true);
    }


    public boolean isGeoIpEnabled() {
        return config.getBoolean("geoip.enabled", true);
    }

    public String getGeoIpProvider() {
        return config.getString("geoip.provider", "ip-api.com");
    }

    public boolean isGeoIpOnFirstLogin() {
        return config.getBoolean("geoip.on-first-login", true);
    }

    public int getGeoIpCacheDuration() {
        return config.getInt("geoip.cache-duration", 3600);
    }

    public boolean isAlertOnProxy() {
        return config.getBoolean("geoip.alert-on-proxy", true);
    }

    public boolean isAlertOnHosting() {
        return config.getBoolean("geoip.alert-on-hosting", true);
    }


    public boolean isAsyncProcessing() {
        return config.getBoolean("performance.async-processing", true);
    }

    public int getThreadPoolSize() {
        return config.getInt("performance.thread-pool-size", 4);
    }

    public boolean isCacheEnabled() {
        return config.getBoolean("performance.cache-enabled", true);
    }

    public int getCacheSize() {
        return config.getInt("performance.cache-size", 10000);
    }


    public boolean isInGameNotificationsEnabled() {
        return config.getBoolean("notifications.in-game.enabled", true);
    }

    public boolean isInGameAltDetection() {
        return config.getBoolean("notifications.in-game.alt-detection", true);
    }

    public boolean isInGameAdminLogin() {
        return config.getBoolean("notifications.in-game.admin-login", true);
    }

    public boolean isInGameSuspiciousActivity() {
        return config.getBoolean("notifications.in-game.suspicious-activity", true);
    }


    @SuppressWarnings("unchecked")
    public List<String> getCommandAliases() {
        return (List<String>) config.getList("commands.aliases", List.of("ipd", "ipdynamic"));
    }

    public int getCommandCooldown() {
        return config.getInt("commands.cooldown", 3);
    }


    public void setValue(String path, Object value) {
        config.set(path, value);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando configuración: " + e.getMessage());
        }
    }


    public void reload() {
        loadConfig();
    }


    public YamlConfiguration getConfig() {
        return config;
    }
}