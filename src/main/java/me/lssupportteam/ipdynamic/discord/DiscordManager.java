package me.lssupportteam.ipdynamic.discord;

import me.lssupportteam.ipdynamic.IPDynamic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.logging.Level;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DiscordManager {

    private final IPDynamic plugin;
    private final ExecutorService discordExecutor;
    private final HttpClient httpClient;

    // Configuration
    private FileConfiguration discordConfig;
    private boolean enabled;
    private String botToken;
    private String guildId;
    private String logsChannelId;

    // Connection settings
    private boolean autoReconnect;
    private int reconnectDelay;
    private int maxReconnectAttempts;
    private int timeout;

    // Debug settings
    private boolean debugEnabled;
    private String debugLevel;
    private boolean logApiCalls;

    // Services
    private DiscordStatsService statsService;
    private BukkitTask statsTask;
    private BukkitTask heartbeatTask;

    // Connection state
    private volatile boolean connected = false;
    private int reconnectAttempts = 0;
    private long lastHeartbeat = 0;

    // Message cache for anti-spam
    private String lastStatsMessageId = null;
    private long lastStatsMessageTime = 0;

    public DiscordManager(IPDynamic plugin) {
        this.plugin = plugin;
        this.discordExecutor = Executors.newFixedThreadPool(4);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    /**
     * Check if Discord configuration is valid and should be initialized
     */
    public static boolean shouldInitialize(IPDynamic plugin) {
        try {
            FileConfiguration discordConfig = plugin.getAddonsManager().getAddonConfig("discord");
            if (discordConfig == null) {
                return false;
            }

            boolean enabled = discordConfig.getBoolean("enabled", false);
            if (!enabled) {
                return false;
            }

            String botToken = discordConfig.getString("bot.token", "YOUR_BOT_TOKEN_HERE");
            String guildId = discordConfig.getString("server.guild-id", "YOUR_GUILD_ID_HERE");

            // Don't initialize if using default values
            if (botToken.equals("YOUR_BOT_TOKEN_HERE") || guildId.equals("YOUR_GUILD_ID_HERE")) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void initialize() {
        plugin.getLogger().info("🔄 Inicializando DiscordManager...");

        if (!loadDiscordConfig()) {
            plugin.getLogger().severe("❌ Error cargando configuración Discord");
            return;
        }

        if (!enabled) {
            plugin.getLogger().info("⚠️ Discord Bot deshabilitado en configuración (enabled: false)");
            return;
        }

        if (!validateConfiguration()) {
            plugin.getLogger().warning("❌ Configuración Discord inválida - revisa token, guild-id y channel-id");
            return;
        }

        initializeServices();
        startConnection();
    }

    private boolean loadDiscordConfig() {
        try {
            discordConfig = plugin.getAddonsManager().getAddonConfig("discord");
            if (discordConfig == null) {
                plugin.getLogger().severe("❌ No se pudo cargar discord.yml de addons/");
                return false;
            }

            // Load main settings
            enabled = discordConfig.getBoolean("enabled", false);
            botToken = discordConfig.getString("bot.token", "");
            guildId = discordConfig.getString("server.guild-id", "");
            logsChannelId = discordConfig.getString("server.logs-channel-id", "");

            // Load connection settings
            autoReconnect = discordConfig.getBoolean("connection.auto-reconnect", true);
            reconnectDelay = discordConfig.getInt("connection.reconnect-delay", 5);
            maxReconnectAttempts = discordConfig.getInt("connection.max-reconnect-attempts", 10);
            timeout = discordConfig.getInt("connection.timeout", 30);

            // Load debug settings
            debugEnabled = discordConfig.getBoolean("debug.enabled", true);
            debugLevel = discordConfig.getString("debug.log-level", "INFO");
            logApiCalls = discordConfig.getBoolean("debug.log-api-calls", false);

            logDebug("Configuración Discord cargada exitosamente", Level.INFO);
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error cargando configuración Discord", e);
            return false;
        }
    }

    private boolean validateConfiguration() {
        plugin.getLogger().info("🔍 Validando configuración Discord...");

        if (botToken == null || botToken.isEmpty() || botToken.equals("YOUR_BOT_TOKEN_HERE")) {
            plugin.getLogger().info("⚠️ Discord Bot no configurado (token por defecto detectado)");
            plugin.getLogger().info("   Para habilitar Discord: configura 'bot.token' en addons/discord.yml");
            return false;
        }

        if (guildId == null || guildId.isEmpty() || guildId.equals("YOUR_GUILD_ID_HERE")) {
            plugin.getLogger().info("⚠️ Discord Bot no configurado (guild-id por defecto detectado)");
            plugin.getLogger().info("   Para habilitar Discord: configura 'server.guild-id' en addons/discord.yml");
            return false;
        }

        // Check for logs channel ID if it's still default
        if (logsChannelId != null && logsChannelId.equals("YOUR_LOGS_CHANNEL_ID_HERE")) {
            plugin.getLogger().info("⚠️ Canal de logs no configurado, Discord funcionará sin notificaciones");
        }

        // Validate token format (basic check)
        if (!botToken.matches("^[A-Za-z0-9._-]+$")) {
            plugin.getLogger().warning("❌ Formato de token inválido");
            plugin.getLogger().warning("   El token debe contener solo letras, números, puntos, guiones y guiones bajos");
            return false;
        }

        plugin.getLogger().info("✅ Configuración Discord validada correctamente");
        return true;
    }

    private void initializeServices() {
        try {
            statsService = new DiscordStatsService(plugin, this);
            logDebug("✅ DiscordStatsService inicializado", Level.INFO);
        } catch (Exception e) {
            logDebug("❌ Error inicializando servicios Discord: " + e.getMessage(), Level.SEVERE);
            if (debugEnabled) {
                e.printStackTrace();
            }
        }
    }

    private void startConnection() {
        CompletableFuture.runAsync(() -> {
            logDebug("🔄 Iniciando conexión con Discord...", Level.INFO);

            if (testBotConnection()) {
                connected = true;
                reconnectAttempts = 0;
                logDebug("✅ Bot conectado exitosamente a Discord", Level.INFO);

                startHeartbeat();

                // Send initial stats immediately upon connection
                plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    logDebug("📊 Enviando estadísticas iniciales al conectar...", Level.INFO);
                    sendCountryStatsAsync();
                }, 40L); // Wait 2 seconds for everything to be ready

                // Schedule regular stats updates every 5 minutes
                scheduleStatsUpdate();

                plugin.getLogger().info("🤖 Discord Bot inicializado correctamente");
            } else {
                logDebug("❌ Error conectando con Discord", Level.SEVERE);
                if (autoReconnect && reconnectAttempts < maxReconnectAttempts) {
                    scheduleReconnect();
                }
            }
        }, discordExecutor);
    }

    private boolean testBotConnection() {
        try {
            String url = "https://discord.com/api/v10/users/@me";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bot " + botToken)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeout))
                .GET()
                .build();

            if (logApiCalls) {
                logDebug("🌐 API Call: GET " + url, Level.INFO);
            }

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                logDebug("✅ Conexión con Discord API exitosa", Level.INFO);
                return true;
            } else {
                logDebug("❌ Error de API Discord: " + response.statusCode() + " - " + response.body(), Level.SEVERE);
                return false;
            }

        } catch (Exception e) {
            logDebug("❌ Excepción probando conexión Discord: " + e.getMessage(), Level.SEVERE);
            if (debugEnabled) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private void startHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel();
        }

        heartbeatTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (connected) {
                lastHeartbeat = System.currentTimeMillis();

                // Test connection every 5 minutes
                if (!testBotConnection()) {
                    logDebug("💔 Heartbeat falló - conexión perdida", Level.WARNING);
                    connected = false;

                    if (autoReconnect && reconnectAttempts < maxReconnectAttempts) {
                        scheduleReconnect();
                    }
                }
            }
        }, 6000L, 6000L); // Every 5 minutes
    }

    private void scheduleReconnect() {
        reconnectAttempts++;
        logDebug("🔄 Programando reconexión #" + reconnectAttempts + " en " + reconnectDelay + " segundos", Level.INFO);

        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            startConnection();
        }, reconnectDelay * 20L);
    }

    public void scheduleStatsUpdate() {
        if (!isStatsEnabled()) return;

        FileConfiguration statsConfig = plugin.getAddonsManager().getAddonConfig("stats");
        if (statsConfig == null || !statsConfig.getBoolean("auto-send.enabled", false)) {
            return;
        }

        // Force 5 minutes interval (300 seconds) for real-time updates
        int intervalSeconds = 300; // 5 minutes
        long intervalTicks = intervalSeconds * 20L;

        if (statsTask != null && !statsTask.isCancelled()) {
            statsTask.cancel();
        }

        statsTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (connected && statsService != null) {
                sendCountryStatsAsync();
            }
        }, intervalTicks, intervalTicks);

        logDebug("📊 Stats automáticos programados cada " + intervalSeconds + " segundos", Level.INFO);
    }

    public CompletableFuture<Void> sendCountryStatsAsync() {
        if (!connected || statsService == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                logDebug("📊 Enviando estadísticas de países...", Level.INFO);

                // Check if we should edit existing message or send new one
                boolean editEnabled = discordConfig.getBoolean("stats.edit-message-enabled", true);
                logDebug("🔧 Modo edición de mensajes: " + (editEnabled ? "HABILITADO" : "DESHABILITADO"), Level.INFO);

                if (editEnabled) {
                    String messageId = findLastStatsMessage();
                    if (messageId != null) {
                        logDebug("✅ Mensaje anterior encontrado: " + messageId, Level.INFO);
                        logDebug("🔄 Editando mensaje existente en lugar de crear uno nuevo", Level.INFO);
                        statsService.editCountryStatsEmbed(messageId);
                        logDebug("✅ Mensaje editado exitosamente", Level.INFO);
                        return; // Exit early, don't send new message
                    } else {
                        logDebug("⚠️ No se encontró mensaje previo para editar", Level.INFO);
                    }
                }

                logDebug("📤 Enviando nuevo mensaje de estadísticas", Level.INFO);
                String newMessageId = statsService.sendCountryStatsEmbed();
                if (newMessageId != null) {
                    lastStatsMessageId = newMessageId;
                    lastStatsMessageTime = System.currentTimeMillis();
                    logDebug("✅ Nuevo mensaje enviado con ID: " + newMessageId, Level.INFO);
                } else {
                    logDebug("❌ Error al enviar nuevo mensaje", Level.WARNING);
                }

                logDebug("✅ Estadísticas enviadas exitosamente", Level.INFO);
            } catch (Exception e) {
                logDebug("❌ Error enviando estadísticas: " + e.getMessage(), Level.SEVERE);
                if (debugEnabled) {
                    e.printStackTrace();
                }
            }
        }, discordExecutor);
    }

    private String findLastStatsMessage() {
        try {
            // Get stats channel ID from stats.yml
            FileConfiguration statsConfig = plugin.getAddonsManager().getAddonConfig("stats");
            if (statsConfig == null) return null;

            String statsChannelId = statsConfig.getString("auto-send.channel-id", "");
            if (statsChannelId.isEmpty() || statsChannelId.equals("YOUR_STATS_CHANNEL_ID")) {
                return null;
            }

            // Get recent messages from stats channel (increased limit for better detection)
            String url = "https://discord.com/api/v10/channels/" + statsChannelId + "/messages?limit=50";
            logDebug("🔍 Buscando mensaje de estadísticas previo en canal: " + statsChannelId, Level.INFO);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bot " + botToken)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeout))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonArray messages = JsonParser.parseString(response.body()).getAsJsonArray();
                logDebug("📨 Encontrados " + messages.size() + " mensajes en el canal", Level.INFO);

                // Look for the bot's last message with embed
                for (int i = 0; i < messages.size(); i++) {
                    JsonObject message = messages.get(i).getAsJsonObject();

                    // Check if message is from our bot
                    JsonObject author = message.getAsJsonObject("author");
                    if (author != null && author.has("bot") && author.get("bot").getAsBoolean()) {

                        // Check if message has embeds
                        JsonArray embeds = message.getAsJsonArray("embeds");
                        if (embeds != null && embeds.size() > 0) {
                            JsonObject embed = embeds.get(0).getAsJsonObject();

                            // Check multiple ways to identify our stats message
                            boolean isStatsMessage = false;

                            // Check title if exists
                            if (embed.has("title") && !embed.get("title").isJsonNull()) {
                                String title = embed.get("title").getAsString();
                                if (title.contains("TOP") || title.contains("Países") || title.contains("País")) {
                                    isStatsMessage = true;
                                }
                            }

                            // Check fields for our specific content pattern
                            if (!isStatsMessage && embed.has("fields")) {
                                JsonArray fields = embed.getAsJsonArray("fields");
                                for (int j = 0; j < fields.size(); j++) {
                                    JsonObject field = fields.get(j).getAsJsonObject();
                                    if (field.has("value")) {
                                        String value = field.get("value").getAsString();
                                        // Look for country entries with medals or specific patterns
                                        if (value.contains("🥇") || value.contains("🥈") || value.contains("🥉") ||
                                            (value.contains("conexiones") && value.contains("%"))) {
                                            isStatsMessage = true;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (isStatsMessage) {
                                String messageId = message.get("id").getAsString();
                                lastStatsMessageId = messageId;
                                lastStatsMessageTime = System.currentTimeMillis();

                                logDebug("📝 Mensaje de estadísticas encontrado: " + messageId, Level.INFO);
                                return messageId;
                            }
                        }
                    }
                }

                logDebug("⚠️ No se encontró mensaje de estadísticas en los últimos " + messages.size() + " mensajes", Level.INFO);
            } else {
                logDebug("❌ Error obteniendo mensajes del canal: " + response.statusCode(), Level.WARNING);
            }
        } catch (Exception e) {
            logDebug("❌ Error buscando último mensaje: " + e.getMessage(), Level.WARNING);
            if (debugEnabled) {
                e.printStackTrace();
            }
        }

        return null;
    }


    public void reload() {
        logDebug("🔄 Recargando DiscordManager (modo suave)...", Level.INFO);

        // Cancel tasks but don't shutdown executor
        if (statsTask != null && !statsTask.isCancelled()) {
            statsTask.cancel();
            statsTask = null;
        }

        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel();
            heartbeatTask = null;
        }

        // Reload configuration
        if (!loadDiscordConfig()) {
            logDebug("Error recargando configuración Discord", Level.SEVERE);
            return;
        }

        // Check if Discord is enabled
        if (!enabled) {
            logDebug("Discord Bot deshabilitado, omitiendo conexión", Level.INFO);
            connected = false;
            return;
        }

        // Validate configuration before attempting any connection
        if (!validateConfiguration()) {
            logDebug("Configuración Discord inválida o no configurada, omitiendo conexión", Level.INFO);
            connected = false;
            return;
        }

        // If already connected and config is still valid, just reschedule tasks
        if (connected) {
            logDebug("🔄 Manteniendo conexión existente, solo actualizando configuración", Level.INFO);
            startHeartbeat();
            scheduleStatsUpdate();
            return;
        }

        // Otherwise, do a full reconnect only if config is valid
        logDebug("🔄 Reconectando Discord Bot...", Level.INFO);
        connected = false;
        reconnectAttempts = 0;
        startConnection();
    }

    public void shutdown() {
        logDebug("🔌 Cerrando DiscordManager...", Level.INFO);

        connected = false;

        if (statsTask != null && !statsTask.isCancelled()) {
            statsTask.cancel();
        }

        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel();
        }

        if (discordExecutor != null && !discordExecutor.isShutdown()) {
            discordExecutor.shutdown();
            try {
                if (!discordExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    discordExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                discordExecutor.shutdownNow();
            }
        }
    }

    // Status and diagnostic methods
    public String getConnectionStatus() {
        if (!enabled) return "DISABLED";
        if (!connected) return "DISCONNECTED";
        return "CONNECTED";
    }

    public String getDiagnosticInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== DISCORD BOT DIAGNOSTIC ===\n");

        // Check if addon config exists
        if (discordConfig == null) {
            info.append("❌ Configuración no cargada - archivo addons/discord.yml no encontrado\n");
            return info.toString();
        }

        info.append("✅ Archivo de configuración cargado\n");
        info.append("Estado: ").append(getConnectionStatus()).append("\n");
        info.append("Habilitado: ").append(enabled ? "Sí" : "No").append("\n");

        if (enabled) {
            // Check configuration values
            info.append("Token configurado: ").append(botToken != null && !botToken.equals("YOUR_BOT_TOKEN_HERE") ? "Sí" : "No").append("\n");
            info.append("Guild ID configurado: ").append(guildId != null && !guildId.equals("YOUR_GUILD_ID_HERE") ? "Sí" : "No").append("\n");

            // Check stats channel from stats.yml
            FileConfiguration statsConfig = plugin.getAddonsManager().getAddonConfig("stats");
            if (statsConfig != null) {
                String statsChannelId = statsConfig.getString("auto-send.channel-id", "");
                info.append("Stats Channel ID configurado: ").append(!statsChannelId.isEmpty() && !statsChannelId.equals("YOUR_STATS_CHANNEL_ID") ? "Sí" : "No").append("\n");
            } else {
                info.append("Stats Channel ID configurado: No (stats.yml no encontrado)\n");
            }

            if (connected) {
                info.append("✅ Bot conectado exitosamente\n");
                if (lastHeartbeat > 0) {
                    long timeSince = (System.currentTimeMillis() - lastHeartbeat) / 1000;
                    info.append("Último heartbeat: ").append(timeSince).append("s atrás\n");
                }
            } else {
                info.append("❌ Bot no conectado\n");
                if (reconnectAttempts > 0) {
                    info.append("Intentos de reconexión: ").append(reconnectAttempts).append("\n");
                }
            }
        }

        info.append("===============================");
        return info.toString();
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    // Helper methods
    private void logDebug(String message, Level level) {
        if (!debugEnabled) return;

        // Only log SEVERE and WARNING messages to reduce console spam
        // INFO level debug messages are suppressed unless explicitly needed
        String prefix = "🤖 [Discord] ";
        switch (level.getName()) {
            case "SEVERE":
                plugin.getLogger().severe(prefix + message);
                break;
            case "WARNING":
                plugin.getLogger().warning(prefix + message);
                break;
            default:
                // Suppress INFO level debug messages to reduce console spam
                break;
        }
    }

    private boolean isStatsEnabled() {
        FileConfiguration statsConfig = plugin.getAddonsManager().getAddonConfig("stats");
        return statsConfig != null && statsConfig.getBoolean("enabled", false);
    }

    // Getters
    public boolean isEnabled() { return enabled; }
    public boolean isConnected() { return connected; }
    public String getBotToken() { return botToken; }
    public String getGuildId() { return guildId; }
    public String getLogsChannelId() { return logsChannelId; }
    public ExecutorService getDiscordExecutor() { return discordExecutor; }
    public HttpClient getHttpClient() { return httpClient; }
    public DiscordStatsService getStatsService() { return statsService; }
    public FileConfiguration getDiscordConfig() { return discordConfig; }
}