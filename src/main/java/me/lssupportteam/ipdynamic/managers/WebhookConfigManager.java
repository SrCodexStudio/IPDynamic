package me.lssupportteam.ipdynamic.managers;

import me.lssupportteam.ipdynamic.IPDynamic;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class WebhookConfigManager {

    private final IPDynamic plugin;
    private final File webhookConfigFile;
    private YamlConfiguration webhookConfig;

    public WebhookConfigManager(IPDynamic plugin) {
        this.plugin = plugin;
        this.webhookConfigFile = new File(plugin.getDataFolder(), "webhook-config.yml");
    }

    public void loadConfig() {
        if (!webhookConfigFile.exists()) {
            saveDefaultWebhookConfig();
        }

        webhookConfig = YamlConfiguration.loadConfiguration(webhookConfigFile);
        addDefaultWebhookValues();

        plugin.getLogger().info(plugin.getLangManager() != null ? plugin.getLangManager().getMessage("webhook-config.loaded") : "Configuración de webhooks cargada correctamente.");
    }

    private void saveDefaultWebhookConfig() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            try (InputStream inputStream = plugin.getResource("webhook-config.yml")) {
                if (inputStream != null) {
                    Files.copy(inputStream, webhookConfigFile.toPath());
                } else {
                    createDefaultWebhookConfig();
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe((plugin.getLangManager() != null ? plugin.getLangManager().getMessage("webhook-config.create-error") : "Error creando archivo de configuración de webhooks: {error}").replace("{error}", e.getMessage()));
            createDefaultWebhookConfig();
        }
    }

    private void createDefaultWebhookConfig() {
        webhookConfig = new YamlConfiguration();


        webhookConfig.set("webhooks.enabled", false);
        webhookConfig.set("webhooks.timeout", 10);
        webhookConfig.set("webhooks.retry-attempts", 3);
        webhookConfig.set("webhooks.retry-delay", 5);


        webhookConfig.set("webhooks.urls.connection", "");
        webhookConfig.set("webhooks.urls.alt-detection", "");
        webhookConfig.set("webhooks.urls.ban", "");
        webhookConfig.set("webhooks.urls.admin", "");
        webhookConfig.set("webhooks.urls.custom", "");


        webhookConfig.set("notifications.connection.enabled", true);
        webhookConfig.set("notifications.connection.first-join-only", false);
        webhookConfig.set("notifications.connection.include-geo", true);
        webhookConfig.set("notifications.connection.include-alts", true);
        webhookConfig.set("notifications.connection.include-history", true);
        webhookConfig.set("notifications.connection.max-history-entries", 10);

        webhookConfig.set("notifications.alt-detection.enabled", true);
        webhookConfig.set("notifications.alt-detection.min-alts", 1);
        webhookConfig.set("notifications.alt-detection.max-alts-display", 10);
        webhookConfig.set("notifications.alt-detection.include-shared-ips", true);
        webhookConfig.set("notifications.alt-detection.cooldown", 300); // 5 minutos

        webhookConfig.set("notifications.ban.enabled", true);
        webhookConfig.set("notifications.ban.include-stats", true);
        webhookConfig.set("notifications.ban.notify-op1", true);
        webhookConfig.set("notifications.ban.notify-op2", true);
        webhookConfig.set("notifications.ban.notify-single", true);

        webhookConfig.set("notifications.admin.enabled", false);
        webhookConfig.set("notifications.admin.include-location", true);
        webhookConfig.set("notifications.admin.include-history", false);
        webhookConfig.set("notifications.admin.only-suspicious", false);


        webhookConfig.set("embeds.username", "IPDynamic 2.5-OMEGA");
        webhookConfig.set("embeds.avatar-url", "https://mc-heads.net/avatar/MHF_Exclamation/100");
        webhookConfig.set("embeds.footer-text", "IPDynamic Security System");
        webhookConfig.set("embeds.timestamp", true);
        webhookConfig.set("embeds.thumbnails", true);


        webhookConfig.set("embeds.colors.success", 0x00FF00);
        webhookConfig.set("embeds.colors.warning", 0xFFAA00);
        webhookConfig.set("embeds.colors.danger", 0xFF0000);
        webhookConfig.set("embeds.colors.info", 0x00AAFF);
        webhookConfig.set("embeds.colors.premium", 0x9B59B6);
        webhookConfig.set("embeds.colors.alt", 0xE74C3C);
        webhookConfig.set("embeds.colors.ban", 0xE67E22);
        webhookConfig.set("embeds.colors.unban", 0x2ECC71);
        webhookConfig.set("embeds.colors.admin", 0xF39C12);


        webhookConfig.set("filters.ignore-local-ips", true);
        webhookConfig.set("filters.ignore-whitelist", true);
        webhookConfig.set("filters.ip-whitelist", List.of());
        webhookConfig.set("filters.ip-blacklist", List.of());
        webhookConfig.set("filters.country-whitelist", List.of());
        webhookConfig.set("filters.country-blacklist", List.of());


        webhookConfig.set("advanced.queue-enabled", true);
        webhookConfig.set("advanced.queue-max-size", 1000);
        webhookConfig.set("advanced.rate-limit.enabled", true);
        webhookConfig.set("advanced.rate-limit.requests-per-minute", 30);
        webhookConfig.set("advanced.batch-notifications", false);
        webhookConfig.set("advanced.batch-size", 5);
        webhookConfig.set("advanced.batch-delay", 10);

        webhookConfig.set("discord.enabled", false);
        webhookConfig.set("discord.bot-token", "YOUR_BOT_TOKEN_HERE");
        webhookConfig.set("discord.channels.stats", "CHANNEL_ID_HERE");
        webhookConfig.set("discord.stats.auto-send-interval", 24);
        webhookConfig.set("discord.stats.include-empty-countries", false);
        webhookConfig.set("discord.stats.max-countries", 10);
        webhookConfig.set("discord.embed.color", 3447003);
        webhookConfig.set("discord.embed.thumbnail", true);
        webhookConfig.set("discord.embed.thumbnail-url", "https://cdn.discordapp.com/attachments/1234567890/globe.png");
        webhookConfig.set("discord.embed.footer-icon-url", "https://cdn.discordapp.com/attachments/1234567890/ipdynamic-icon.png");

        try {
            webhookConfig.save(webhookConfigFile);
            plugin.getLogger().info("Archivo de configuración de webhooks creado con valores por defecto.");
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando configuración de webhooks: " + e.getMessage());
        }
    }

    private void addDefaultWebhookValues() {
        boolean modified = false;


        if (!webhookConfig.contains("advanced.rate-limit.enabled")) {
            webhookConfig.set("advanced.rate-limit.enabled", true);
            webhookConfig.set("advanced.rate-limit.requests-per-minute", 30);
            modified = true;
        }

        if (!webhookConfig.contains("notifications.connection.max-history-entries")) {
            webhookConfig.set("notifications.connection.max-history-entries", 10);
            modified = true;
        }

        if (!webhookConfig.contains("embeds.thumbnails")) {
            webhookConfig.set("embeds.thumbnails", true);
            modified = true;
        }

        if (!webhookConfig.contains("discord.enabled")) {
            webhookConfig.set("discord.enabled", false);
            webhookConfig.set("discord.bot-token", "YOUR_BOT_TOKEN_HERE");
            webhookConfig.set("discord.channels.stats", "CHANNEL_ID_HERE");
            webhookConfig.set("discord.stats.auto-send-interval", 24);
            webhookConfig.set("discord.stats.include-empty-countries", false);
            webhookConfig.set("discord.stats.max-countries", 10);
            webhookConfig.set("discord.embed.color", 3447003);
            webhookConfig.set("discord.embed.thumbnail", true);
            webhookConfig.set("discord.embed.thumbnail-url", "https://cdn.discordapp.com/attachments/1234567890/globe.png");
            webhookConfig.set("discord.embed.footer-icon-url", "https://cdn.discordapp.com/attachments/1234567890/ipdynamic-icon.png");
            modified = true;
        }

        if (modified) {
            try {
                webhookConfig.save(webhookConfigFile);
                plugin.getLogger().info("Configuración de webhooks actualizada con nuevos valores.");
            } catch (IOException e) {
                plugin.getLogger().severe("Error actualizando configuración de webhooks: " + e.getMessage());
            }
        }
    }


    public boolean isWebhooksEnabled() {
        return webhookConfig.getBoolean("webhooks.enabled", false);
    }

    public int getTimeout() {
        return webhookConfig.getInt("webhooks.timeout", 10);
    }

    public int getRetryAttempts() {
        return webhookConfig.getInt("webhooks.retry-attempts", 3);
    }

    public int getRetryDelay() {
        return webhookConfig.getInt("webhooks.retry-delay", 5);
    }


    public String getConnectionWebhook() {
        return webhookConfig.getString("webhooks.urls.connection", "");
    }

    public String getAltWebhook() {
        return webhookConfig.getString("webhooks.urls.alt-detection", "");
    }

    public String getBanWebhook() {
        return webhookConfig.getString("webhooks.urls.ban", "");
    }

    public String getAdminWebhook() {
        return webhookConfig.getString("webhooks.urls.admin", "");
    }

    public String getCustomWebhook() {
        return webhookConfig.getString("webhooks.urls.custom", "");
    }


    public boolean isConnectionNotificationsEnabled() {
        return isWebhooksEnabled() &&
               webhookConfig.getBoolean("notifications.connection.enabled", true) &&
               !getConnectionWebhook().isEmpty();
    }

    public boolean isFirstJoinOnly() {
        return webhookConfig.getBoolean("notifications.connection.first-join-only", false);
    }

    public boolean isIncludeGeo() {
        return webhookConfig.getBoolean("notifications.connection.include-geo", true);
    }

    public boolean isIncludeAlts() {
        return webhookConfig.getBoolean("notifications.connection.include-alts", true);
    }

    public boolean isIncludeHistory() {
        return webhookConfig.getBoolean("notifications.connection.include-history", true);
    }

    public int getMaxHistoryEntries() {
        return webhookConfig.getInt("notifications.connection.max-history-entries", 10);
    }


    public boolean isAltNotificationsEnabled() {
        return isWebhooksEnabled() &&
               webhookConfig.getBoolean("notifications.alt-detection.enabled", true) &&
               !getAltWebhook().isEmpty();
    }

    public int getMinAlts() {
        return webhookConfig.getInt("notifications.alt-detection.min-alts", 1);
    }

    public int getMaxAltsDisplay() {
        return webhookConfig.getInt("notifications.alt-detection.max-alts-display", 10);
    }

    public boolean isIncludeSharedIps() {
        return webhookConfig.getBoolean("notifications.alt-detection.include-shared-ips", true);
    }

    public int getAltDetectionCooldown() {
        return webhookConfig.getInt("notifications.alt-detection.cooldown", 300);
    }


    public boolean isBanNotificationsEnabled() {
        return isWebhooksEnabled() &&
               webhookConfig.getBoolean("notifications.ban.enabled", true) &&
               !getBanWebhook().isEmpty();
    }

    public boolean isIncludeStats() {
        return webhookConfig.getBoolean("notifications.ban.include-stats", true);
    }

    public boolean isNotifyOp1() {
        return webhookConfig.getBoolean("notifications.ban.notify-op1", true);
    }

    public boolean isNotifyOp2() {
        return webhookConfig.getBoolean("notifications.ban.notify-op2", true);
    }

    public boolean isNotifySingle() {
        return webhookConfig.getBoolean("notifications.ban.notify-single", true);
    }


    public boolean isAdminNotificationsEnabled() {
        return isWebhooksEnabled() &&
               webhookConfig.getBoolean("notifications.admin.enabled", false) &&
               !getAdminWebhook().isEmpty();
    }

    public boolean isIncludeLocation() {
        return webhookConfig.getBoolean("notifications.admin.include-location", true);
    }

    public boolean isIncludeAdminHistory() {
        return webhookConfig.getBoolean("notifications.admin.include-history", false);
    }

    public boolean isOnlySuspicious() {
        return webhookConfig.getBoolean("notifications.admin.only-suspicious", false);
    }


    public String getEmbedUsername() {
        return webhookConfig.getString("embeds.username", "IPDynamic 2.5-OMEGA");
    }

    public String getAvatarUrl() {
        return webhookConfig.getString("embeds.avatar-url", "https://mc-heads.net/avatar/MHF_Exclamation/100");
    }

    public String getFooterText() {
        return webhookConfig.getString("embeds.footer-text", "IPDynamic Security System");
    }

    public boolean isTimestamp() {
        return webhookConfig.getBoolean("embeds.timestamp", true);
    }

    public boolean isThumbnails() {
        return webhookConfig.getBoolean("embeds.thumbnails", true);
    }


    public int getSuccessColor() {
        return webhookConfig.getInt("embeds.colors.success", 0x00FF00);
    }

    public int getWarningColor() {
        return webhookConfig.getInt("embeds.colors.warning", 0xFFAA00);
    }

    public int getDangerColor() {
        return webhookConfig.getInt("embeds.colors.danger", 0xFF0000);
    }

    public int getInfoColor() {
        return webhookConfig.getInt("embeds.colors.info", 0x00AAFF);
    }

    public int getPremiumColor() {
        return webhookConfig.getInt("embeds.colors.premium", 0x9B59B6);
    }

    public int getAltColor() {
        return webhookConfig.getInt("embeds.colors.alt", 0xE74C3C);
    }

    public int getBanColor() {
        return webhookConfig.getInt("embeds.colors.ban", 0xE67E22);
    }

    public int getUnbanColor() {
        return webhookConfig.getInt("embeds.colors.unban", 0x2ECC71);
    }

    public int getAdminColor() {
        return webhookConfig.getInt("embeds.colors.admin", 0xF39C12);
    }


    public boolean isIgnoreLocalIps() {
        return webhookConfig.getBoolean("filters.ignore-local-ips", true);
    }

    public boolean isIgnoreWhitelist() {
        return webhookConfig.getBoolean("filters.ignore-whitelist", true);
    }

    @SuppressWarnings("unchecked")
    public List<String> getIpWhitelist() {
        return (List<String>) webhookConfig.getList("filters.ip-whitelist", List.of());
    }

    @SuppressWarnings("unchecked")
    public List<String> getIpBlacklist() {
        return (List<String>) webhookConfig.getList("filters.ip-blacklist", List.of());
    }

    @SuppressWarnings("unchecked")
    public List<String> getCountryWhitelist() {
        return (List<String>) webhookConfig.getList("filters.country-whitelist", List.of());
    }

    @SuppressWarnings("unchecked")
    public List<String> getCountryBlacklist() {
        return (List<String>) webhookConfig.getList("filters.country-blacklist", List.of());
    }


    public boolean isQueueEnabled() {
        return webhookConfig.getBoolean("advanced.queue-enabled", true);
    }

    public int getQueueMaxSize() {
        return webhookConfig.getInt("advanced.queue-max-size", 1000);
    }

    public boolean isRateLimitEnabled() {
        return webhookConfig.getBoolean("advanced.rate-limit.enabled", true);
    }

    public int getRequestsPerMinute() {
        return webhookConfig.getInt("advanced.rate-limit.requests-per-minute", 30);
    }

    public boolean isBatchNotifications() {
        return webhookConfig.getBoolean("advanced.batch-notifications", false);
    }

    public int getBatchSize() {
        return webhookConfig.getInt("advanced.batch-size", 5);
    }

    public int getBatchDelay() {
        return webhookConfig.getInt("advanced.batch-delay", 10);
    }


    public void setValue(String path, Object value) {
        webhookConfig.set(path, value);
        try {
            webhookConfig.save(webhookConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando configuración de webhooks: " + e.getMessage());
        }
    }

    public void reload() {
        loadConfig();
    }

    public YamlConfiguration getConfig() {
        return webhookConfig;
    }

    public boolean isDiscordEnabled() {
        return webhookConfig.getBoolean("discord.enabled", false);
    }

    public String getDiscordBotToken() {
        return webhookConfig.getString("discord.bot-token", "YOUR_BOT_TOKEN_HERE");
    }

    public String getDiscordStatsChannel() {
        return webhookConfig.getString("discord.channels.stats", "CHANNEL_ID_HERE");
    }

    public int getDiscordStatsInterval() {
        return webhookConfig.getInt("discord.stats.auto-send-interval", 24);
    }

    public boolean isIncludeEmptyCountries() {
        return webhookConfig.getBoolean("discord.stats.include-empty-countries", false);
    }

    public int getMaxCountries() {
        return webhookConfig.getInt("discord.stats.max-countries", 10);
    }

    public int getDiscordEmbedColor() {
        return webhookConfig.getInt("discord.embed.color", 3447003);
    }

    public boolean isDiscordThumbnail() {
        return webhookConfig.getBoolean("discord.embed.thumbnail", true);
    }

    public String getDiscordThumbnailUrl() {
        return webhookConfig.getString("discord.embed.thumbnail-url", "https://cdn.discordapp.com/attachments/1234567890/globe.png");
    }

    public String getDiscordFooterIconUrl() {
        return webhookConfig.getString("discord.embed.footer-icon-url", "https://cdn.discordapp.com/attachments/1234567890/ipdynamic-icon.png");
    }
}