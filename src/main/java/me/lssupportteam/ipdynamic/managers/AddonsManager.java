package me.lssupportteam.ipdynamic.managers;

import me.lssupportteam.ipdynamic.IPDynamic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class AddonsManager {

    private final IPDynamic plugin;
    private final File addonsDir;
    private final Map<String, FileConfiguration> addonConfigs;

    public AddonsManager(IPDynamic plugin) {
        this.plugin = plugin;
        this.addonsDir = new File(plugin.getDataFolder(), "addons");
        this.addonConfigs = new HashMap<>();
    }

    public void initialize() {
        createAddonsDirectory();
        createDefaultAddonFiles();
        loadAllAddons();

        plugin.getLogger().info("AddonsManager inicializado correctamente");
    }

    private void createAddonsDirectory() {
        if (!addonsDir.exists()) {
            if (addonsDir.mkdirs()) {
                plugin.getLogger().info("Carpeta addons/ creada exitosamente");
            } else {
                plugin.getLogger().severe("Error creando carpeta addons/");
            }
        }
    }

    private void createDefaultAddonFiles() {
        createDefaultAddon("discord.yml");
        createDefaultAddon("stats.yml");
    }

    private void createDefaultAddon(String fileName) {
        File addonFile = new File(addonsDir, fileName);

        if (!addonFile.exists()) {
            try {
                // Intentar copiar desde recursos internos
                InputStream resource = plugin.getResource("addons/" + fileName);
                if (resource != null) {
                    java.nio.file.Files.copy(resource, addonFile.toPath());
                    plugin.getLogger().info("Archivo addon creado: " + fileName);
                } else {
                    // Crear archivo básico si no existe en recursos
                    createBasicAddonFile(addonFile, fileName);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Error creando addon " + fileName, e);
                createBasicAddonFile(addonFile, fileName);
            }
        }
    }

    private void createBasicAddonFile(File file, String fileName) {
        try {
            FileConfiguration config = new YamlConfiguration();

            switch (fileName) {
                case "discord.yml":
                    createDiscordConfig(config);
                    break;
                case "stats.yml":
                    createStatsConfig(config);
                    break;
            }

            config.save(file);
            plugin.getLogger().info("Archivo addon básico creado: " + fileName);

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando addon " + fileName, e);
        }
    }

    private void createDiscordConfig(FileConfiguration config) {
        // Discord Bot Configuration
        config.set("enabled", false);
        config.set("bot.token", "YOUR_BOT_TOKEN_HERE");
        config.set("bot.activity", "IPDynamic 2.5-OMEGA");
        config.set("bot.status", "ONLINE");

        // Server Configuration
        config.set("server.guild-id", "YOUR_GUILD_ID_HERE");
        config.set("server.logs-channel-id", "YOUR_LOGS_CHANNEL_ID_HERE");

        // Connection Settings
        config.set("connection.auto-reconnect", true);
        config.set("connection.reconnect-delay", 5);
        config.set("connection.max-reconnect-attempts", 10);
        config.set("connection.timeout", 30);

        // Stats Settings
        config.set("stats.edit-message-enabled", true);

        // Debug Settings
        config.set("debug.enabled", true);
        config.set("debug.log-level", "INFO");
        config.set("debug.log-api-calls", false);

        // Comments
        config.setComments("enabled", java.util.Arrays.asList(
            "Habilitar/deshabilitar el sistema Discord Bot",
            "Si está deshabilitado, no se iniciará el bot"
        ));

        config.setComments("bot.token", java.util.Arrays.asList(
            "Token del bot de Discord",
            "Obtén tu token en: https://discord.com/developers/applications"
        ));
    }

    private void createStatsConfig(FileConfiguration config) {
        // Stats Configuration
        config.set("enabled", true);
        config.set("auto-send.enabled", true);
        config.set("auto-send.interval", 300); // 5 minutes for real-time updates
        config.set("auto-send.channel-id", "YOUR_STATS_CHANNEL_ID");

        // Top Countries Configuration
        config.set("top-countries.enabled", true);
        config.set("top-countries.limit", 10);
        config.set("top-countries.show-flags", true);
        config.set("top-countries.show-percentages", true);

        // Embed Configuration
        config.set("embed.title", "🌍 TOP {limit} Países");
        config.set("embed.description", "");
        config.set("embed.color", "#00FF00");

        // Content Configuration
        config.set("embed.content.separator", "");

        // Footer Configuration
        config.set("embed.footer.text", "IPDynamic • Última actualización");
        config.set("embed.footer.icon", "");

        // Optional fields
        config.set("embed.thumbnail", "");
        config.set("embed.author.name", "");
        config.set("embed.author.url", "");
        config.set("embed.author.icon", "");
        config.set("embed.image", "");
        config.set("embed.timestamp", true);

        // Field Templates
        config.set("templates.country-entry", "{medal} **{country}** {flag} ➜ `{connections:,}` conexiones (**{percentage}%**)");
        config.set("templates.no-data", "❌ No hay datos disponibles");

        // Medal Configuration
        config.set("medals.first", "🥇");
        config.set("medals.second", "🥈");
        config.set("medals.third", "🥉");
        config.set("medals.default", "🔹");
    }


    private void loadAllAddons() {
        File[] addonFiles = addonsDir.listFiles((dir, name) -> name.endsWith(".yml"));

        if (addonFiles != null) {
            for (File addonFile : addonFiles) {
                loadAddon(addonFile);
            }
        }
    }

    private void loadAddon(File addonFile) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(addonFile);
            String addonName = addonFile.getName().replace(".yml", "");

            addonConfigs.put(addonName, config);
            plugin.getLogger().info("Addon cargado: " + addonName);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error cargando addon: " + addonFile.getName(), e);
        }
    }

    public FileConfiguration getAddonConfig(String addonName) {
        return addonConfigs.get(addonName);
    }

    public boolean isAddonEnabled(String addonName) {
        FileConfiguration config = getAddonConfig(addonName);
        return config != null && config.getBoolean("enabled", false);
    }

    public void reloadAddon(String addonName) {
        File addonFile = new File(addonsDir, addonName + ".yml");
        if (addonFile.exists()) {
            loadAddon(addonFile);
            plugin.getLogger().info("Addon recargado: " + addonName);
        }
    }

    public void reloadAllAddons() {
        addonConfigs.clear();
        loadAllAddons();
        plugin.getLogger().info("Todos los addons recargados");
    }

    public File getAddonsDirectory() {
        return addonsDir;
    }

    public Map<String, FileConfiguration> getAllAddonConfigs() {
        return new HashMap<>(addonConfigs);
    }
}