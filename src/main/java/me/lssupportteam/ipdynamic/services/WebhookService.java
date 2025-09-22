package me.lssupportteam.ipdynamic.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.lssupportteam.ipdynamic.IPDynamic;
import me.lssupportteam.ipdynamic.models.BanEntry;
import me.lssupportteam.ipdynamic.models.GeoLocation;
import me.lssupportteam.ipdynamic.models.PlayerData;
import me.lssupportteam.ipdynamic.utils.ColorUtils;

import javax.net.ssl.HttpsURLConnection;
import java.awt.Color;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class WebhookService {

    private final IPDynamic plugin;
    private final SimpleDateFormat dateFormat;


    private static final int COLOR_SUCCESS = 0x00FF00;
    private static final int COLOR_WARNING = 0xFFAA00;
    private static final int COLOR_DANGER = 0xFF0000;
    private static final int COLOR_INFO = 0x00AAFF;
    private static final int COLOR_PREMIUM = 0x9B59B6;
    private static final int COLOR_ALT = 0xE74C3C;
    private static final int COLOR_BAN = 0xE67E22;
    private static final int COLOR_UNBAN = 0x2ECC71;
    private static final int COLOR_ADMIN = 0xF39C12;

    public WebhookService(IPDynamic plugin) {
        this.plugin = plugin;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    }


    public void sendConnectionNotification(PlayerData playerData, String ip, boolean isFirstJoin) {
        if (!plugin.getWebhookConfigManager().isConnectionNotificationsEnabled()) return;

        String webhookUrl = plugin.getWebhookConfigManager().getConnectionWebhook();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject embed = new JsonObject();
                embed.addProperty("title", isFirstJoin ? "✨ Nueva Conexión" : "🔗 Conexión de Jugador");
                embed.addProperty("color", isFirstJoin ? COLOR_PREMIUM : COLOR_SUCCESS);

                JsonArray fields = new JsonArray();


                JsonObject playerField = new JsonObject();
                playerField.addProperty("name", "👤 Jugador");
                playerField.addProperty("value", String.format("`%s`", playerData.getUsername()));
                playerField.addProperty("inline", true);
                fields.add(playerField);


                JsonObject ipField = new JsonObject();
                ipField.addProperty("name", "🌐 IP");
                ipField.addProperty("value", String.format("`%s`", ip));
                ipField.addProperty("inline", true);
                fields.add(ipField);


                JsonObject connectionsField = new JsonObject();
                connectionsField.addProperty("name", "📊 Conexiones");
                connectionsField.addProperty("value", String.format("`%d`", playerData.getTotalConnections()));
                connectionsField.addProperty("inline", true);
                fields.add(connectionsField);


                if (playerData.getGeoLocation() != null) {
                    GeoLocation geo = playerData.getGeoLocation();
                    JsonObject geoField = new JsonObject();
                    geoField.addProperty("name", "📍 Ubicación");
                    geoField.addProperty("value", String.format("```%s```", geo.getFormattedLocation()));
                    geoField.addProperty("inline", false);


                    if (geo.isProxy()) {
                        JsonObject proxyField = new JsonObject();
                        proxyField.addProperty("name", "⚠️ Advertencia");
                        proxyField.addProperty("value", "**Proxy/VPN detectado**");
                        proxyField.addProperty("inline", false);
                        fields.add(proxyField);
                    }

                    fields.add(geoField);
                }


                if (playerData.hasMultipleIps()) {
                    JsonObject historyField = new JsonObject();
                    historyField.addProperty("name", "📜 Historial de IPs");

                    StringBuilder ips = new StringBuilder("```");
                    for (String historicIp : playerData.getIpHistory()) {
                        long firstSeen = playerData.getIpFirstSeen(historicIp);
                        ips.append(String.format("%s - %s\n", historicIp,
                            dateFormat.format(new Date(firstSeen))));
                    }
                    ips.append("```");

                    historyField.addProperty("value", ips.toString());
                    historyField.addProperty("inline", false);
                    fields.add(historyField);
                }


                JsonObject uuidField = new JsonObject();
                uuidField.addProperty("name", "🔑 UUID");
                uuidField.addProperty("value", String.format("`%s`", playerData.getUuid().toString()));
                uuidField.addProperty("inline", false);
                fields.add(uuidField);

                embed.add("fields", fields);


                JsonObject footer = new JsonObject();
                footer.addProperty("text", "IPDynamic 2.5-OMEGA");
                footer.addProperty("icon_url", "https://mc-heads.net/avatar/" + playerData.getUuid() + "/100");
                embed.add("footer", footer);

                embed.addProperty("timestamp", new Date().toInstant().toString());


                JsonObject thumbnail = new JsonObject();
                thumbnail.addProperty("url", "https://mc-heads.net/body/" + playerData.getUuid() + "/100");
                embed.add("thumbnail", thumbnail);

                sendWebhook(webhookUrl, embed);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error enviando webhook de conexión", e);
            }
        }, plugin.getExecutorService());
    }


    public void sendAltDetectionNotification(PlayerData mainAccount, List<PlayerData> alts) {
        if (!plugin.getWebhookConfigManager().isAltNotificationsEnabled()) return;

        String webhookUrl = plugin.getWebhookConfigManager().getAltWebhook();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject embed = new JsonObject();
                embed.addProperty("title", "🔍 Posibles Cuentas Alternativas Detectadas");
                embed.addProperty("color", COLOR_ALT);
                embed.addProperty("description", String.format(
                    "Se han detectado **%d** posibles cuentas alternativas para **%s**",
                    alts.size(), mainAccount.getUsername()
                ));

                JsonArray fields = new JsonArray();


                JsonObject mainField = new JsonObject();
                mainField.addProperty("name", "🎯 Cuenta Principal");
                mainField.addProperty("value", String.format(
                    "**Jugador:** `%s`\n**UUID:** `%s`\n**IP Actual:** `%s`",
                    mainAccount.getUsername(),
                    mainAccount.getUuid().toString().substring(0, 8) + "...",
                    mainAccount.getLastIp()
                ));
                mainField.addProperty("inline", false);
                fields.add(mainField);


                int altCount = 0;
                StringBuilder altList = new StringBuilder("```\n");
                for (PlayerData alt : alts) {
                    if (altCount >= 10) {
                        altList.append(String.format("... y %d más\n", alts.size() - 10));
                        break;
                    }

                    altList.append(String.format("• %s - Última vez: %s\n",
                        alt.getUsername(),
                        dateFormat.format(new Date(alt.getLastLogin()))
                    ));
                    altCount++;
                }
                altList.append("```");

                JsonObject altsField = new JsonObject();
                altsField.addProperty("name", "👥 Cuentas Alternativas");
                altsField.addProperty("value", altList.toString());
                altsField.addProperty("inline", false);
                fields.add(altsField);


                JsonObject sharedIpsField = new JsonObject();
                sharedIpsField.addProperty("name", "🌐 IPs Compartidas");
                StringBuilder sharedIps = new StringBuilder("```\n");

                for (String ip : mainAccount.getIpHistory()) {
                    int accountsWithIp = plugin.getDataManager().findAltsByIp(ip).size();
                    if (accountsWithIp > 1) {
                        sharedIps.append(String.format("%s - %d cuentas\n", ip, accountsWithIp));
                    }
                }
                sharedIps.append("```");

                sharedIpsField.addProperty("value", sharedIps.toString());
                sharedIpsField.addProperty("inline", false);
                fields.add(sharedIpsField);

                embed.add("fields", fields);


                JsonObject footer = new JsonObject();
                footer.addProperty("text", "IPDynamic Alt Detection System");
                footer.addProperty("icon_url", "https://mc-heads.net/avatar/" + mainAccount.getUuid() + "/100");
                embed.add("footer", footer);

                embed.addProperty("timestamp", new Date().toInstant().toString());

                sendWebhook(webhookUrl, embed);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error enviando webhook de detección de alts", e);
            }
        }, plugin.getExecutorService());
    }


    public void sendBanNotification(String pattern, String reason, String bannedBy, BanEntry.BanType type, int affectedIps) {
        if (!plugin.getWebhookConfigManager().isBanNotificationsEnabled()) return;

        String webhookUrl = plugin.getWebhookConfigManager().getBanWebhook();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject embed = new JsonObject();
                embed.addProperty("title", "⛔ Ban de IP Aplicado");
                embed.addProperty("color", COLOR_BAN);

                String description = String.format(
                    "Se ha aplicado un ban de tipo **%s** afectando a **%d** direcciones IP",
                    type.getType().toUpperCase(), affectedIps
                );
                embed.addProperty("description", description);

                JsonArray fields = new JsonArray();


                JsonObject patternField = new JsonObject();
                patternField.addProperty("name", "🎯 Patrón/IP");
                patternField.addProperty("value", String.format("`%s`", pattern));
                patternField.addProperty("inline", true);
                fields.add(patternField);


                JsonObject typeField = new JsonObject();
                typeField.addProperty("name", "📋 Tipo");
                typeField.addProperty("value", String.format("`%s`", type.getType().toUpperCase()));
                typeField.addProperty("inline", true);
                fields.add(typeField);


                JsonObject affectedField = new JsonObject();
                affectedField.addProperty("name", "📊 IPs Afectadas");
                affectedField.addProperty("value", String.format("`%,d`", affectedIps));
                affectedField.addProperty("inline", true);
                fields.add(affectedField);


                JsonObject reasonField = new JsonObject();
                reasonField.addProperty("name", "📝 Razón");
                reasonField.addProperty("value", String.format("```%s```", reason));
                reasonField.addProperty("inline", false);
                fields.add(reasonField);


                JsonObject bannedByField = new JsonObject();
                bannedByField.addProperty("name", "👮 Baneado por");
                bannedByField.addProperty("value", String.format("`%s`", bannedBy));
                bannedByField.addProperty("inline", true);
                fields.add(bannedByField);


                JsonObject dateField = new JsonObject();
                dateField.addProperty("name", "📅 Fecha");
                dateField.addProperty("value", String.format("`%s`", dateFormat.format(new Date())));
                dateField.addProperty("inline", true);
                fields.add(dateField);


                if (type == BanEntry.BanType.OP2 || type == BanEntry.BanType.OP3) {
                    JsonObject warningField = new JsonObject();
                    warningField.addProperty("name", "⚠️ Advertencia");
                    warningField.addProperty("value",
                        "**Ban masivo en proceso**\nEste ban se aplicará progresivamente para evitar lag del servidor.");
                    warningField.addProperty("inline", false);
                    fields.add(warningField);
                }

                embed.add("fields", fields);


                JsonObject footer = new JsonObject();
                footer.addProperty("text", "IPDynamic Ban System");
                embed.add("footer", footer);

                embed.addProperty("timestamp", new Date().toInstant().toString());

                sendWebhook(webhookUrl, embed);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error enviando webhook de ban", e);
            }
        }, plugin.getExecutorService());
    }


    public void sendUnbanNotification(String pattern, String unbannedBy, BanEntry.BanType type, int affectedIps) {
        if (!plugin.getWebhookConfigManager().isBanNotificationsEnabled()) return;

        String webhookUrl = plugin.getWebhookConfigManager().getBanWebhook();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject embed = new JsonObject();
                embed.addProperty("title", "✅ Unban de IP Aplicado");
                embed.addProperty("color", COLOR_UNBAN);

                JsonArray fields = new JsonArray();


                JsonObject patternField = new JsonObject();
                patternField.addProperty("name", "🎯 Patrón/IP");
                patternField.addProperty("value", String.format("`%s`", pattern));
                patternField.addProperty("inline", true);
                fields.add(patternField);


                JsonObject typeField = new JsonObject();
                typeField.addProperty("name", "📋 Tipo");
                typeField.addProperty("value", String.format("`%s`", type.getType().toUpperCase()));
                typeField.addProperty("inline", true);
                fields.add(typeField);


                JsonObject affectedField = new JsonObject();
                affectedField.addProperty("name", "📊 IPs Liberadas");
                affectedField.addProperty("value", String.format("`%,d`", affectedIps));
                affectedField.addProperty("inline", true);
                fields.add(affectedField);


                JsonObject unbannedByField = new JsonObject();
                unbannedByField.addProperty("name", "👤 Desbaneado por");
                unbannedByField.addProperty("value", String.format("`%s`", unbannedBy));
                unbannedByField.addProperty("inline", true);
                fields.add(unbannedByField);


                JsonObject dateField = new JsonObject();
                dateField.addProperty("name", "📅 Fecha");
                dateField.addProperty("value", String.format("`%s`", dateFormat.format(new Date())));
                dateField.addProperty("inline", true);
                fields.add(dateField);

                embed.add("fields", fields);


                JsonObject footer = new JsonObject();
                footer.addProperty("text", "IPDynamic Unban System");
                embed.add("footer", footer);

                embed.addProperty("timestamp", new Date().toInstant().toString());

                sendWebhook(webhookUrl, embed);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error enviando webhook de unban", e);
            }
        }, plugin.getExecutorService());
    }


    public void sendAdminLoginNotification(PlayerData adminData, String ip) {
        if (!plugin.getWebhookConfigManager().isAdminNotificationsEnabled()) return;

        String webhookUrl = plugin.getWebhookConfigManager().getAdminWebhook();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject embed = new JsonObject();
                embed.addProperty("title", "👑 Conexión de Administrador");
                embed.addProperty("color", COLOR_ADMIN);
                embed.addProperty("description", "Un administrador ha iniciado sesión en el servidor");

                JsonArray fields = new JsonArray();


                JsonObject adminField = new JsonObject();
                adminField.addProperty("name", "🛡️ Administrador");
                adminField.addProperty("value", String.format(
                    "**Nombre:** `%s`\n**UUID:** `%s`",
                    adminData.getUsername(),
                    adminData.getUuid().toString()
                ));
                adminField.addProperty("inline", false);
                fields.add(adminField);


                JsonObject ipField = new JsonObject();
                ipField.addProperty("name", "🌐 Conexión");
                ipField.addProperty("value", String.format("**IP:** `%s`", ip));

                if (adminData.getGeoLocation() != null) {
                    ipField.addProperty("value", String.format(
                        "**IP:** `%s`\n**Ubicación:** `%s`",
                        ip, adminData.getGeoLocation().getFormattedLocation()
                    ));
                }
                ipField.addProperty("inline", false);
                fields.add(ipField);


                JsonObject timeField = new JsonObject();
                timeField.addProperty("name", "⏰ Hora de Conexión");
                timeField.addProperty("value", String.format("`%s`", dateFormat.format(new Date())));
                timeField.addProperty("inline", true);
                fields.add(timeField);


                JsonObject connectionsField = new JsonObject();
                connectionsField.addProperty("name", "📊 Conexiones Totales");
                connectionsField.addProperty("value", String.format("`%d`", adminData.getTotalConnections()));
                connectionsField.addProperty("inline", true);
                fields.add(connectionsField);

                embed.add("fields", fields);


                JsonObject footer = new JsonObject();
                footer.addProperty("text", "IPDynamic Admin Monitor");
                footer.addProperty("icon_url", "https://mc-heads.net/avatar/" + adminData.getUuid() + "/100");
                embed.add("footer", footer);

                embed.addProperty("timestamp", new Date().toInstant().toString());


                JsonObject thumbnail = new JsonObject();
                thumbnail.addProperty("url", "https://mc-heads.net/head/" + adminData.getUuid() + "/100");
                embed.add("thumbnail", thumbnail);

                sendWebhook(webhookUrl, embed);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error enviando webhook de admin", e);
            }
        }, plugin.getExecutorService());
    }


    private void sendWebhook(String webhookUrl, JsonObject embed) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("username", "IPDynamic 2.5-OMEGA");
            payload.addProperty("avatar_url", "https://mc-heads.net/avatar/MHF_Exclamation/100");

            JsonArray embeds = new JsonArray();
            embeds.add(embed);
            payload.add("embeds", embeds);

            URL url = new URL(webhookUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "IPDynamic/2.5-OMEGA");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204 && responseCode != 200) {
                plugin.getLogger().warning("Webhook respondió con código: " + responseCode);
            }

            connection.disconnect();

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error enviando webhook", e);
        }
    }


    public void sendCustomNotification(String title, String description, int color, JsonArray fields) {
        String webhookUrl = plugin.getWebhookConfigManager().getCustomWebhook();
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject embed = new JsonObject();
                embed.addProperty("title", title);
                embed.addProperty("description", description);
                embed.addProperty("color", color);

                if (fields != null && fields.size() > 0) {
                    embed.add("fields", fields);
                }

                JsonObject footer = new JsonObject();
                footer.addProperty("text", "IPDynamic 2.5-OMEGA");
                embed.add("footer", footer);

                embed.addProperty("timestamp", new Date().toInstant().toString());

                sendWebhook(webhookUrl, embed);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error enviando webhook personalizado", e);
            }
        }, plugin.getExecutorService());
    }
}