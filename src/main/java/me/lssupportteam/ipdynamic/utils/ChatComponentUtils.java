package me.lssupportteam.ipdynamic.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/**
 * Utility class for creating clickable chat components
 */
public class ChatComponentUtils {

    /**
     * Creates a clickable pagination component for next page
     */
    public static TextComponent createNextPageComponent(String displayText, String hoverText, String targetPlayer) {
        TextComponent component = new TextComponent(ColorUtils.translateColor(displayText));

        // Click event to run pagination command
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ipdy page next " + targetPlayer));

        // Hover text
        if (hoverText != null && !hoverText.isEmpty()) {
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(ColorUtils.translateColor(hoverText)).create()));
        }

        return component;
    }

    /**
     * Creates a clickable pagination component for previous page
     */
    public static TextComponent createPreviousPageComponent(String displayText, String hoverText, String targetPlayer) {
        TextComponent component = new TextComponent(ColorUtils.translateColor(displayText));

        // Click event to run pagination command
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ipdy page prev " + targetPlayer));

        // Hover text
        if (hoverText != null && !hoverText.isEmpty()) {
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(ColorUtils.translateColor(hoverText)).create()));
        }

        return component;
    }

    /**
     * Sends a message with pagination controls to a player
     */
    public static void sendPaginationMessage(Player player, String pageInfo,
                                           boolean hasNext, boolean hasPrevious,
                                           String nextText, String nextHover,
                                           String prevText, String prevHover,
                                           String targetPlayer) {

        // Send page info
        if (pageInfo != null && !pageInfo.isEmpty()) {
            player.sendMessage(ColorUtils.translateColor(pageInfo));
        }

        // Create navigation components
        ComponentBuilder builder = new ComponentBuilder("");

        boolean hasAnyNavigation = false;

        // Add previous page button if available
        if (hasPrevious) {
            TextComponent prevComponent = createPreviousPageComponent(prevText, prevHover, targetPlayer);
            builder.append(prevComponent);
            hasAnyNavigation = true;
        }

        // Add spacing between buttons if both exist
        if (hasPrevious && hasNext) {
            builder.append("   "); // 3 spaces
        }

        // Add next page button if available
        if (hasNext) {
            TextComponent nextComponent = createNextPageComponent(nextText, nextHover, targetPlayer);
            builder.append(nextComponent);
            hasAnyNavigation = true;
        }

        // Send navigation if any buttons exist
        if (hasAnyNavigation) {
            player.spigot().sendMessage(builder.create());
        }
    }

    /**
     * Centers text with spaces
     */
    public static String centerText(String text, int totalWidth) {
        if (text.length() >= totalWidth) {
            return text;
        }

        int spacesNeeded = totalWidth - text.length();
        int leftSpaces = spacesNeeded / 2;

        StringBuilder centered = new StringBuilder();
        for (int i = 0; i < leftSpaces; i++) {
            centered.append(" ");
        }
        centered.append(text);

        return centered.toString();
    }
}