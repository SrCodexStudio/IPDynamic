package me.lssupportteam.ipdynamic.nms;

import org.bukkit.Bukkit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NMSVersion {

    private static final Pattern VERSION_PATTERN = Pattern.compile("\\(MC:\\s*([\\d.]+)\\)");
    private final String serverVersion;
    private final String nmsVersion;
    private final int majorVersion;
    private final int minorVersion;
    private final int patchVersion;
    private final ServerType serverType;

    public enum ServerType {
        SPIGOT("Spigot"),
        PAPER("Paper"),
        PURPUR("Purpur"),
        TUINITY("Tuinity"),
        AIRPLANE("Airplane"),
        PUFFERFISH("Pufferfish"),
        UNKNOWN("Unknown");

        private final String name;

        ServerType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public NMSVersion() {
        this.serverVersion = Bukkit.getVersion();
        this.nmsVersion = extractNMSVersion();

        String[] versionParts = extractMinecraftVersion().split("\\.");
        this.majorVersion = versionParts.length > 0 ? parseVersion(versionParts[0]) : 1;
        this.minorVersion = versionParts.length > 1 ? parseVersion(versionParts[1]) : 0;
        this.patchVersion = versionParts.length > 2 ? parseVersion(versionParts[2]) : 0;
        this.serverType = detectServerType();
    }

    private String extractNMSVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String[] parts = packageName.split("\\.");
            if (parts.length > 3) {
                return parts[3];
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    private String extractMinecraftVersion() {
        Matcher matcher = VERSION_PATTERN.matcher(serverVersion);
        if (matcher.find()) {
            return matcher.group(1);
        }


        if (nmsVersion.startsWith("v")) {
            String[] parts = nmsVersion.split("_");
            if (parts.length >= 3) {
                return parts[0].substring(1) + "." + parts[1] + "." + parts[2].replace("R", "");
            }
        }

        return "1.13.0";
    }

    private int parseVersion(String version) {
        try {
            return Integer.parseInt(version.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private ServerType detectServerType() {
        String version = serverVersion.toLowerCase();

        if (version.contains("paper")) {
            return ServerType.PAPER;
        } else if (version.contains("purpur")) {
            return ServerType.PURPUR;
        } else if (version.contains("tuinity")) {
            return ServerType.TUINITY;
        } else if (version.contains("airplane")) {
            return ServerType.AIRPLANE;
        } else if (version.contains("pufferfish")) {
            return ServerType.PUFFERFISH;
        } else if (version.contains("spigot")) {
            return ServerType.SPIGOT;
        }


        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return ServerType.PAPER;
        } catch (ClassNotFoundException ignored) {}

        try {
            Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".CraftServer");
            return ServerType.SPIGOT;
        } catch (ClassNotFoundException ignored) {}

        return ServerType.UNKNOWN;
    }

    public boolean isVersionSupported() {

        if (majorVersion != 1) return false;
        if (minorVersion < 13) return false;
        if (minorVersion > 21) return false;
        if (minorVersion == 21 && patchVersion > 8) return false;
        return true;
    }

    public boolean isModernVersion() {

        return majorVersion == 1 && minorVersion >= 16;
    }

    public boolean isLegacyVersion() {

        return majorVersion == 1 && minorVersion >= 13 && minorVersion <= 15;
    }

    public boolean isPaperBased() {
        return serverType == ServerType.PAPER ||
               serverType == ServerType.PURPUR ||
               serverType == ServerType.TUINITY ||
               serverType == ServerType.AIRPLANE ||
               serverType == ServerType.PUFFERFISH;
    }

    public String getVersionString() {
        return majorVersion + "." + minorVersion + "." + patchVersion;
    }

    public String getFullVersionInfo() {
        return String.format("%s %s (NMS: %s)", serverType.getName(), getVersionString(), nmsVersion);
    }

    public boolean isAtLeast(int major, int minor) {
        if (majorVersion > major) return true;
        if (majorVersion < major) return false;
        return minorVersion >= minor;
    }

    public boolean isAtLeast(int major, int minor, int patch) {
        if (majorVersion > major) return true;
        if (majorVersion < major) return false;
        if (minorVersion > minor) return true;
        if (minorVersion < minor) return false;
        return patchVersion >= patch;
    }


    public String getServerVersion() { return serverVersion; }
    public String getNmsVersion() { return nmsVersion; }
    public int getMajorVersion() { return majorVersion; }
    public int getMinorVersion() { return minorVersion; }
    public int getPatchVersion() { return patchVersion; }
    public ServerType getServerType() { return serverType; }
}