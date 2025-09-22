package me.lssupportteam.ipdynamic.listeners;

import me.lssupportteam.ipdynamic.IPDynamic;
import me.lssupportteam.ipdynamic.models.BanEntry;
import me.lssupportteam.ipdynamic.models.PlayerData;
import me.lssupportteam.ipdynamic.utils.ColorUtils;
import me.lssupportteam.ipdynamic.utils.IPUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayerConnectionListener implements Listener {

    private final IPDynamic plugin;

    public PlayerConnectionListener(IPDynamic plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        InetAddress address = event.getAddress();
        String ip = address.getHostAddress();


        if (plugin.getBanManager().isBanned(ip)) {
            BanEntry banEntry = plugin.getBanManager().getBanEntry(ip);
            if (banEntry != null) {
                String kickMessage = ColorUtils.translateColor(
                    plugin.getConfigManager().getKickMessage()
                        .replace("{reason}", banEntry.getReason())
                );
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);

                plugin.getLogger().info(String.format(
                    "Conexión bloqueada: %s (%s) - IP baneada: %s",
                    playerName, ip, banEntry.getReason()
                ));
                return;
            }
        }


        if (plugin.getConfigManager().isWhitelistImmune() &&
            plugin.getWhitelistManager().isWhitelisted(playerName)) {

            return;
        }


        if (plugin.getConfigManager().isIgnoreLocalIps() && IPUtils.isLocalIp(ip)) {
            return;
        }


        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info(String.format(
                "Intento de conexión: %s desde %s", playerName, ip
            ));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String ip = player.getAddress() != null ?
            player.getAddress().getAddress().getHostAddress() : "UNKNOWN";


        CompletableFuture.runAsync(() -> {
            try {

                PlayerData playerData = plugin.getDataManager().registerConnection(player);
                boolean isFirstJoin = playerData.getTotalConnections() == 1;


                if (plugin.getConfigManager().isAltDetectionEnabled()) {
                    detectAndNotifyAlts(player, playerData, ip);
                }


                if (plugin.getWebhookConfigManager().isConnectionNotificationsEnabled()) {

                    if (!plugin.getWebhookConfigManager().isFirstJoinOnly() || isFirstJoin) {
                        plugin.getWebhookService().sendConnectionNotification(playerData, ip, isFirstJoin);
                    }
                }


                if ((player.hasPermission("ipdynamic.admin") || player.isOp()) &&
                    plugin.getWebhookConfigManager().isAdminNotificationsEnabled()) {
                    plugin.getWebhookService().sendAdminLoginNotification(playerData, ip);
                }


                if (plugin.getConfigManager().isInGameNotificationsEnabled()) {
                    notifyAdminsInGame(player, playerData, ip, isFirstJoin);
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Error procesando conexión de " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, plugin.getExecutorService());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();


        CompletableFuture.runAsync(() -> {
            try {
                plugin.getDataManager().registerDisconnection(player);
            } catch (Exception e) {
                plugin.getLogger().severe("Error procesando desconexión de " + player.getName() + ": " + e.getMessage());
            }
        }, plugin.getExecutorService());
    }

    private void detectAndNotifyAlts(Player player, PlayerData playerData, String ip) {
        List<PlayerData> alts = plugin.getDataManager().findAlts(player.getUniqueId());

        if (!alts.isEmpty()) {

            List<PlayerData> significantAlts = alts.stream()
                .filter(alt -> {

                    long sharedIps = alt.getIpHistory().stream()
                        .filter(playerData.getIpHistory()::contains)
                        .count();
                    return sharedIps >= plugin.getConfigManager().getMinSharedIps();
                })
                .limit(20) // Limitar para evitar spam
                .toList();

            if (!significantAlts.isEmpty()) {

                if (plugin.getWebhookConfigManager().isAltNotificationsEnabled()) {
                    plugin.getWebhookService().sendAltDetectionNotification(playerData, significantAlts);
                }


                if (plugin.getConfigManager().isInGameAltDetection() &&
                    plugin.getConfigManager().isNotifyAdminsOnAlt()) {
                    notifyAdminsAboutAlts(player, significantAlts);
                }


                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info(String.format(
                        "Detectadas %d cuentas alternativas para %s",
                        significantAlts.size(), player.getName()
                    ));
                }
            }
        }
    }

    private void notifyAdminsInGame(Player player, PlayerData playerData, String ip, boolean isFirstJoin) {
        String notification = null;

        if (isFirstJoin) {
            notification = String.format(
                "&a[&b+&a] &e%s &fse ha conectado por primera vez desde &e%s",
                player.getName(), ip
            );
        } else if (playerData.hasMultipleIps()) {
            notification = String.format(
                "&a[&b+&a] &e%s &fse conectó desde &e%s &7(tiene %d IPs registradas)",
                player.getName(), ip, playerData.getIpHistory().size()
            );
        }

        if (notification != null) {
            final String finalNotification = notification;
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player admin : Bukkit.getOnlinePlayers()) {
                    if (admin.hasPermission("ipdynamic.notify.connections")) {
                        admin.sendMessage(ColorUtils.translateColor(finalNotification));
                    }
                }
            });
        }
    }

    private void notifyAdminsAboutAlts(Player player, List<PlayerData> alts) {
        String message = String.format(
            "&c[&e⚠&c] &6%s &ftiene &c%d &fposibles alts: &e%s",
            player.getName(),
            alts.size(),
            alts.stream()
                .limit(5)
                .map(PlayerData::getUsername)
                .reduce((a, b) -> a + "&7, &e" + b)
                .orElse("Ninguno")
        );

        if (alts.size() > 5) {
            message += " &7y " + (alts.size() - 5) + " más";
        }

        final String finalMessage = message;
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission("ipdynamic.notify.alts")) {
                    admin.sendMessage(ColorUtils.translateColor(finalMessage));
                }
            }
        });
    }
}