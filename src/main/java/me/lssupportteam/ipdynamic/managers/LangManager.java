package me.lssupportteam.ipdynamic.managers;

import me.lssupportteam.ipdynamic.IPDynamic;
import me.lssupportteam.ipdynamic.utils.ColorUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class LangManager {

    private final IPDynamic plugin;
    private final File langDir;
    private YamlConfiguration langConfig;
    private String currentLang;
    private final Map<String, String> messageCache;

    public LangManager(IPDynamic plugin) {
        this.plugin = plugin;
        this.langDir = new File(plugin.getDataFolder(), "lang");
        this.messageCache = new HashMap<>();
        this.currentLang = "spanish";

        if (!langDir.exists()) {
            langDir.mkdirs();
        }
    }

    public void loadLanguageFile(String fileName) {
        this.currentLang = fileName.replace(".yml", "");
        File langFile = new File(langDir, fileName);


        if (!langFile.exists()) {
            saveDefaultLanguageFile(fileName);
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
        cacheMessages();

        plugin.getLogger().info("Idioma cargado: " + currentLang);
    }

    private void saveDefaultLanguageFile(String fileName) {
        try {
            try (InputStream inputStream = plugin.getResource("lang/" + fileName)) {
                if (inputStream != null) {
                    Files.copy(inputStream, new File(langDir, fileName).toPath());
                } else {
                    createDefaultLanguageFile(fileName);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error creando archivo de idioma: " + e.getMessage());
            createDefaultLanguageFile(fileName);
        }
    }

    private void createDefaultLanguageFile(String fileName) {
        YamlConfiguration config = new YamlConfiguration();

        if (fileName.startsWith("spanish")) {
            createSpanishMessages(config);
        } else if (fileName.startsWith("english")) {
            createEnglishMessages(config);
        } else {
            createSpanishMessages(config); // Fallback
        }

        try {
            config.save(new File(langDir, fileName));
            plugin.getLogger().info("Archivo de idioma creado: " + fileName);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando archivo de idioma: " + e.getMessage());
        }
    }

    private void createSpanishMessages(YamlConfiguration config) {

        config.set("prefix", "&8[&bIPDynamic&8]");
        config.set("no-permission", "&cNo tienes permisos para usar este comando.");
        config.set("player-not-found", "&cJugador no encontrado.");
        config.set("invalid-ip", "&cFormato de IP inválido.");
        config.set("reload-success", "&a¡Plugin recargado exitosamente!");


        config.set("ban.success", "&a✅ Ban aplicado exitosamente!");
        config.set("ban.already-banned", "&cEsta IP/patrón ya está baneada.");
        config.set("ban.invalid-type", "&cTipo de ban inválido. Usa: op1, op2");
        config.set("ban.reason-default", "Comportamiento sospechoso");


        config.set("unban.success", "&a✅ Unban aplicado exitosamente!");
        config.set("unban.not-banned", "&cEsta IP/patrón no está baneada.");
        config.set("unban.processing", "&e⏳ Procesando unban, esto puede tomar unos minutos...");


        config.set("kick.banned-ip", "&c&lIPDynamic\n\n&fTu IP ha sido baneada del servidor\n&7Razón: &e{reason}\n\n&7Si crees que esto es un error,\n&7contacta con un administrador.");


        config.set("alts.no-alts-found", "&a✅ No se encontraron cuentas alternativas.");
        config.set("alts.found", "&e🔍 Se encontraron &c{count} &eposibles alts:");
        config.set("alts.detection", "&c[⚠] &6{player} &ftiene &c{count} &fposibles alts.");


        config.set("info.player-info", "&b📊 Información de {player}:");
        config.set("info.first-login", "&7Primera conexión: &f{date}");
        config.set("info.last-login", "&7Última conexión: &f{date}");
        config.set("info.total-connections", "&7Conexiones totales: &f{count}");
        config.set("info.current-ip", "&7IP actual: &f{ip}");
        config.set("info.ip-history", "&7IPs históricas: &f{count}");


        config.set("stats.title", "&b📊 Estadísticas de IPDynamic:");
        config.set("stats.total-players", "&7Jugadores totales: &f{count}");
        config.set("stats.unique-ips", "&7IPs únicas: &f{count}");
        config.set("stats.single-bans", "&7Bans simples: &f{count}");
        config.set("stats.op1-bans", "&7Bans OP1: &f{count}");
        config.set("stats.op2-bans", "&7Bans OP2: &f{count}");


        config.set("notify.first-join", "&a[+] &e{player} &fse conectó por primera vez desde &e{ip}");
        config.set("notify.reconnection", "&a[+] &e{player} &fse reconectó desde &e{ip}");
        config.set("notify.multiple-ips", "&a[+] &e{player} &fse conectó desde &e{ip} &7(tiene {count} IPs)");
        config.set("notify.admin-login", "&6[👑] &eAdmin &6{player} &fse conectó desde &e{ip}");


        config.set("help.title", "&b✨ IPDynamic 2.5-OMEGA - Ayuda ✨");
        config.set("help.ban-op1", "&e/ipdy ban op1 <IP> [razón] &7- Ban OP1 (256 IPs)");
        config.set("help.ban-op2", "&e/ipdy ban op2 <IP> [razón] &7- Ban OP2 (65K IPs)");
        config.set("help.unban-op1", "&e/ipdy unban op1 <IP> &7- Unban OP1");
        config.set("help.unban-op2", "&e/ipdy unban op2 <IP> &7- Unban OP2");
        config.set("help.alts", "&e/ipdy alts <jugador> &7- Ver alts de un jugador");
        config.set("help.info", "&e/ipdy info <jugador> &7- Info detallada de jugador");
        config.set("help.stats", "&e/ipdy stats &7- Estadísticas del plugin");
        config.set("help.reload", "&e/ipdy reload &7- Recargar plugin");


        config.set("webhook.connection.title", "🔗 Nueva Conexión");
        config.set("webhook.connection.first-join", "✨ Primera Conexión");
        config.set("webhook.alt.title", "🔍 Posibles Alts Detectados");
        config.set("webhook.ban.title", "⛔ Ban de IP Aplicado");
        config.set("webhook.unban.title", "✅ Unban de IP Aplicado");
        config.set("webhook.admin.title", "👑 Conexión de Administrador");
    }

    private void createEnglishMessages(YamlConfiguration config) {

        config.set("prefix", "&8[&bIPDynamic&8]");
        config.set("no-permission", "&cYou don't have permission to use this command.");
        config.set("player-not-found", "&cPlayer not found.");
        config.set("invalid-ip", "&cInvalid IP format.");
        config.set("reload-success", "&aPlugin reloaded successfully!");


        config.set("ban.success", "&a✅ Ban applied successfully!");
        config.set("ban.already-banned", "&cThis IP/pattern is already banned.");
        config.set("ban.invalid-type", "&cInvalid ban type. Use: op1, op2");
        config.set("ban.reason-default", "Suspicious behavior");


        config.set("unban.success", "&a✅ Unban applied successfully!");
        config.set("unban.not-banned", "&cThis IP/pattern is not banned.");
        config.set("unban.processing", "&e⏳ Processing unban, this may take a few minutes...");


        config.set("kick.banned-ip", "&c&lIPDynamic\n\n&fYour IP has been banned from the server\n&7Reason: &e{reason}\n\n&7If you think this is an error,\n&7contact an administrator.");


        config.set("alts.no-alts-found", "&a✅ No alternative accounts found.");
        config.set("alts.found", "&e🔍 Found &c{count} &epossible alts:");
        config.set("alts.detection", "&c[⚠] &6{player} &fhas &c{count} &fpossible alts.");


        config.set("info.player-info", "&b📊 Information for {player}:");
        config.set("info.first-login", "&7First login: &f{date}");
        config.set("info.last-login", "&7Last login: &f{date}");
        config.set("info.total-connections", "&7Total connections: &f{count}");
        config.set("info.current-ip", "&7Current IP: &f{ip}");
        config.set("info.ip-history", "&7Historical IPs: &f{count}");


        config.set("stats.title", "&b📊 IPDynamic Statistics:");
        config.set("stats.total-players", "&7Total players: &f{count}");
        config.set("stats.unique-ips", "&7Unique IPs: &f{count}");
        config.set("stats.single-bans", "&7Single bans: &f{count}");
        config.set("stats.op1-bans", "&7OP1 bans: &f{count}");
        config.set("stats.op2-bans", "&7OP2 bans: &f{count}");


        config.set("notify.first-join", "&a[+] &e{player} &fconnected for the first time from &e{ip}");
        config.set("notify.reconnection", "&a[+] &e{player} &freconnected from &e{ip}");
        config.set("notify.multiple-ips", "&a[+] &e{player} &fconnected from &e{ip} &7(has {count} IPs)");
        config.set("notify.admin-login", "&6[👑] &eAdmin &6{player} &fconnected from &e{ip}");


        config.set("help.title", "&b✨ IPDynamic 2.5-OMEGA - Help ✨");
        config.set("help.ban-op1", "&e/ipdy ban op1 <IP> [reason] &7- OP1 ban (256 IPs)");
        config.set("help.ban-op2", "&e/ipdy ban op2 <IP> [reason] &7- OP2 ban (65K IPs)");
        config.set("help.unban-op1", "&e/ipdy unban op1 <IP> &7- OP1 unban");
        config.set("help.unban-op2", "&e/ipdy unban op2 <IP> &7- OP2 unban");
        config.set("help.alts", "&e/ipdy alts <player> &7- View player's alts");
        config.set("help.info", "&e/ipdy info <player> &7- Detailed player info");
        config.set("help.stats", "&e/ipdy stats &7- Plugin statistics");
        config.set("help.reload", "&e/ipdy reload &7- Reload plugin");


        config.set("webhook.connection.title", "🔗 New Connection");
        config.set("webhook.connection.first-join", "✨ First Connection");
        config.set("webhook.alt.title", "🔍 Possible Alts Detected");
        config.set("webhook.ban.title", "⛔ IP Ban Applied");
        config.set("webhook.unban.title", "✅ IP Unban Applied");
        config.set("webhook.admin.title", "👑 Administrator Connection");
    }

    private void cacheMessages() {
        messageCache.clear();
        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isString(key)) {
                messageCache.put(key, langConfig.getString(key));
            }
        }
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getRawString(key);
        if (message == null) return "Missing message: " + key;


        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return ColorUtils.translateColor(message);
    }

    public String getMessage(String key) {
        return getMessage(key, null);
    }

    public String getRawString(String key) {
        return messageCache.getOrDefault(key, langConfig.getString(key, "Missing: " + key));
    }

    public java.util.List<String> getMessageLines(String key) {
        return langConfig.getStringList(key);
    }

    public void reloadLanguage() {
        loadLanguageFile(currentLang + ".yml");
    }

    public String getCurrentLanguage() {
        return currentLang;
    }
}