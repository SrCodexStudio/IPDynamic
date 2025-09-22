package me.lssupportteam.ipdynamic.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.lssupportteam.ipdynamic.IPDynamic;
import me.lssupportteam.ipdynamic.models.GeoLocation;
import me.lssupportteam.ipdynamic.models.PlayerData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class DiscordStatsService {

    private final IPDynamic plugin;
    private final DiscordManager discordManager;
    private final DecimalFormat decimalFormat;

    private static final String DISCORD_API_BASE = "https://discord.com/api/v10";
    private static final Map<String, String> COUNTRY_FLAGS = new HashMap<>();
    private static final Map<String, Integer> COUNTRY_COLORS = new HashMap<>();

    static {
        initializeCountryFlags();
        initializeCountryColors();
    }

    public DiscordStatsService(IPDynamic plugin, DiscordManager discordManager) {
        this.plugin = plugin;
        this.discordManager = discordManager;
        this.decimalFormat = new DecimalFormat("#,###");
    }

    public String sendCountryStatsEmbed() {
        try {
            org.bukkit.configuration.file.FileConfiguration statsConfig = plugin.getAddonsManager().getAddonConfig("stats");
            if (statsConfig == null || !statsConfig.getBoolean("enabled", false)) {
                return null;
            }

            Map<String, CountryStats> countryStats = calculateCountryStats();
            int limit = statsConfig.getInt("top-countries.limit", 10);
            List<CountryStats> topCountries = getTopCountries(countryStats, limit);

            JsonObject embed = createCountryStatsEmbed(topCountries, statsConfig);
            String channelId = statsConfig.getString("auto-send.channel-id", "");
            if (channelId.isEmpty() || channelId.equals("YOUR_STATS_CHANNEL_ID")) {
                plugin.getLogger().warning("❌ Channel ID de estadísticas no configurado en addons/stats.yml");
                return null;
            }
            String messageId = sendEmbedToChannel(embed, channelId);

            plugin.getLogger().info("✅ Estadísticas de países enviadas a Discord");
            return messageId;

        } catch (Exception e) {
            plugin.getLogger().severe("Error generando estadísticas de países: " + e.getMessage());
            if (plugin.getConfigManager().isDebugMode()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public void editCountryStatsEmbed(String messageId) {
        try {
            org.bukkit.configuration.file.FileConfiguration statsConfig = plugin.getAddonsManager().getAddonConfig("stats");
            if (statsConfig == null || !statsConfig.getBoolean("enabled", false)) {
                return;
            }

            Map<String, CountryStats> countryStats = calculateCountryStats();
            int limit = statsConfig.getInt("top-countries.limit", 10);
            List<CountryStats> topCountries = getTopCountries(countryStats, limit);

            JsonObject embed = createCountryStatsEmbed(topCountries, statsConfig);
            String channelId = statsConfig.getString("auto-send.channel-id", "");
            if (channelId.isEmpty() || channelId.equals("YOUR_STATS_CHANNEL_ID")) {
                plugin.getLogger().warning("❌ Channel ID de estadísticas no configurado en addons/stats.yml");
                return;
            }
            editMessageEmbed(messageId, embed, channelId);

            // Statistics updated silently to reduce console spam

        } catch (Exception e) {
            plugin.getLogger().severe("Error actualizando estadísticas de países: " + e.getMessage());
            if (plugin.getConfigManager().isDebugMode()) {
                e.printStackTrace();
            }
        }
    }

    private Map<String, CountryStats> calculateCountryStats() {
        Map<String, CountryStats> countryStatsMap = new HashMap<>();

        Collection<PlayerData> allPlayers = plugin.getDataManager().getAllPlayers();
        int totalConnections = 0;

        for (PlayerData player : allPlayers) {
            GeoLocation geoLocation = player.getGeoLocation();
            if (geoLocation != null && geoLocation.getCountry() != null) {
                String country = geoLocation.getCountry();
                int playerConnections = player.getTotalConnections();
                totalConnections += playerConnections;

                countryStatsMap.computeIfAbsent(country, k -> new CountryStats(country))
                    .addPlayer(playerConnections);
            }
        }

        for (CountryStats stats : countryStatsMap.values()) {
            stats.calculatePercentage(totalConnections);
        }

        return countryStatsMap;
    }

    private List<CountryStats> getTopCountries(Map<String, CountryStats> countryStats, int limit) {
        return countryStats.values().stream()
            .sorted((a, b) -> Integer.compare(b.getTotalConnections(), a.getTotalConnections()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    private JsonObject createCountryStatsEmbed(List<CountryStats> topCountries, org.bukkit.configuration.file.FileConfiguration statsConfig) {
        JsonObject embed = new JsonObject();

        // Use configuration for embed properties
        int limit = statsConfig.getInt("top-countries.limit", 10);
        String title = statsConfig.getString("embed.title", "🌍 TOP {limit} Países por Conexiones")
            .replace("{limit}", String.valueOf(limit));
        String description = statsConfig.getString("embed.description", "");
        String colorHex = statsConfig.getString("embed.color", "#00FF00");
        String thumbnailUrl = statsConfig.getString("embed.thumbnail", "");

        embed.addProperty("title", title);

        // Only add description if not empty
        if (!description.isEmpty()) {
            embed.addProperty("description", description);
        }

        embed.addProperty("color", hexToDecimal(colorHex));

        // Add timestamp if enabled
        if (statsConfig.getBoolean("embed.timestamp", true)) {
            embed.addProperty("timestamp", Instant.now().toString());
        }

        // Add thumbnail if configured
        if (!thumbnailUrl.isEmpty()) {
            JsonObject thumbnail = new JsonObject();
            thumbnail.addProperty("url", thumbnailUrl);
            embed.add("thumbnail", thumbnail);
        }

        // Add author if configured
        String authorName = statsConfig.getString("embed.author.name", "");
        String authorUrl = statsConfig.getString("embed.author.url", "");
        String authorIcon = statsConfig.getString("embed.author.icon", "");
        if (!authorName.isEmpty()) {
            JsonObject author = new JsonObject();
            author.addProperty("name", authorName);
            if (!authorUrl.isEmpty()) author.addProperty("url", authorUrl);
            if (!authorIcon.isEmpty()) author.addProperty("icon_url", authorIcon);
            embed.add("author", author);
        }

        // Add image if configured
        String imageUrl = statsConfig.getString("embed.image", "");
        if (!imageUrl.isEmpty()) {
            JsonObject image = new JsonObject();
            image.addProperty("url", imageUrl);
            embed.add("image", image);
        }

        // Build fields
        JsonArray fields = new JsonArray();
        boolean showFlags = statsConfig.getBoolean("top-countries.show-flags", true);
        boolean showPercentages = statsConfig.getBoolean("top-countries.show-percentages", true);
        String entryTemplate = statsConfig.getString("templates.country-entry",
            "{medal} **{country}** {flag}\n`{connections:,} conexiones` ({percentage}%)");

        // Calculate total connections for percentages
        int totalConnections = topCountries.stream()
            .mapToInt(CountryStats::getTotalConnections)
            .sum();

        if (topCountries.isEmpty()) {
            JsonObject field = new JsonObject();
            field.addProperty("name", "📊 Estadísticas");
            field.addProperty("value", statsConfig.getString("templates.no-data", "❌ No hay datos disponibles"));
            field.addProperty("inline", false);
            fields.add(field);
        } else {
            // Create country list
            StringBuilder countryList = new StringBuilder();

            // Get separator from config
            String separator = statsConfig.getString("embed.content.separator", "");

            // Add country entries
            for (int i = 0; i < topCountries.size(); i++) {
                CountryStats stats = topCountries.get(i);
                String flag = showFlags ? COUNTRY_FLAGS.getOrDefault(stats.getCountry(), "🏳️") : "";
                String medal = getConfiguredMedal(i + 1, statsConfig);
                double percentage = totalConnections > 0 ? (stats.getTotalConnections() * 100.0 / totalConnections) : 0;

                String entryText = entryTemplate
                    .replace("{medal}", medal)
                    .replace("{country}", stats.getCountry())
                    .replace("{flag}", flag)
                    .replace("{connections:,}", decimalFormat.format(stats.getTotalConnections()))
                    .replace("{connections}", String.valueOf(stats.getTotalConnections()))
                    .replace("{percentage}", showPercentages ? String.format("%.1f", percentage) : "N/A");

                countryList.append(entryText);

                // Add separator if configured (except for last item)
                if (i < topCountries.size() - 1) {
                    countryList.append("\n");
                    if (!separator.isEmpty()) {
                        countryList.append(separator).append("\n");
                    }
                }
            }

            // Add the complete list as a single field
            JsonObject field = new JsonObject();
            field.addProperty("name", "\u200B"); // Invisible character for clean look
            field.addProperty("value", countryList.toString());
            field.addProperty("inline", false);
            fields.add(field);
        }

        embed.add("fields", fields);

        // Footer
        String footerText = statsConfig.getString("embed.footer.text", "IPDynamic 2.5-OMEGA • Actualizado");
        String footerIcon = statsConfig.getString("embed.footer.icon", "");

        if (!footerText.isEmpty()) {
            JsonObject footer = new JsonObject();
            footer.addProperty("text", footerText);
            if (!footerIcon.isEmpty()) {
                footer.addProperty("icon_url", footerIcon);
            }
            embed.add("footer", footer);
        }

        return embed;
    }

    private String getConfiguredMedal(int position, org.bukkit.configuration.file.FileConfiguration statsConfig) {
        switch (position) {
            case 1:
                return statsConfig.getString("medals.first", "🥇");
            case 2:
                return statsConfig.getString("medals.second", "🥈");
            case 3:
                return statsConfig.getString("medals.third", "🥉");
            default:
                return statsConfig.getString("medals.default", "🔹");
        }
    }

    private int hexToDecimal(String hex) {
        try {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return 0x00FF00; // Default green
        }
    }

    private String sendEmbedToChannel(JsonObject embed, String channelId) throws IOException {
        try {
            String url = DISCORD_API_BASE + "/channels/" + channelId + "/messages";

            JsonObject payload = new JsonObject();
            JsonArray embeds = new JsonArray();
            embeds.add(embed);
            payload.add("embeds", embeds);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bot " + discordManager.getBotToken())
                .header("Content-Type", "application/json")
                .header("User-Agent", "IPDynamic/2.5-OMEGA")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

            HttpClient httpClient = discordManager.getHttpClient();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Discord API respondió con código: " + response.statusCode() + " - " + response.body());
            }

            // Parse response to get message ID
            JsonObject responseJson = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
            return responseJson.get("id").getAsString();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Operación interrumpida: " + e.getMessage());
        } catch (Exception e) {
            throw new IOException("Error enviando mensaje a Discord: " + e.getMessage());
        }
    }

    private void editMessageEmbed(String messageId, JsonObject embed, String channelId) throws IOException {
        try {
            String url = DISCORD_API_BASE + "/channels/" + channelId + "/messages/" + messageId;

            JsonObject payload = new JsonObject();
            JsonArray embeds = new JsonArray();
            embeds.add(embed);
            payload.add("embeds", embeds);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bot " + discordManager.getBotToken())
                .header("Content-Type", "application/json")
                .header("User-Agent", "IPDynamic/2.5-OMEGA")
                .timeout(Duration.ofSeconds(30))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

            HttpClient httpClient = discordManager.getHttpClient();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Discord API respondió con código: " + response.statusCode() + " - " + response.body());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Operación interrumpida: " + e.getMessage());
        } catch (Exception e) {
            throw new IOException("Error editando mensaje en Discord: " + e.getMessage());
        }
    }

    private String getMedal(int position) {
        switch (position) {
            case 1: return "\uD83E\uDD47";
            case 2: return "\uD83E\uDD48";
            case 3: return "\uD83E\uDD49";
            default: return "\uD83C\uDFC5";
        }
    }

    private int getRandomCountryColor() {
        List<Integer> colors = new ArrayList<>(COUNTRY_COLORS.values());
        return colors.get(new Random().nextInt(colors.size()));
    }

    private static void initializeCountryFlags() {
        COUNTRY_FLAGS.put("United States", "\uD83C\uDDFA\uD83C\uDDF8");
        COUNTRY_FLAGS.put("Brazil", "\uD83C\uDDE7\uD83C\uDDF7");
        COUNTRY_FLAGS.put("Mexico", "\uD83C\uDDF2\uD83C\uDDFD");
        COUNTRY_FLAGS.put("Argentina", "\uD83C\uDDE6\uD83C\uDDF7");
        COUNTRY_FLAGS.put("Spain", "\uD83C\uDDEA\uD83C\uDDF8");
        COUNTRY_FLAGS.put("Germany", "\uD83C\uDDE9\uD83C\uDDEA");
        COUNTRY_FLAGS.put("France", "\uD83C\uDDEB\uD83C\uDDF7");
        COUNTRY_FLAGS.put("United Kingdom", "\uD83C\uDDEC\uD83C\uDDE7");
        COUNTRY_FLAGS.put("Canada", "\uD83C\uDDE8\uD83C\uDDE6");
        COUNTRY_FLAGS.put("Australia", "\uD83C\uDDE6\uD83C\uDDFA");
        COUNTRY_FLAGS.put("Japan", "\uD83C\uDDEF\uD83C\uDDF5");
        COUNTRY_FLAGS.put("South Korea", "\uD83C\uDDF0\uD83C\uDDF7");
        COUNTRY_FLAGS.put("China", "\uD83C\uDDE8\uD83C\uDDF3");
        COUNTRY_FLAGS.put("India", "\uD83C\uDDEE\uD83C\uDDF3");
        COUNTRY_FLAGS.put("Russia", "\uD83C\uDDF7\uD83C\uDDFA");
        COUNTRY_FLAGS.put("Poland", "\uD83C\uDDF5\uD83C\uDDF1");
        COUNTRY_FLAGS.put("Netherlands", "\uD83C\uDDF3\uD83C\uDDF1");
        COUNTRY_FLAGS.put("Italy", "\uD83C\uDDEE\uD83C\uDDF9");
        COUNTRY_FLAGS.put("Sweden", "\uD83C\uDDF8\uD83C\uDDEA");
        COUNTRY_FLAGS.put("Norway", "\uD83C\uDDF3\uD83C\uDDF4");
    }

    private static void initializeCountryColors() {
        COUNTRY_COLORS.put("United States", 0x1F77B4);
        COUNTRY_COLORS.put("Brazil", 0x2ECC71);
        COUNTRY_COLORS.put("Mexico", 0xE74C3C);
        COUNTRY_COLORS.put("Argentina", 0x3498DB);
        COUNTRY_COLORS.put("Spain", 0xF39C12);
        COUNTRY_COLORS.put("Germany", 0x9B59B6);
        COUNTRY_COLORS.put("France", 0x1ABC9C);
        COUNTRY_COLORS.put("United Kingdom", 0xE67E22);
        COUNTRY_COLORS.put("Canada", 0x34495E);
        COUNTRY_COLORS.put("Australia", 0xF1C40F);
    }

    private static class CountryStats {
        private final String country;
        private int playerCount;
        private int totalConnections;
        private double percentage;

        public CountryStats(String country) {
            this.country = country;
            this.playerCount = 0;
            this.totalConnections = 0;
            this.percentage = 0.0;
        }

        public void addPlayer(int connections) {
            this.playerCount++;
            this.totalConnections += connections;
        }

        public void calculatePercentage(int totalServerConnections) {
            if (totalServerConnections > 0) {
                this.percentage = (double) totalConnections / totalServerConnections * 100.0;
            }
        }

        public double getAverageConnections() {
            return playerCount > 0 ? (double) totalConnections / playerCount : 0.0;
        }

        public String getCountry() { return country; }
        public int getPlayerCount() { return playerCount; }
        public int getTotalConnections() { return totalConnections; }
        public double getPercentage() { return percentage; }
    }
}