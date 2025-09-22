package me.lssupportteam.ipdynamic.managers;

import me.lssupportteam.ipdynamic.IPDynamic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;

public class ConfigMigrator {

    private final IPDynamic plugin;
    private final SimpleDateFormat dateFormat;

    // Current version of configuration files
    private static final String CONFIG_VERSION = "2.5-OMEGA";
    private static final String CURRENT_SCHEMA_VERSION = "1.0";

    public ConfigMigrator(IPDynamic plugin) {
        this.plugin = plugin;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    }

    /**
     * Migrates all configuration files to the latest version
     */
    public void migrateAllConfigs() {
        plugin.getLogger().info("🔄 Verificando actualizaciones de configuración...");

        // Skip data folder migration as requested
        migrateMainConfig();
        migrateWebhookConfig();
        migrateLangFiles();
        migrateAddonConfigs();

        plugin.getLogger().info("✅ Verificación de configuraciones completada");
    }

    /**
     * Migrates the main config.yml file
     */
    private void migrateMainConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) return;

        try {
            FileConfiguration existingConfig = YamlConfiguration.loadConfiguration(configFile);
            InputStream defaultResource = plugin.getResource("config.yml");

            if (defaultResource != null) {
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(defaultResource, java.nio.charset.StandardCharsets.UTF_8)
                );

                if (needsMigration(existingConfig, defaultConfig, "config.yml")) {
                    plugin.getLogger().info("🔧 Actualizando config.yml con nuevas opciones...");

                    createBackup(configFile, "config.yml");
                    mergeConfigurations(existingConfig, defaultConfig);

                    // Update version info
                    existingConfig.set("version", CONFIG_VERSION);
                    existingConfig.set("schema-version", CURRENT_SCHEMA_VERSION);

                    existingConfig.save(configFile);
                    plugin.getLogger().info("✅ config.yml actualizado exitosamente");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error migrando config.yml", e);
        }
    }

    /**
     * Migrates webhook-config.yml
     */
    private void migrateWebhookConfig() {
        File webhookFile = new File(plugin.getDataFolder(), "webhook-config.yml");
        if (!webhookFile.exists()) return;

        try {
            FileConfiguration existingConfig = YamlConfiguration.loadConfiguration(webhookFile);
            InputStream defaultResource = plugin.getResource("webhook-config.yml");

            if (defaultResource != null) {
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(defaultResource, java.nio.charset.StandardCharsets.UTF_8)
                );

                if (needsMigration(existingConfig, defaultConfig, "webhook-config.yml")) {
                    plugin.getLogger().info("🔧 Actualizando webhook-config.yml con nuevas opciones...");

                    createBackup(webhookFile, "webhook-config.yml");
                    mergeConfigurations(existingConfig, defaultConfig);

                    existingConfig.set("version", CONFIG_VERSION);
                    existingConfig.save(webhookFile);
                    plugin.getLogger().info("✅ webhook-config.yml actualizado exitosamente");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error migrando webhook-config.yml", e);
        }
    }

    /**
     * Migrates language files
     */
    private void migrateLangFiles() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) return;

        File[] langFiles = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (langFiles == null) return;

        for (File langFile : langFiles) {
            migrateLangFile(langFile);
        }
    }

    private void migrateLangFile(File langFile) {
        try {
            FileConfiguration existingLang = YamlConfiguration.loadConfiguration(langFile);
            InputStream defaultResource = plugin.getResource("lang/" + langFile.getName());

            if (defaultResource != null) {
                FileConfiguration defaultLang = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(defaultResource, java.nio.charset.StandardCharsets.UTF_8)
                );

                if (needsMigration(existingLang, defaultLang, langFile.getName())) {
                    plugin.getLogger().info("🔧 Actualizando " + langFile.getName() + " con nuevos mensajes...");

                    createBackup(langFile, langFile.getName());
                    mergeConfigurations(existingLang, defaultLang);

                    existingLang.set("version", CONFIG_VERSION);
                    existingLang.save(langFile);
                    plugin.getLogger().info("✅ " + langFile.getName() + " actualizado exitosamente");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error migrando " + langFile.getName(), e);
        }
    }

    /**
     * Migrates addon configurations
     */
    private void migrateAddonConfigs() {
        File addonsDir = new File(plugin.getDataFolder(), "addons");
        if (!addonsDir.exists()) return;

        File[] addonFiles = addonsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (addonFiles == null) return;

        for (File addonFile : addonFiles) {
            migrateAddonFile(addonFile);
        }
    }

    private void migrateAddonFile(File addonFile) {
        try {
            FileConfiguration existingAddon = YamlConfiguration.loadConfiguration(addonFile);
            InputStream defaultResource = plugin.getResource("addons/" + addonFile.getName());

            if (defaultResource != null) {
                FileConfiguration defaultAddon = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(defaultResource, java.nio.charset.StandardCharsets.UTF_8)
                );

                if (needsMigration(existingAddon, defaultAddon, addonFile.getName())) {
                    plugin.getLogger().info("🔧 Actualizando " + addonFile.getName() + " con nuevas opciones...");

                    createBackup(addonFile, addonFile.getName());
                    mergeConfigurations(existingAddon, defaultAddon);

                    existingAddon.set("version", CONFIG_VERSION);
                    existingAddon.save(addonFile);
                    plugin.getLogger().info("✅ " + addonFile.getName() + " actualizado exitosamente");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error migrando " + addonFile.getName(), e);
        }
    }

    /**
     * Checks if a configuration file needs migration
     */
    private boolean needsMigration(FileConfiguration existing, FileConfiguration defaultConfig, String fileName) {
        // Check version first
        String existingVersion = existing.getString("version", "");
        String existingSchema = existing.getString("schema-version", "");

        // If versions don't match, check for new keys
        if (!CONFIG_VERSION.equals(existingVersion) || !CURRENT_SCHEMA_VERSION.equals(existingSchema)) {
            return hasNewKeys(existing, defaultConfig, "");
        }

        return false;
    }

    /**
     * Recursively checks for new keys in the default configuration
     */
    private boolean hasNewKeys(FileConfiguration existing, FileConfiguration defaultConfig, String path) {
        Set<String> defaultKeys = defaultConfig.getKeys(false);

        for (String key : defaultKeys) {
            String fullPath = path.isEmpty() ? key : path + "." + key;

            if (!existing.contains(fullPath)) {
                plugin.getLogger().info("🆕 Nueva configuración encontrada: " + fullPath);
                return true;
            }

            // Check nested sections
            if (defaultConfig.isConfigurationSection(key)) {
                ConfigurationSection defaultSection = defaultConfig.getConfigurationSection(key);
                if (existing.isConfigurationSection(fullPath)) {
                    ConfigurationSection existingSection = existing.getConfigurationSection(fullPath);

                    // Create temporary configurations for recursion
                    FileConfiguration tempDefault = new YamlConfiguration();
                    FileConfiguration tempExisting = new YamlConfiguration();

                    // Copy section contents
                    for (String subKey : defaultSection.getKeys(true)) {
                        tempDefault.set(subKey, defaultSection.get(subKey));
                    }
                    for (String subKey : existingSection.getKeys(true)) {
                        tempExisting.set(subKey, existingSection.get(subKey));
                    }

                    if (hasNewKeys(tempExisting, tempDefault, "")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Merges default configuration with existing, preserving user values
     */
    private void mergeConfigurations(FileConfiguration existing, FileConfiguration defaultConfig) {
        mergeSection(existing, defaultConfig, "");
    }

    private void mergeSection(FileConfiguration existing, FileConfiguration defaultConfig, String path) {
        Set<String> defaultKeys = defaultConfig.getKeys(false);

        for (String key : defaultKeys) {
            String fullPath = path.isEmpty() ? key : path + "." + key;

            if (!existing.contains(fullPath)) {
                // Add new key with default value
                Object defaultValue = defaultConfig.get(key);
                existing.set(fullPath, defaultValue);
                plugin.getLogger().info("➕ Añadida nueva opción: " + fullPath + " = " + defaultValue);
            } else if (defaultConfig.isConfigurationSection(key) && existing.isConfigurationSection(fullPath)) {
                // Recursively merge sections
                ConfigurationSection defaultSection = defaultConfig.getConfigurationSection(key);

                FileConfiguration tempDefault = new YamlConfiguration();
                for (String subKey : defaultSection.getKeys(true)) {
                    tempDefault.set(subKey, defaultSection.get(subKey));
                }

                mergeSection(existing, tempDefault, fullPath);
            }
        }
    }

    /**
     * Creates a backup of the configuration file before migration
     */
    private void createBackup(File originalFile, String fileName) {
        try {
            File backupDir = new File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            String timestamp = dateFormat.format(new Date());
            String backupName = fileName.replace(".yml", "_" + timestamp + ".yml.backup");
            File backupFile = new File(backupDir, backupName);

            Files.copy(originalFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("💾 Backup creado: " + backupName);

        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error creando backup de " + fileName, e);
        }
    }
}