package me.lssupportteam.ipdynamic.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.lssupportteam.ipdynamic.IPDynamic;
import me.lssupportteam.ipdynamic.models.GeoLocation;
import me.lssupportteam.ipdynamic.utils.IPUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class GeoIPService {

    private final IPDynamic plugin;
    private final ConcurrentHashMap<String, CacheEntry> locationCache;
    private final String apiUrl;
    private final int cacheDuration;


    private static final int MAX_REQUESTS_PER_MINUTE = 45; // IP-API.com permite 45/min
    private int requestCount = 0;
    private long lastRequestReset = System.currentTimeMillis();

    public GeoIPService(IPDynamic plugin) {
        this.plugin = plugin;
        this.locationCache = new ConcurrentHashMap<>();
        this.apiUrl = "http://ip-api.com/json/";
        this.cacheDuration = plugin.getConfigManager().getGeoIpCacheDuration() * 1000; // Convertir a ms
    }

    public CompletableFuture<GeoLocation> getLocation(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            try {

                if (!IPUtils.isValidIpAddress(ip) || IPUtils.isLocalIp(ip)) {
                    return null;
                }


                CacheEntry cached = locationCache.get(ip);
                if (cached != null && !cached.isExpired()) {
                    return cached.location;
                }


                if (!checkRateLimit()) {
                    plugin.getLogger().warning("Rate limit alcanzado para GeoIP. Intentando más tarde...");
                    return null;
                }


                GeoLocation location = fetchLocationFromAPI(ip);
                if (location != null) {

                    locationCache.put(ip, new CacheEntry(location, System.currentTimeMillis() + cacheDuration));
                }

                return location;

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error obteniendo geolocalización para " + ip, e);
                return null;
            }
        }, plugin.getExecutorService());
    }

    private boolean checkRateLimit() {
        long currentTime = System.currentTimeMillis();


        if (currentTime - lastRequestReset > 60000) {
            requestCount = 0;
            lastRequestReset = currentTime;
        }

        if (requestCount >= MAX_REQUESTS_PER_MINUTE) {
            return false;
        }

        requestCount++;
        return true;
    }

    private GeoLocation fetchLocationFromAPI(String ip) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(apiUrl + ip + "?fields=status,message,country,countryCode,region,regionName,city,zip,lat,lon,timezone,isp,org,as,proxy,hosting,mobile,query");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 segundos
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "IPDynamic/2.5-OMEGA");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                plugin.getLogger().warning("GeoIP API respondió con código: " + responseCode);
                return null;
            }


            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();


                String status = json.has("status") ? json.get("status").getAsString() : "";
                if (!"success".equals(status)) {
                    String message = json.has("message") ? json.get("message").getAsString() : "Unknown error";
                    plugin.getLogger().warning("GeoIP API error: " + message);
                    return null;
                }


                GeoLocation location = new GeoLocation();

                if (json.has("country") && !json.get("country").isJsonNull()) {
                    location.setCountry(json.get("country").getAsString());
                }

                if (json.has("countryCode") && !json.get("countryCode").isJsonNull()) {
                    location.setCountryCode(json.get("countryCode").getAsString());
                }

                if (json.has("region") && !json.get("region").isJsonNull()) {
                    location.setRegion(json.get("region").getAsString());
                }

                if (json.has("regionName") && !json.get("regionName").isJsonNull()) {
                    location.setRegionName(json.get("regionName").getAsString());
                }

                if (json.has("city") && !json.get("city").isJsonNull()) {
                    location.setCity(json.get("city").getAsString());
                }

                if (json.has("zip") && !json.get("zip").isJsonNull()) {
                    location.setZip(json.get("zip").getAsString());
                }

                if (json.has("lat") && !json.get("lat").isJsonNull()) {
                    location.setLatitude(json.get("lat").getAsDouble());
                }

                if (json.has("lon") && !json.get("lon").isJsonNull()) {
                    location.setLongitude(json.get("lon").getAsDouble());
                }

                if (json.has("timezone") && !json.get("timezone").isJsonNull()) {
                    location.setTimezone(json.get("timezone").getAsString());
                }

                if (json.has("isp") && !json.get("isp").isJsonNull()) {
                    location.setIsp(json.get("isp").getAsString());
                }

                if (json.has("org") && !json.get("org").isJsonNull()) {
                    location.setOrg(json.get("org").getAsString());
                }

                if (json.has("as") && !json.get("as").isJsonNull()) {
                    location.setAs(json.get("as").getAsString());
                }

                if (json.has("proxy") && !json.get("proxy").isJsonNull()) {
                    location.setProxy(json.get("proxy").getAsBoolean());
                }

                if (json.has("hosting") && !json.get("hosting").isJsonNull()) {
                    location.setHosting(json.get("hosting").getAsBoolean());
                }

                if (json.has("mobile") && !json.get("mobile").isJsonNull()) {
                    location.setMobile(json.get("mobile").getAsBoolean());
                }

                if (json.has("query") && !json.get("query").isJsonNull()) {
                    location.setQuery(json.get("query").getAsString());
                }

                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info(String.format(
                        "GeoIP obtenido para %s: %s, %s",
                        ip, location.getCity(), location.getCountry()
                    ));
                }

                return location;
            }

        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error de conexión con GeoIP API", e);
            return null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parseando respuesta GeoIP", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void clearCache() {
        locationCache.clear();
        plugin.getLogger().info(plugin.getLangManager().getMessage("log.geoip-cache-cleared"));
    }

    public void clearExpiredCache() {
        long currentTime = System.currentTimeMillis();
        locationCache.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));
    }

    public int getCacheSize() {
        return locationCache.size();
    }

    public int getRequestCount() {
        return requestCount;
    }

    public boolean isRateLimited() {
        return requestCount >= MAX_REQUESTS_PER_MINUTE;
    }


    private static class CacheEntry {
        final GeoLocation location;
        final long expirationTime;

        CacheEntry(GeoLocation location, long expirationTime) {
            this.location = location;
            this.expirationTime = expirationTime;
        }

        boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        boolean isExpired(long currentTime) {
            return currentTime > expirationTime;
        }
    }
}