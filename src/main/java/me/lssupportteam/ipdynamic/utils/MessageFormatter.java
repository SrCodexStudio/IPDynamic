package me.lssupportteam.ipdynamic.utils;

public class MessageFormatter {

    public static String formatForDiscord(String minecraftMessage) {
        if (minecraftMessage == null) return "";

        String discordMessage = minecraftMessage
                .replaceAll("&[0-9a-fA-F]", "")
                .replaceAll("&#[0-9a-fA-F]{6}", "")
                .replaceAll("&[klmnor]", "")
                .replaceAll("&r", "")
                .replace("&l", "**")
                .replace("&o", "*")
                .replace("&n", "__")
                .replace("&m", "~~")
                .replaceAll("\\s+", " ")
                .trim();

        return discordMessage;
    }

    public static String formatForMinecraft(String message) {
        return ColorUtils.translateColor(message);
    }

    public static String addMinecraftColorsToConsole(String message) {
        return ColorUtils.translateColor(message);
    }
}