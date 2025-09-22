package me.lssupportteam.ipdynamic.utils;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern BUKKIT_COLOR_PATTERN = Pattern.compile("&([0-9a-fklmnor])");

    public static String translateColor(String message) {
        if (message == null) return "";


        message = translateHexColors(message);


        message = ChatColor.translateAlternateColorCodes('&', message);

        return message;
    }

    public static String translateHexColors(String message) {
        if (message == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            String replacement = "";

            try {

                replacement = net.md_5.bungee.api.ChatColor.of("#" + hexColor).toString();
            } catch (NoSuchMethodError | NoClassDefFoundError e) {

                replacement = getNearestBukkitColor(hexColor);
            }

            matcher.appendReplacement(buffer, replacement);
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String getNearestBukkitColor(String hexColor) {

        int r = Integer.parseInt(hexColor.substring(0, 2), 16);
        int g = Integer.parseInt(hexColor.substring(2, 4), 16);
        int b = Integer.parseInt(hexColor.substring(4, 6), 16);


        ChatColor nearestColor = ChatColor.WHITE;
        double minDistance = Double.MAX_VALUE;

        for (ChatColor color : ChatColor.values()) {
            if (color.isColor()) {
                double distance = getColorDistance(r, g, b, color);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestColor = color;
                }
            }
        }

        return nearestColor.toString();
    }

    private static double getColorDistance(int r, int g, int b, ChatColor color) {

        int[] rgb = getMinecraftColorRGB(color);
        return Math.sqrt(
            Math.pow(r - rgb[0], 2) +
            Math.pow(g - rgb[1], 2) +
            Math.pow(b - rgb[2], 2)
        );
    }

    private static int[] getMinecraftColorRGB(ChatColor color) {
        switch (color) {
            case BLACK: return new int[]{0, 0, 0};
            case DARK_BLUE: return new int[]{0, 0, 170};
            case DARK_GREEN: return new int[]{0, 170, 0};
            case DARK_AQUA: return new int[]{0, 170, 170};
            case DARK_RED: return new int[]{170, 0, 0};
            case DARK_PURPLE: return new int[]{170, 0, 170};
            case GOLD: return new int[]{255, 170, 0};
            case GRAY: return new int[]{170, 170, 170};
            case DARK_GRAY: return new int[]{85, 85, 85};
            case BLUE: return new int[]{85, 85, 255};
            case GREEN: return new int[]{85, 255, 85};
            case AQUA: return new int[]{85, 255, 255};
            case RED: return new int[]{255, 85, 85};
            case LIGHT_PURPLE: return new int[]{255, 85, 255};
            case YELLOW: return new int[]{255, 255, 85};
            case WHITE: return new int[]{255, 255, 255};
            default: return new int[]{255, 255, 255};
        }
    }

    public static String stripColor(String message) {
        if (message == null) return "";
        return ChatColor.stripColor(translateColor(message));
    }

    public static String gradient(String text, String startHex, String endHex) {
        if (text == null || text.isEmpty()) return "";

        int length = text.length();
        StringBuilder result = new StringBuilder();

        int startR = Integer.parseInt(startHex.substring(0, 2), 16);
        int startG = Integer.parseInt(startHex.substring(2, 4), 16);
        int startB = Integer.parseInt(startHex.substring(4, 6), 16);

        int endR = Integer.parseInt(endHex.substring(0, 2), 16);
        int endG = Integer.parseInt(endHex.substring(2, 4), 16);
        int endB = Integer.parseInt(endHex.substring(4, 6), 16);

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (length - 1);

            int r = Math.round(startR + ratio * (endR - startR));
            int g = Math.round(startG + ratio * (endG - startG));
            int b = Math.round(startB + ratio * (endB - startB));

            String hex = String.format("%02x%02x%02x", r, g, b);
            result.append("&#").append(hex).append(text.charAt(i));
        }

        return translateColor(result.toString());
    }

    public static String rainbow(String text) {
        if (text == null || text.isEmpty()) return "";

        String[] rainbowColors = {"ff0000", "ff7f00", "ffff00", "00ff00", "0000ff", "4b0082", "9400d3"};
        StringBuilder result = new StringBuilder();
        int colorIndex = 0;

        for (char c : text.toCharArray()) {
            if (c != ' ') {
                result.append("&#").append(rainbowColors[colorIndex % rainbowColors.length]).append(c);
                colorIndex++;
            } else {
                result.append(c);
            }
        }

        return translateColor(result.toString());
    }
}