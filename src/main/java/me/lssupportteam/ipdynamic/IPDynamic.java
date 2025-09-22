package me.lssupportteam.ipdynamic;

import me.lssupportteam.ipdynamic.commands.CommandManager;
import me.lssupportteam.ipdynamic.discord.DiscordManager;
import me.lssupportteam.ipdynamic.listeners.PlayerConnectionListener;
import me.lssupportteam.ipdynamic.managers.*;
import me.lssupportteam.ipdynamic.managers.ConfigMigrator;
import me.lssupportteam.ipdynamic.nms.NMSVersion;
import me.lssupportteam.ipdynamic.services.GeoIPService;
import me.lssupportteam.ipdynamic.services.WebhookService;
import me.lssupportteam.ipdynamic.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class IPDynamic extends JavaPlugin {

    private static IPDynamic instance;
    private NMSVersion nmsVersion;
    private ExecutorService executorService;


    private ConfigManager configManager;
    private DataManager dataManager;
    private BanManager banManager;
    private WhitelistManager whitelistManager;
    private LangManager langManager;
    private WebhookConfigManager webhookConfigManager;
    private AddonsManager addonsManager;
    private ConfigMigrator configMigrator;


    private GeoIPService geoIPService;
    private WebhookService webhookService;
    private DiscordManager discordManager;


    private BukkitTask autosaveTask;
    private BukkitTask banProcessTask;
    private BukkitTask unbanProcessTask;

    @Override
    public void onEnable() {
        instance = this;


        nmsVersion = new NMSVersion();
        if (!nmsVersion.isVersionSupported()) {
            getLogger().severe("═══════════════════════════════════════════");
            getLogger().severe(" IPDynamic 2.5-OMEGA - VERSION NO SOPORTADA");
            getLogger().severe(" Versión detectada: " + nmsVersion.getFullVersionInfo());
            getLogger().severe(" Versiones soportadas: 1.13 - 1.21.8");
            getLogger().severe("═══════════════════════════════════════════");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }


        executorService = Executors.newFixedThreadPool(4);

        printEnableMessage();


        initializeManagers();




        loadAllData();


        registerListeners();


        registerCommands();


        setupTasks();

        getLogger().info(ColorUtils.translateColor(langManager.getMessage("system.plugin-enabled")));
    }

    @Override
    public void onDisable() {
        printDisableMessage();


        if (autosaveTask != null && !autosaveTask.isCancelled()) {
            autosaveTask.cancel();
        }
        if (banProcessTask != null && !banProcessTask.isCancelled()) {
            banProcessTask.cancel();
        }
        if (unbanProcessTask != null && !unbanProcessTask.isCancelled()) {
            unbanProcessTask.cancel();
        }


        saveAllData();


        if (discordManager != null) {
            discordManager.shutdown();
        }

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }

        getLogger().info(langManager.getMessage("system.plugin-disabled"));
        instance = null;
    }

    private void initializeManagers() {

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Initialize migrator first to update configurations
        configMigrator = new ConfigMigrator(this);
        configMigrator.migrateAllConfigs();

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        webhookConfigManager = new WebhookConfigManager(this);
        webhookConfigManager.loadConfig();

        addonsManager = new AddonsManager(this);
        addonsManager.initialize();

        langManager = new LangManager(this);
        langManager.loadLanguageFile(configManager.getLangFileName());

        dataManager = new DataManager(this);
        banManager = new BanManager(this);
        whitelistManager = new WhitelistManager(this);


        webhookService = new WebhookService(this);

        discordManager = new DiscordManager(this);
        discordManager.initialize();

        String geoIpProvider = configManager.getGeoIpProvider().toLowerCase();
        if ("ip-api.com".equals(geoIpProvider)) {
            geoIPService = new GeoIPService(this);
        }
    }

    private void loadAllData() {
        dataManager.loadAllData();
        banManager.loadBans();
        whitelistManager.loadWhitelist();

        getLogger().info(langManager.getMessage("system.data-loaded"));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
    }

    private void registerCommands() {
        CommandManager commandManager = new CommandManager(this);
        getCommand("ipdynamic").setExecutor(commandManager);
        getCommand("ipdynamic").setTabCompleter(commandManager);
        getCommand("ipdy").setExecutor(commandManager);
        getCommand("ipdy").setTabCompleter(commandManager);
    }

    private void setupTasks() {

        int autosaveInterval = configManager.getAutosaveInterval();
        if (autosaveInterval > 0) {
            long intervalTicks = autosaveInterval * 60 * 20L;
            autosaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                this::saveAllData, intervalTicks, intervalTicks);
        }


        if (banManager.hasPendingOp2Bans()) {
            long processInterval = 5 * 60 * 20L; // 5 minutos
            banProcessTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> banManager.processPendingOp2Bans(), processInterval, processInterval);
        }


        if (banManager.hasPendingUnbans()) {
            long processInterval = 5 * 60 * 20L; // 5 minutos
            unbanProcessTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> banManager.processPendingUnbans(), processInterval, processInterval);
        }

        if (discordManager != null) {
            discordManager.scheduleStatsUpdate();
        }
    }

    public void saveAllData() {
        executorService.submit(() -> {
            dataManager.saveAllData();
            banManager.saveBans();
            whitelistManager.saveWhitelist();

            if (configManager.isDebugMode()) {
                getLogger().info(langManager.getMessage("system.data-saved"));
            }
        });
    }

    public void reloadPlugin() {

        if (autosaveTask != null && !autosaveTask.isCancelled()) {
            autosaveTask.cancel();
        }
        if (banProcessTask != null && !banProcessTask.isCancelled()) {
            banProcessTask.cancel();
        }
        if (unbanProcessTask != null && !unbanProcessTask.isCancelled()) {
            unbanProcessTask.cancel();
        }


        configManager.loadConfig();
        webhookConfigManager.loadConfig();
        addonsManager.reloadAllAddons();
        langManager.loadLanguageFile(configManager.getLangFileName());


        loadAllData();

        if (discordManager != null) {
            discordManager.reload();
        }


        String geoIpProvider = configManager.getGeoIpProvider().toLowerCase();
        if ("ip-api.com".equals(geoIpProvider) && geoIPService == null) {
            geoIPService = new GeoIPService(this);
        } else if (!"ip-api.com".equals(geoIpProvider) && geoIPService != null) {
            geoIPService = null;
        }


        setupTasks();

        getLogger().info(langManager.getMessage("system.plugin-reloaded"));
    }


    private void printEnableMessage() {
        String[] logo = {
            "",
            "&b      ╭─────────────── &fIPDynamic 2.5-OMEGA &b───────────────╮",
            "&b      │                                                   ",
            "&b      │        &f___ ____  ____                              _  ",
            "&b      │       &f|_ _|  _ \\|  _ \\ _   _ _ __   __ _ _ __ ___ (_) ",
            "&b      │        &f| || |_) | | | | | | | '_ \\ / _` | '_ ` _ \\| | ",
            "&b      │        &f| ||  __/| |_| | |_| | | | | (_| | | | | | | | ",
            "&b      │       &f|___|_|   |____/ \\__, |_| |_|\\__,_|_| |_| |_|_| ",
            "&b      │                        &f|___/                          ",
            "&b      │                                                   ",
            "&b      │  &e➣ &fVersión: &a2.5-OMEGA                      ",
            "&b      │  &e➣ &fAutor: &aSrCodex                          ",
            "&b      │  &e➣ &fServidor: &a" + nmsVersion.getFullVersionInfo(),
            "&b      │  &e➣ &fEstado: &a✓ Iniciando...                   ",
            "&b      │                                                   ",
            "&b      ╰───────────────────────────────────────────────────╯",
            ""
        };

        for (String line : logo) {
            Bukkit.getConsoleSender().sendMessage(ColorUtils.translateColor(line));
        }
    }

    private void printDisableMessage() {
        String[] logo = {
            "",
            "&c      ╭─────────────── &fIPDynamic 2.5-OMEGA &c───────────────╮",
            "&c      │                                                   ",
            "&c      │              &fDeshabilitando plugin...            ",
            "&c      │              &fGuardando todos los datos...       ",
            "&c      │                                                   ",
            "&c      ╰───────────────────────────────────────────────────╯",
            ""
        };

        for (String line : logo) {
            Bukkit.getConsoleSender().sendMessage(ColorUtils.translateColor(line));
        }
    }


    public static IPDynamic getInstance() { return instance; }
    public NMSVersion getNMSVersion() { return nmsVersion; }
    public ExecutorService getExecutorService() { return executorService; }
    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public BanManager getBanManager() { return banManager; }
    public WhitelistManager getWhitelistManager() { return whitelistManager; }
    public LangManager getLangManager() { return langManager; }
    public WebhookConfigManager getWebhookConfigManager() { return webhookConfigManager; }
    public AddonsManager getAddonsManager() { return addonsManager; }
    public GeoIPService getGeoIPService() { return geoIPService; }
    public WebhookService getWebhookService() { return webhookService; }
    public DiscordManager getDiscordManager() { return discordManager; }
    public ConfigMigrator getConfigMigrator() { return configMigrator; }
    public File getPluginDataFolder() { return getDataFolder(); }
}