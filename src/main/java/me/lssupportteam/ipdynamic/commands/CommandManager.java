package me.lssupportteam.ipdynamic.commands;

import me.lssupportteam.ipdynamic.IPDynamic;
import me.lssupportteam.ipdynamic.managers.BanManager;
import me.lssupportteam.ipdynamic.models.BanEntry;
import me.lssupportteam.ipdynamic.models.PlayerData;
import me.lssupportteam.ipdynamic.utils.ColorUtils;
import me.lssupportteam.ipdynamic.utils.IPUtils;
import me.lssupportteam.ipdynamic.utils.PaginationManager;
import me.lssupportteam.ipdynamic.utils.ChatComponentUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final IPDynamic plugin;
    private final SimpleDateFormat dateFormat;
    private final Map<String, Long> commandCooldowns;


    private static final String[] COMMANDS = {
        "help", "reload", "ban", "unban", "alts", "info", "stats", "version", "discord", "migrate", "whitelist", "page"
    };

    private static final String[] BAN_TYPES = {
        "op1", "op2", "single"
    };

    public CommandManager(IPDynamic plugin) {
        this.plugin = plugin;
        this.dateFormat = plugin.getConfigManager().getDateFormat();
        this.commandCooldowns = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!checkCooldown(sender)) {
            sendMessage(sender, plugin.getLangManager().getMessage("system.wait-command"));
            return true;
        }

        if (args.length == 0) {
            sendHelpMenu(sender, 1);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "help":
                handleHelp(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "ban":
                handleBan(sender, args);
                break;
            case "unban":
                handleUnban(sender, args);
                break;
            case "alts":
                handleAlts(sender, args);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "stats":
                handleStats(sender);
                break;
            case "version":
                handleVersion(sender);
                break;
            case "discord":
                handleDiscord(sender, args);
                break;
            case "migrate":
                handleMigrate(sender);
                break;
            case "whitelist":
                handleWhitelist(sender, args);
                break;
            case "page":
                handlePageCommand(sender, args);
                break;
            default:
                sendMessage(sender, plugin.getLangManager().getMessage("errors.unknown-command"));
                break;
        }

        return true;
    }

    private boolean checkCooldown(CommandSender sender) {
        if (!(sender instanceof Player) || sender.hasPermission("ipdynamic.bypass.cooldown")) {
            return true;
        }

        String playerName = sender.getName();
        long currentTime = System.currentTimeMillis();
        long cooldownTime = plugin.getConfigManager().getCommandCooldown() * 1000L;

        Long lastUsed = commandCooldowns.get(playerName);
        if (lastUsed != null && (currentTime - lastUsed) < cooldownTime) {
            return false;
        }

        commandCooldowns.put(playerName, currentTime);
        return true;
    }

    private void handleHelp(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }
        sendHelpMenu(sender, page);
    }

    private void sendHelpMenu(CommandSender sender, int page) {
        List<String> lines = plugin.getLangManager().getMessageLines("menus.help.lines");

        for (String line : lines) {
            sender.sendMessage(ColorUtils.translateColor(line));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("ipdynamic.reload")) {
            sendMessage(sender, plugin.getLangManager().getMessage("no-permission"));
            return;
        }

        sendMessage(sender, plugin.getLangManager().getMessage("system.reloading"));

        CompletableFuture.runAsync(() -> {
            plugin.reloadPlugin();
            sendMessage(sender, plugin.getLangManager().getMessage("reload-success"));
        }, plugin.getExecutorService());
    }

    private void handleBan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ipdynamic.ban")) {
            sendMessage(sender, plugin.getLangManager().getMessage("no-permission"));
            return;
        }

        if (args.length < 3) {
            sendMessage(sender, plugin.getLangManager().getMessage("command.ban-usage"));
            sendMessage(sender, plugin.getLangManager().getMessage("command.ban-examples"));
            sendMessage(sender, plugin.getLangManager().getMessage("command.ban-example1"));
            sendMessage(sender, plugin.getLangManager().getMessage("command.ban-example2"));
            return;
        }

        String type = args[1].toLowerCase();
        String pattern = args[2];
        String reason = args.length > 3 ?
            String.join(" ", Arrays.copyOfRange(args, 3, args.length)) :
            plugin.getConfigManager().getDefaultBanReason();


        if (!type.equals("op1") && !type.equals("op2")) {
            sendMessage(sender, plugin.getLangManager().getMessage("command.invalid-ban-type"));
            return;
        }


        String finalPattern = convertPattern(pattern, type);
        if (finalPattern == null) {
            sendMessage(sender, plugin.getLangManager().getMessage("command.invalid-pattern").replace("{type}", type));
            return;
        }

        String bannedBy = sender instanceof Player ? sender.getName() : "Console";

        sendMessage(sender, plugin.getLangManager().getMessage("command.ban-processing")
            .replace("{type}", type.toUpperCase())
            .replace("{pattern}", finalPattern));

        plugin.getBanManager().ban(finalPattern, reason, bannedBy).thenAccept(result -> {
            if (result.success) {
                sendMessage(sender, plugin.getLangManager().getMessage("command.ban-success-detailed")
                    .replace("{type}", type.toUpperCase())
                    .replace("{pattern}", finalPattern)
                    .replace("{count}", String.format("%,d", result.affectedIps))
                    .replace("{reason}", reason));


                BanEntry.BanType banType = type.equals("op1") ? BanEntry.BanType.OP1 : BanEntry.BanType.OP2;
                plugin.getWebhookService().sendBanNotification(finalPattern, reason, bannedBy, banType, result.affectedIps);


                kickAffectedPlayers(finalPattern, reason);

            } else {
                sendMessage(sender, plugin.getLangManager().getMessage("errors.applying-ban").replace("{error}", result.message));
            }
        }).exceptionally(throwable -> {
            sendMessage(sender, plugin.getLangManager().getMessage("errors.unexpected-error").replace("{error}", throwable.getMessage()));
            return null;
        });
    }

    private void handleUnban(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ipdynamic.unban")) {
            sendMessage(sender, plugin.getLangManager().getMessage("no-permission"));
            return;
        }

        if (args.length < 3) {
            sendMessage(sender, plugin.getLangManager().getMessage("command.unban-usage"));
            return;
        }

        String type = args[1].toLowerCase();
        String pattern = args[2];

        if (!type.equals("op1") && !type.equals("op2")) {
            sendMessage(sender, plugin.getLangManager().getMessage("command.invalid-unban-type"));
            return;
        }

        String finalPattern = convertPattern(pattern, type);
        if (finalPattern == null) {
            sendMessage(sender, plugin.getLangManager().getMessage("command.invalid-pattern").replace("{type}", type));
            return;
        }

        String unbannedBy = sender instanceof Player ? sender.getName() : "Console";

        sendMessage(sender, plugin.getLangManager().getMessage("command.unban-processing")
            .replace("{type}", type.toUpperCase())
            .replace("{pattern}", finalPattern));

        plugin.getBanManager().unban(finalPattern).thenAccept(result -> {
            if (result.success) {
                sendMessage(sender, plugin.getLangManager().getMessage("command.unban-success-detailed")
                    .replace("{type}", type.toUpperCase())
                    .replace("{pattern}", finalPattern)
                    .replace("{count}", String.format("%,d", result.affectedIps)));


                BanEntry.BanType banType = type.equals("op1") ? BanEntry.BanType.OP1 : BanEntry.BanType.OP2;
                plugin.getWebhookService().sendUnbanNotification(finalPattern, unbannedBy, banType, result.affectedIps);

            } else {
                sendMessage(sender, plugin.getLangManager().getMessage("errors.applying-unban").replace("{error}", result.message));
            }
        }).exceptionally(throwable -> {
            sendMessage(sender, plugin.getLangManager().getMessage("errors.unexpected-error").replace("{error}", throwable.getMessage()));
            return null;
        });
    }

    private void handleAlts(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ipdynamic.alts")) {
            sendMessage(sender, plugin.getLangManager().getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            sendMessage(sender, plugin.getLangManager().getMessage("command.alts-usage"));
            return;
        }

        String targetName = args[1];

        CompletableFuture.runAsync(() -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            PlayerData playerData = plugin.getDataManager().getPlayerData(target);

            if (playerData == null) {
                sendMessage(sender, plugin.getLangManager().getMessage("command.player-not-found-db"));
                return;
            }

            List<PlayerData> alts = plugin.getDataManager().findAlts(target.getUniqueId());
            sendAltsMenu(sender, playerData, alts);

        }, plugin.getExecutorService());
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ipdynamic.info")) {
            sendMessage(sender, plugin.getLangManager().getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            sendMessage(sender, plugin.getLangManager().getMessage("command.info-usage"));
            return;
        }

        String targetName = args[1];

        CompletableFuture.runAsync(() -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            PlayerData playerData = plugin.getDataManager().getPlayerData(target);

            if (playerData == null) {
                sendMessage(sender, plugin.getLangManager().getMessage("command.player-not-found-db"));
                return;
            }

            sendPlayerInfoMenu(sender, playerData);

        }, plugin.getExecutorService());
    }

    private void handleStats(CommandSender sender) {
        if (!sender.hasPermission("ipdynamic.stats")) {
            sendMessage(sender, plugin.getLangManager().getMessage("no-permission"));
            return;
        }

        CompletableFuture.runAsync(() -> {
            Map<String, Integer> banStats = plugin.getBanManager().getStatistics();
            Map<String, Object> dataStats = plugin.getDataManager().getStatistics();
            sendStatsMenu(sender, banStats, dataStats);
        }, plugin.getExecutorService());
    }

    private void handleVersion(CommandSender sender) {
        List<String> lines = plugin.getLangManager().getMessageLines("menus.version.lines");

        for (String line : lines) {
            String processedLine = line
                .replace("{server_version}", plugin.getNMSVersion().getFullVersionInfo())
                .replace("{java_version}", System.getProperty("java.version"));
            sender.sendMessage(ColorUtils.translateColor(processedLine));
        }
    }

    private void handleDiscord(CommandSender sender, String[] args) {
        // Restrict to console only
        if (sender instanceof Player) {
            sendMessage(sender, plugin.getLangManager().getMessage("discord.console-only"));
            return;
        }

        if (!sender.hasPermission("ipdynamic.discord")) {
            sendMessage(sender, plugin.getLangManager().getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            sendMessage(sender, plugin.getLangManager().getMessage("discord.usage"));
            return;
        }

        String subCommand = args[1].toLowerCase();
        switch (subCommand) {
            case "status":
                handleDiscordStatus(sender);
                break;
            case "stats":
                handleDiscordStats(sender);
                break;
            default:
                sendMessage(sender, plugin.getLangManager().getMessage("discord.invalid-subcommand"));
                break;
        }
    }

    private void handleDiscordStatus(CommandSender sender) {
        if (plugin.getDiscordManager() == null) {
            sendMessage(sender, plugin.getLangManager().getMessage("discord.not-initialized"));
            return;
        }

        var discordManager = plugin.getDiscordManager();
        String diagnosticInfo = discordManager.getDiagnosticInfo();

        // Send diagnostic info line by line
        for (String line : diagnosticInfo.split("\n")) {
            if (line.trim().isEmpty()) continue;

            // Color code the output
            if (line.contains("❌")) {
                sender.sendMessage(ColorUtils.translateColor("&c" + line));
            } else if (line.contains("✅")) {
                sender.sendMessage(ColorUtils.translateColor("&a" + line));
            } else if (line.contains("⚠️")) {
                sender.sendMessage(ColorUtils.translateColor("&e" + line));
            } else if (line.contains("===")) {
                sender.sendMessage(ColorUtils.translateColor("&b" + line));
            } else {
                sender.sendMessage(ColorUtils.translateColor("&7" + line));
            }
        }

        // Additional help if bot is not working
        if (!discordManager.isEnabled()) {
            sendMessage(sender, plugin.getLangManager().getMessage("discord.setup-instructions"));
            sendMessage(sender, plugin.getLangManager().getMessage("discord.step1"));
            sendMessage(sender, plugin.getLangManager().getMessage("discord.step2"));
            sendMessage(sender, plugin.getLangManager().getMessage("discord.step3"));
            sendMessage(sender, plugin.getLangManager().getMessage("discord.step4"));
        } else if (!discordManager.isConnected()) {
            sendMessage(sender, plugin.getLangManager().getMessage("discord.troubleshooting"));
            sendMessage(sender, plugin.getLangManager().getMessage("discord.trouble1"));
            sendMessage(sender, plugin.getLangManager().getMessage("discord.trouble2"));
            sendMessage(sender, plugin.getLangManager().getMessage("discord.trouble3"));
        }
    }

    private void handleDiscordStats(CommandSender sender) {
        if (plugin.getDiscordManager() == null || !plugin.getDiscordManager().isConnected()) {
            sendMessage(sender, plugin.getLangManager().getMessage("discord.not-connected"));
            return;
        }

        sendMessage(sender, plugin.getLangManager().getMessage("discord.sending-stats"));

        plugin.getDiscordManager().sendCountryStatsAsync()
            .thenRun(() -> sendMessage(sender, plugin.getLangManager().getMessage("discord.stats-sent-success")))
            .exceptionally(throwable -> {
                sendMessage(sender, plugin.getLangManager().getMessage("discord.error-sending-stats").replace("{error}", throwable.getMessage()));
                return null;
            });
    }

    private void handleMigrate(CommandSender sender) {
        // Restrict to console only
        if (sender instanceof Player) {
            sendMessage(sender, plugin.getLangManager().getMessage("discord.console-only"));
            return;
        }

        if (!sender.hasPermission("ipdynamic.migrate")) {
            sendMessage(sender, plugin.getLangManager().getMessage("no-permission"));
            return;
        }

        sendMessage(sender, plugin.getLangManager().getMessage("migration-command.starting"));

        CompletableFuture.runAsync(() -> {
            try {
                plugin.getConfigMigrator().migrateAllConfigs();
                sendMessage(sender, plugin.getLangManager().getMessage("migration-command.completed"));
                sendMessage(sender, plugin.getLangManager().getMessage("migration-command.check-logs"));
            } catch (Exception e) {
                sendMessage(sender, plugin.getLangManager().getMessage("migration-command.error").replace("{error}", e.getMessage()));
                plugin.getLogger().severe("Error en migración manual: " + e.getMessage());
            }
        }, plugin.getExecutorService());
    }

    private void handleWhitelist(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ipdynamic.whitelist")) {
            sendMessage(sender, plugin.getLangManager().getMessage("no-permission"));
            return;
        }

        if (args.length < 3) {
            sendMessage(sender, plugin.getLangManager().getMessage("whitelist-command.usage"));
            sendMessage(sender, plugin.getLangManager().getMessage("whitelist-command.examples-header"));
            sendMessage(sender, plugin.getLangManager().getMessage("whitelist-command.example-add"));
            sendMessage(sender, plugin.getLangManager().getMessage("whitelist-command.example-remove"));
            sendMessage(sender, plugin.getLangManager().getMessage("whitelist-command.example-list"));
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add":
                if (args.length < 3) {
                    sendMessage(sender, plugin.getLangManager().getMessage("whitelist-command.specify-player"));
                    return;
                }
                String playerToAdd = args[2];
                handleWhitelistAdd(sender, playerToAdd);
                break;

            case "remove":
                if (args.length < 3) {
                    sendMessage(sender, plugin.getLangManager().getMessage("whitelist-command.specify-player"));
                    return;
                }
                String playerToRemove = args[2];
                handleWhitelistRemove(sender, playerToRemove);
                break;

            case "list":
                handleWhitelistList(sender);
                break;

            default:
                sendMessage(sender, plugin.getLangManager().getMessage("whitelist-command.invalid-action"));
                break;
        }
    }

    private void handleWhitelistAdd(CommandSender sender, String playerName) {
        CompletableFuture.runAsync(() -> {
            if (plugin.getWhitelistManager().isWhitelisted(playerName)) {
                sendMessage(sender, plugin.getLangManager().getMessage("whitelist-command.already-whitelisted").replace("{player}", playerName));
                return;
            }

            plugin.getWhitelistManager().addPlayer(playerName);
            sendMessage(sender, plugin.getLangManager().getMessage("whitelist-command.added-success").replace("{player}", playerName));
            sendMessage(sender, plugin.getLangManager().getMessage("whitelist-command.added-info"));

            // Log action
            plugin.getLogger().info(plugin.getLangManager().getMessage("log.player-added-whitelist").replace("{player}", playerName).replace("{admin}", sender.getName()));
        }, plugin.getExecutorService());
    }

    private void handleWhitelistRemove(CommandSender sender, String playerName) {
        CompletableFuture.runAsync(() -> {
            if (!plugin.getWhitelistManager().isWhitelisted(playerName)) {
                sendMessage(sender, plugin.getLangManager().getMessage("whitelist-command.not-whitelisted").replace("{player}", playerName));
                return;
            }

            plugin.getWhitelistManager().removePlayer(playerName);
            sendMessage(sender, plugin.getLangManager().getMessage("whitelist-command.removed-success").replace("{player}", playerName));

            // Log action
            plugin.getLogger().info(plugin.getLangManager().getMessage("log.player-removed-whitelist").replace("{player}", playerName).replace("{admin}", sender.getName()));
        }, plugin.getExecutorService());
    }

    private void handleWhitelistList(CommandSender sender) {
        CompletableFuture.runAsync(() -> {
            Set<String> whitelistedPlayers = plugin.getWhitelistManager().getAllWhitelistedPlayers();

            // Send header
            List<String> headerLines = plugin.getLangManager().getMessageLines("menus.whitelist.header");
            for (String line : headerLines) {
                sender.sendMessage(ColorUtils.translateColor(line));
            }

            // Send player count
            List<String> playerCountLines = plugin.getLangManager().getMessageLines("menus.whitelist.player-count");
            for (String line : playerCountLines) {
                String processedLine = line.replace("{count}", String.valueOf(whitelistedPlayers.size()));
                sender.sendMessage(ColorUtils.translateColor(processedLine));
            }

            if (whitelistedPlayers.isEmpty()) {
                // Send no players message
                List<String> noPlayersLines = plugin.getLangManager().getMessageLines("menus.whitelist.no-players");
                for (String line : noPlayersLines) {
                    sender.sendMessage(ColorUtils.translateColor(line));
                }
            } else {
                // Send player entries
                String playerEntryTemplate = plugin.getLangManager().getMessage("menus.whitelist.player-entry");
                for (String player : whitelistedPlayers) {
                    String playerEntry = playerEntryTemplate.replace("{player}", player);
                    sender.sendMessage(ColorUtils.translateColor(playerEntry));
                }
            }

            // Send description
            List<String> descriptionLines = plugin.getLangManager().getMessageLines("menus.whitelist.description");
            for (String line : descriptionLines) {
                sender.sendMessage(ColorUtils.translateColor(line));
            }

            // Send footer
            List<String> footerLines = plugin.getLangManager().getMessageLines("menus.whitelist.footer");
            for (String line : footerLines) {
                sender.sendMessage(ColorUtils.translateColor(line));
            }
        }, plugin.getExecutorService());
    }

    private String getStatusColor(String status) {
        switch (status.toUpperCase()) {
            case "CONNECTED": return "&a";
            case "DISCONNECTED": return "&c";
            case "DISABLED": return "&7";
            default: return "&e";
        }
    }

    private void sendAltsMenu(CommandSender sender, PlayerData playerData, List<PlayerData> alts) {
        // Send header
        List<String> headerLines = plugin.getLangManager().getMessageLines("menus.alts.header");
        for (String line : headerLines) {
            sender.sendMessage(ColorUtils.translateColor(line));
        }

        // Send player info
        List<String> playerInfoLines = plugin.getLangManager().getMessageLines("menus.alts.player-info");
        for (String line : playerInfoLines) {
            String processedLine = line
                .replace("{player}", playerData.getUsername())
                .replace("{connections}", String.valueOf(playerData.getTotalConnections()))
                .replace("{last}", dateFormat.format(new Date(playerData.getLastLogin())));
            sender.sendMessage(ColorUtils.translateColor(processedLine));
        }

        if (alts.isEmpty()) {
            List<String> noAltsLines = plugin.getLangManager().getMessageLines("menus.alts.no-alts");
            for (String line : noAltsLines) {
                sender.sendMessage(ColorUtils.translateColor(line));
            }
        } else {
            int count = 0;
            for (PlayerData alt : alts) {
                if (count >= 10) break;

                // Get country info for the alt IP
                String country = "Desconocido";
                if (alt.getGeoLocation() != null && alt.getGeoLocation().getCountry() != null) {
                    country = alt.getGeoLocation().getCountry();
                }

                List<String> altEntryLines = plugin.getLangManager().getMessageLines("menus.alts.alt-entry");
                for (String line : altEntryLines) {
                    String processedLine = line
                            .replace("{name}", alt.getUsername())
                            .replace("{connections}", String.valueOf(alt.getTotalConnections()))
                            .replace("{ip}", alt.getLastIp())
                            .replace("{country}", country);
                    sender.sendMessage(ColorUtils.translateColor(processedLine));
                }
                count++;
            }

            List<String> totalLines = plugin.getLangManager().getMessageLines("menus.alts.total");
            for (String line : totalLines) {
                String processedLine = line.replace("{count}", String.valueOf(alts.size()));
                sender.sendMessage(ColorUtils.translateColor(processedLine));
            }
        }

        // Send footer
        List<String> footerLines = plugin.getLangManager().getMessageLines("menus.alts.footer");
        for (String line : footerLines) {
            sender.sendMessage(ColorUtils.translateColor(line));
        }
    }

    private void sendPlayerInfoMenu(CommandSender sender, PlayerData playerData) {
        String country = "Desconocido";
        String flag = "";
        if (playerData.getGeoLocation() != null) {
            country = playerData.getGeoLocation().getCountry();
            flag = "🌍";
        }

        String adminStatus = playerData.isAdmin() ? "&a✅ Sí" : "&c❌ No";

        // Send header
        List<String> headerLines = plugin.getLangManager().getMessageLines("menus.info.header");
        for (String line : headerLines) {
            sender.sendMessage(ColorUtils.translateColor(line));
        }

        // Send player data
        List<String> playerDataLines = plugin.getLangManager().getMessageLines("menus.info.player-data");
        for (String line : playerDataLines) {
            String processedLine = line
                .replace("{player}", playerData.getUsername())
                .replace("{uuid}", playerData.getUuid().toString())
                .replace("{connections}", String.valueOf(playerData.getTotalConnections()))
                .replace("{first_join}", dateFormat.format(new Date(playerData.getFirstLogin())))
                .replace("{last_join}", dateFormat.format(new Date(playerData.getLastLogin())))
                .replace("{current_ip}", playerData.getLastIp())
                .replace("{country}", country)
                .replace("{flag}", flag)
                .replace("{admin_status}", adminStatus)
                .replace("{ip_count}", String.valueOf(playerData.getIpHistory().size()))
                .replace("{alt_count}", String.valueOf(plugin.getDataManager().findAlts(playerData.getUuid()).size()));
            sender.sendMessage(ColorUtils.translateColor(processedLine));
        }

        // Send IP history section
        List<String> ipHistoryLines = plugin.getLangManager().getMessageLines("menus.info.ip-history");
        for (String line : ipHistoryLines) {
            sender.sendMessage(ColorUtils.translateColor(line));
        }

        // Get connection statistics by IP (excluding current IP)
        Map<String, Integer> ipConnectionStats = plugin.getDataManager().getConnectionStatsByIP(playerData.getUuid());
        Map<String, Integer> ipHistoryFiltered = new HashMap<>();

        // Filter out current IP from history
        for (String ip : playerData.getIpHistory()) {
            if (!ip.equals(playerData.getLastIp())) {
                ipHistoryFiltered.put(ip, ipConnectionStats.getOrDefault(ip, 1));
            }
        }

        if (ipHistoryFiltered.size() > 0) {
            // Check if sender is a player and if there are many IPs (for pagination)
            if (sender instanceof Player && ipHistoryFiltered.size() > 5) {
                Player player = (Player) sender;

                // Create pagination state
                plugin.getPaginationManager().createPaginationState(player, playerData.getUsername(), ipHistoryFiltered);

                // Show first page
                showIPHistoryPage(player, playerData.getUsername());
            } else {
                // Show all IPs without pagination (console or few IPs)
                for (Map.Entry<String, Integer> entry : ipHistoryFiltered.entrySet()) {
                    String ip = entry.getKey();
                    int connections = entry.getValue();

                    // Get linked accounts for this IP
                    List<String> linkedAccounts = plugin.getDataManager().getUsernamesLinkedToIP(ip);
                    // Remove current player from linked accounts list
                    linkedAccounts.removeIf(username -> username.equalsIgnoreCase(playerData.getUsername()));
                    String linkedAccountsStr = linkedAccounts.isEmpty() ?
                        plugin.getLangManager().getMessage("info.no-linked-accounts") :
                        String.join(", ", linkedAccounts);

                    // Get country and display IP entry
                    if (plugin.getGeoIPService() != null) {
                        plugin.getGeoIPService().getLocation(ip).thenAccept(geoLocation -> {
                            String geoCountry = "Desconocido";
                            if (geoLocation != null && geoLocation.getCountry() != null) {
                                geoCountry = geoLocation.getCountry();
                            }

                            displayIPEntry(sender, ip, playerData, geoCountry, connections, linkedAccountsStr);
                        }).exceptionally(throwable -> {
                            displayIPEntry(sender, ip, playerData, "Desconocido", connections, linkedAccountsStr);
                            return null;
                        });
                    } else {
                        displayIPEntry(sender, ip, playerData, "Desconocido", connections, linkedAccountsStr);
                    }
                }
            }
        } else {
            List<String> noIpHistoryLines = plugin.getLangManager().getMessageLines("menus.info.no-ip-history");
            for (String line : noIpHistoryLines) {
                sender.sendMessage(ColorUtils.translateColor(line));
            }
        }

        // Send footer only if not using pagination
        if (!(sender instanceof Player) || ipHistoryFiltered.size() <= 5) {
            List<String> footerLines = plugin.getLangManager().getMessageLines("menus.info.footer");
            for (String line : footerLines) {
                sender.sendMessage(ColorUtils.translateColor(line));
            }
        }
    }

    private void sendStatsMenu(CommandSender sender, Map<String, Integer> banStats, Map<String, Object> dataStats) {
        // Send header
        List<String> headerLines = plugin.getLangManager().getMessageLines("menus.stats.header");
        for (String line : headerLines) {
            sender.sendMessage(ColorUtils.translateColor(line));
        }

        // Send data section
        List<String> dataSectionLines = plugin.getLangManager().getMessageLines("menus.stats.data-section");
        for (String line : dataSectionLines) {
            String processedLine = line
                .replace("{total_players}", String.valueOf(dataStats.getOrDefault("totalPlayers", 0)))
                .replace("{unique_ips}", String.valueOf(dataStats.getOrDefault("uniqueIps", 0)))
                .replace("{active_connections}", String.valueOf(dataStats.getOrDefault("activeConnections", 0)))
                .replace("{connection_history}", String.valueOf(dataStats.getOrDefault("connectionHistory", 0)))
                .replace("{admin_logins}", String.valueOf(dataStats.getOrDefault("adminLogins", 0)))
                .replace("{detected_alts}", String.valueOf(dataStats.getOrDefault("detectedAlts", 0)));
            sender.sendMessage(ColorUtils.translateColor(processedLine));
        }

        // Send ban section
        List<String> banSectionLines = plugin.getLangManager().getMessageLines("menus.stats.ban-section");
        for (String line : banSectionLines) {
            String processedLine = line
                .replace("{single_bans}", String.valueOf(banStats.getOrDefault("singleBans", 0)))
                .replace("{op1_bans}", String.valueOf(banStats.getOrDefault("op1Bans", 0)))
                .replace("{op2_bans}", String.valueOf(banStats.getOrDefault("op2Bans", 0)))
                .replace("{pending_bans}", String.valueOf(banStats.getOrDefault("pendingOp2", 0)));
            sender.sendMessage(ColorUtils.translateColor(processedLine));
        }

        // Send footer
        List<String> footerLines = plugin.getLangManager().getMessageLines("menus.stats.footer");
        for (String line : footerLines) {
            sender.sendMessage(ColorUtils.translateColor(line));
        }
    }

    private String convertPattern(String input, String type) {
        if (!IPUtils.isValidIpAddress(input) && !IPUtils.isValidIpPattern(input)) {
            return null;
        }

        String[] parts = input.split("\\.");
        if (parts.length != 4) return null;

        switch (type) {
            case "op1":

                return parts[0] + "." + parts[1] + "." + parts[2] + ".*";
            case "op2":

                return parts[0] + "." + parts[1] + ".*.*";
            default:
                return input;
        }
    }

    private void kickAffectedPlayers(String pattern, String reason) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String kickMessage = ColorUtils.translateColor(
                plugin.getConfigManager().getKickMessage().replace("{reason}", reason)
            );

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getWhitelistManager().isWhitelisted(player.getName())) {
                    continue;
                }

                String playerIp = player.getAddress() != null ?
                    player.getAddress().getAddress().getHostAddress() : null;

                if (playerIp != null && IPUtils.matches(playerIp, pattern)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.kickPlayer(kickMessage);
                    });
                }
            }
        });
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ColorUtils.translateColor("&8[&bIPDynamic&8] " + message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList(COMMANDS), completions);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("ban") || subCommand.equals("unban")) {
                StringUtil.copyPartialMatches(args[1], Arrays.asList(BAN_TYPES), completions);
            } else if (subCommand.equals("alts") || subCommand.equals("info")) {

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (sender.hasPermission("ipdynamic.see.all") ||
                        (sender instanceof Player && ((Player) sender).canSee(player))) {
                        completions.add(player.getName());
                    }
                }
            } else if (subCommand.equals("discord")) {
                StringUtil.copyPartialMatches(args[1], Arrays.asList("status", "stats"), completions);
            } else if (subCommand.equals("whitelist")) {
                StringUtil.copyPartialMatches(args[1], Arrays.asList("add", "remove", "list"), completions);
            } else if (subCommand.equals("page")) {
                StringUtil.copyPartialMatches(args[1], Arrays.asList("next", "prev", "previous"), completions);
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();

            if (subCommand.equals("whitelist")) {
                if (action.equals("add")) {
                    // For add, suggest online players
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (sender.hasPermission("ipdynamic.see.all") ||
                            (sender instanceof Player && ((Player) sender).canSee(player))) {
                            completions.add(player.getName());
                        }
                    }
                } else if (action.equals("remove")) {
                    // For remove, suggest whitelisted players
                    Set<String> whitelistedPlayers = plugin.getWhitelistManager().getAllWhitelistedPlayers();
                    StringUtil.copyPartialMatches(args[2], whitelistedPlayers, completions);
                }
            } else if (subCommand.equals("page")) {
                // For page command, suggest online players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (sender.hasPermission("ipdynamic.see.all") ||
                        (sender instanceof Player && ((Player) sender).canSee(player))) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        Collections.sort(completions);
        return completions;
    }

    private void handlePageCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, plugin.getLangManager().getMessage("errors.player-only"));
            return;
        }

        Player player = (Player) sender;
        PaginationManager paginationManager = plugin.getPaginationManager();

        if (args.length < 3) {
            sendMessage(sender, plugin.getLangManager().getMessage("errors.usage") + " /ipdy page [next|prev] [player]");
            return;
        }

        String direction = args[1].toLowerCase();
        String targetPlayer = args[2];

        if (!paginationManager.hasState(player.getUniqueId())) {
            sendMessage(sender, plugin.getLangManager().getMessage("errors.no-pagination-state"));
            return;
        }

        boolean success = false;
        if ("next".equals(direction)) {
            success = paginationManager.nextPage(player.getUniqueId());
        } else if ("prev".equals(direction) || "previous".equals(direction)) {
            success = paginationManager.previousPage(player.getUniqueId());
        }

        if (success) {
            // Show the updated page
            showIPHistoryPage(player, targetPlayer);
        } else {
            sendMessage(sender, plugin.getLangManager().getMessage("errors.no-more-pages"));
        }
    }

    private void showIPHistoryPage(Player player, String targetPlayerName) {
        PaginationManager.PaginationData pageData = plugin.getPaginationManager().getCurrentPage(player.getUniqueId());

        if (pageData == null) {
            sendMessage(player, plugin.getLangManager().getMessage("errors.no-pagination-data"));
            return;
        }

        // Get player data for the target
        PlayerData playerData = plugin.getDataManager().getPlayerData(targetPlayerName);
        if (playerData == null) {
            sendMessage(player, plugin.getLangManager().getMessage("errors.player-not-found").replace("{player}", targetPlayerName));
            return;
        }

        // Send page information
        String pageInfo = plugin.getLangManager().getMessage("menus.info.pagination.page-info")
                .replace("{current}", String.valueOf(pageData.getCurrentPage()))
                .replace("{total}", String.valueOf(pageData.getTotalPages()));
        player.sendMessage(ColorUtils.translateColor(pageInfo));

        // Show IPs for this page
        for (Map.Entry<String, Integer> entry : pageData.getPageIps().entrySet()) {
            String ip = entry.getKey();
            int connections = entry.getValue();

            // Get linked accounts for this IP
            List<String> linkedAccounts = plugin.getDataManager().getUsernamesLinkedToIP(ip);
            // Remove current player from linked accounts list
            linkedAccounts.removeIf(username -> username.equalsIgnoreCase(targetPlayerName));
            String linkedAccountsStr = linkedAccounts.isEmpty() ?
                plugin.getLangManager().getMessage("info.no-linked-accounts") :
                String.join(", ", linkedAccounts);

            // Get country and display IP entry
            if (plugin.getGeoIPService() != null) {
                plugin.getGeoIPService().getLocation(ip).thenAccept(geoLocation -> {
                    String country = "Desconocido";
                    if (geoLocation != null && geoLocation.getCountry() != null) {
                        country = geoLocation.getCountry();
                    }

                    displayIPEntry(player, ip, playerData, country, connections, linkedAccountsStr);
                }).exceptionally(throwable -> {
                    displayIPEntry(player, ip, playerData, "Desconocido", connections, linkedAccountsStr);
                    return null;
                });
            } else {
                displayIPEntry(player, ip, playerData, "Desconocido", connections, linkedAccountsStr);
            }
        }

        // Show pagination controls
        String nextText = plugin.getLangManager().getMessage("menus.info.pagination.next-page");
        String prevText = plugin.getLangManager().getMessage("menus.info.pagination.previous-page");
        String nextHover = plugin.getLangManager().getMessage("menus.info.pagination.next-page-hover");
        String prevHover = plugin.getLangManager().getMessage("menus.info.pagination.previous-page-hover");

        ChatComponentUtils.sendPaginationMessage(player, "",
                pageData.hasNext(), pageData.hasPrevious(),
                nextText, nextHover, prevText, prevHover, targetPlayerName);
    }

    /**
     * Helper method to display IP entry information
     */
    private void displayIPEntry(CommandSender sender, String ip, PlayerData playerData,
                               String country, int connections, String linkedAccounts) {
        List<String> ipEntryLines = plugin.getLangManager().getMessageLines("menus.info.ip-entry");
        for (String line : ipEntryLines) {
            String processedLine = line
                    .replace("{ip}", ip)
                    .replace("{last_seen}", dateFormat.format(new Date(playerData.getIpFirstSeen(ip))))
                    .replace("{country}", country)
                    .replace("{connections}", String.valueOf(connections))
                    .replace("{linked_accounts}", linkedAccounts);
            sender.sendMessage(ColorUtils.translateColor(processedLine));
        }
    }
}