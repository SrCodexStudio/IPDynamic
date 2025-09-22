package me.lssupportteam.ipdynamic.managers;

import com.google.gson.reflect.TypeToken;
import me.lssupportteam.ipdynamic.IPDynamic;
import me.lssupportteam.ipdynamic.models.BanEntry;
import me.lssupportteam.ipdynamic.utils.IPUtils;
import me.lssupportteam.ipdynamic.utils.JsonUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class BanManager {

    private final IPDynamic plugin;
    private final File op1BansFile;
    private final File op2BansFile;
    private final File singleBansFile;


    private final ConcurrentHashMap<String, BanEntry> singleBans;
    private final ConcurrentHashMap<String, BanEntry> op1Bans;
    private final ConcurrentHashMap<String, BanEntry> op2Bans;


    private final ConcurrentLinkedQueue<BanEntry> pendingOp2Bans;
    private final ConcurrentLinkedQueue<UnbanRequest> pendingUnbans;


    private final Set<String> bannedIpCache;
    private final ReadWriteLock cacheLock;


    private int totalBansProcessed = 0;
    private int totalUnbansProcessed = 0;

    public BanManager(IPDynamic plugin) {
        this.plugin = plugin;
        this.op1BansFile = new File(plugin.getPluginDataFolder(), "data/op1-bans.json");
        this.op2BansFile = new File(plugin.getPluginDataFolder(), "data/op2-bans.json");
        this.singleBansFile = new File(plugin.getPluginDataFolder(), "data/single-bans.json");

        this.singleBans = new ConcurrentHashMap<>();
        this.op1Bans = new ConcurrentHashMap<>();
        this.op2Bans = new ConcurrentHashMap<>();

        this.pendingOp2Bans = new ConcurrentLinkedQueue<>();
        this.pendingUnbans = new ConcurrentLinkedQueue<>();

        this.bannedIpCache = ConcurrentHashMap.newKeySet();
        this.cacheLock = new ReentrantReadWriteLock();

        ensureDataDirectory();
    }

    private void ensureDataDirectory() {
        File dataDir = new File(plugin.getPluginDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    public void loadBans() {
        loadSingleBans();
        loadOp1Bans();
        loadOp2Bans();
        rebuildCache();

        plugin.getLogger().info(plugin.getLangManager().getMessage("bans.loaded")
            .replace("{single}", String.valueOf(singleBans.size()))
            .replace("{op1}", String.valueOf(op1Bans.size()))
            .replace("{op2}", String.valueOf(op2Bans.size())));
    }

    private void loadSingleBans() {
        Type type = JsonUtils.getTypeTokenMap(String.class, BanEntry.class);
        Map<String, BanEntry> loaded = JsonUtils.loadData(singleBansFile, type, plugin.getLogger());
        if (loaded != null) {
            singleBans.putAll(loaded);
        }
    }

    private void loadOp1Bans() {
        Type type = JsonUtils.getTypeTokenMap(String.class, BanEntry.class);
        Map<String, BanEntry> loaded = JsonUtils.loadData(op1BansFile, type, plugin.getLogger());
        if (loaded != null) {
            op1Bans.putAll(loaded);
        }
    }

    private void loadOp2Bans() {
        Type type = JsonUtils.getTypeTokenMap(String.class, BanEntry.class);
        Map<String, BanEntry> loaded = JsonUtils.loadData(op2BansFile, type, plugin.getLogger());
        if (loaded != null) {
            op2Bans.putAll(loaded);
        }
    }

    public void saveBans() {
        JsonUtils.saveData(singleBansFile, new HashMap<>(singleBans), plugin.getLogger());
        JsonUtils.saveData(op1BansFile, new HashMap<>(op1Bans), plugin.getLogger());
        JsonUtils.saveData(op2BansFile, new HashMap<>(op2Bans), plugin.getLogger());
    }


    public CompletableFuture<BanResult> ban(String pattern, String reason, String bannedBy) {
        return CompletableFuture.supplyAsync(() -> {
            if (!IPUtils.isValidIpPattern(pattern)) {
                return new BanResult(false, "Patrón de IP inválido");
            }

            BanEntry entry = new BanEntry(pattern, reason, bannedBy);
            BanEntry.BanType type = entry.getBanType();

            switch (type) {
                case SINGLE:
                    return banSingle(entry);
                case OP1:
                    return banOp1(entry);
                case OP2:
                    return banOp2(entry);
                default:
                    return new BanResult(false, "Tipo de ban no soportado");
            }
        }, plugin.getExecutorService());
    }

    private BanResult banSingle(BanEntry entry) {
        String ip = entry.getPattern();
        if (singleBans.containsKey(ip)) {
            return new BanResult(false, "IP ya baneada");
        }

        singleBans.put(ip, entry);
        bannedIpCache.add(ip);
        totalBansProcessed++;

        // Save to JSON file immediately
        JsonUtils.saveData(singleBansFile, new HashMap<>(singleBans), plugin.getLogger());

        plugin.getLogger().info(String.format("IP baneada: %s por %s", ip, entry.getBannedBy()));
        return new BanResult(true, "IP baneada exitosamente", 1);
    }

    private BanResult banOp1(BanEntry entry) {
        String pattern = entry.getPattern();
        if (op1Bans.containsKey(pattern)) {
            return new BanResult(false, "Patrón OP1 ya baneado");
        }

        op1Bans.put(pattern, entry);

        // Save to JSON file immediately
        JsonUtils.saveData(op1BansFile, new HashMap<>(op1Bans), plugin.getLogger());

        List<String> ips = IPUtils.generateIpRange(pattern);
        int count = 0;

        cacheLock.writeLock().lock();
        try {
            for (String ip : ips) {
                if (bannedIpCache.add(ip)) {
                    count++;
                }
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        totalBansProcessed += count;
        plugin.getLogger().info(String.format("OP1 ban aplicado: %s (%d IPs) por %s",
            pattern, count, entry.getBannedBy()));

        return new BanResult(true, String.format("OP1 ban aplicado a %d IPs", count), count);
    }

    private BanResult banOp2(BanEntry entry) {
        String pattern = entry.getPattern();
        if (op2Bans.containsKey(pattern)) {
            return new BanResult(false, "Patrón OP2 ya baneado");
        }

        op2Bans.put(pattern, entry);

        // Save to JSON file immediately
        JsonUtils.saveData(op2BansFile, new HashMap<>(op2Bans), plugin.getLogger());

        pendingOp2Bans.offer(entry);

        int totalIps = IPUtils.countAffectedIps(pattern);
        plugin.getLogger().info(String.format("OP2 ban programado: %s (%d IPs) por %s",
            pattern, totalIps, entry.getBannedBy()));

        return new BanResult(true,
            String.format("OP2 ban programado para %d IPs. Se procesará progresivamente.", totalIps),
            totalIps);
    }


    public void processPendingOp2Bans() {
        if (pendingOp2Bans.isEmpty()) return;

        plugin.getExecutorService().submit(() -> {
            BanEntry entry = pendingOp2Bans.peek();
            if (entry == null) return;

            String pattern = entry.getPattern();
            String[] parts = pattern.split("\\.");


            int processedThisCycle = 0;
            int maxPerCycle = 256; // Procesar 256 IPs cada 5 minutos

            for (int i = 0; i < 256 && processedThisCycle < maxPerCycle; i++) {
                if (parts[2].equals("*")) {
                    String subPattern = parts[0] + "." + parts[1] + "." + i + ".*";
                    List<String> ips = IPUtils.generateIpRange(subPattern);

                    cacheLock.writeLock().lock();
                    try {
                        for (String ip : ips) {
                            bannedIpCache.add(ip);
                            processedThisCycle++;
                        }
                    } finally {
                        cacheLock.writeLock().unlock();
                    }
                }
            }

            totalBansProcessed += processedThisCycle;


            if (processedThisCycle < maxPerCycle) {
                pendingOp2Bans.poll();
                plugin.getLogger().info(String.format("OP2 ban completado: %s", pattern));
            } else {
                plugin.getLogger().info(String.format("OP2 ban en progreso: %s (%d IPs procesadas)",
                    pattern, processedThisCycle));
            }
        });
    }


    public CompletableFuture<UnbanResult> unban(String pattern) {
        return CompletableFuture.supplyAsync(() -> {
            BanEntry.BanType type = BanEntry.BanType.fromPattern(pattern);

            switch (type) {
                case SINGLE:
                    return unbanSingle(pattern);
                case OP1:
                    return unbanOp1(pattern);
                case OP2:
                    return unbanOp2(pattern);
                default:
                    return new UnbanResult(false, "Tipo de unban no soportado");
            }
        }, plugin.getExecutorService());
    }

    private UnbanResult unbanSingle(String ip) {
        BanEntry removed = singleBans.remove(ip);
        if (removed == null) {
            return new UnbanResult(false, "IP no está baneada");
        }

        bannedIpCache.remove(ip);
        totalUnbansProcessed++;

        // Save to JSON file immediately
        JsonUtils.saveData(singleBansFile, new HashMap<>(singleBans), plugin.getLogger());

        return new UnbanResult(true, "IP desbaneada exitosamente", 1);
    }

    private UnbanResult unbanOp1(String pattern) {
        BanEntry removed = op1Bans.remove(pattern);
        if (removed == null) {
            return new UnbanResult(false, "Patrón OP1 no está baneado");
        }

        // Save to JSON file immediately
        JsonUtils.saveData(op1BansFile, new HashMap<>(op1Bans), plugin.getLogger());

        pendingUnbans.offer(new UnbanRequest(pattern, BanEntry.BanType.OP1));

        return new UnbanResult(true, "Unban OP1 programado. Se procesará progresivamente.", 256);
    }

    private UnbanResult unbanOp2(String pattern) {
        BanEntry removed = op2Bans.remove(pattern);
        if (removed == null) {
            return new UnbanResult(false, "Patrón OP2 no está baneado");
        }

        // Save to JSON file immediately
        JsonUtils.saveData(op2BansFile, new HashMap<>(op2Bans), plugin.getLogger());

        pendingUnbans.offer(new UnbanRequest(pattern, BanEntry.BanType.OP2));

        int totalIps = IPUtils.countAffectedIps(pattern);
        return new UnbanResult(true,
            String.format("Unban OP2 programado para %d IPs. Se procesará progresivamente.", totalIps),
            totalIps);
    }


    public void processPendingUnbans() {
        if (pendingUnbans.isEmpty()) return;

        plugin.getExecutorService().submit(() -> {
            int processedThisCycle = 0;
            int maxPerCycle = 1000; // Procesar 1000 IPs cada 5 minutos

            while (!pendingUnbans.isEmpty() && processedThisCycle < maxPerCycle) {
                UnbanRequest request = pendingUnbans.peek();
                if (request == null) break;

                int processed = processUnbanRequest(request, maxPerCycle - processedThisCycle);
                processedThisCycle += processed;

                if (request.isComplete()) {
                    pendingUnbans.poll();
                    plugin.getLogger().info(String.format("Unban completado: %s", request.pattern));
                }
            }

            totalUnbansProcessed += processedThisCycle;
        });
    }

    private int processUnbanRequest(UnbanRequest request, int maxToProcess) {
        List<String> ipsToRemove = new ArrayList<>();
        int count = 0;

        if (request.type == BanEntry.BanType.OP1) {
            List<String> ips = IPUtils.generateIpRange(request.pattern);
            for (String ip : ips) {
                if (count >= maxToProcess) break;
                ipsToRemove.add(ip);
                count++;
            }
        }

        cacheLock.writeLock().lock();
        try {
            for (String ip : ipsToRemove) {
                bannedIpCache.remove(ip);
                request.processedCount++;
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        return count;
    }


    public boolean isBanned(String ip) {
        if (!IPUtils.isValidIpAddress(ip)) return false;


        if (bannedIpCache.contains(ip)) {
            return true;
        }


        for (String pattern : op1Bans.keySet()) {
            if (IPUtils.matches(ip, pattern)) {
                return true;
            }
        }

        for (String pattern : op2Bans.keySet()) {
            if (IPUtils.matches(ip, pattern)) {
                return true;
            }
        }

        return false;
    }

    public BanEntry getBanEntry(String ip) {
        BanEntry entry = singleBans.get(ip);
        if (entry != null) return entry;

        for (Map.Entry<String, BanEntry> e : op1Bans.entrySet()) {
            if (IPUtils.matches(ip, e.getKey())) {
                return e.getValue();
            }
        }

        for (Map.Entry<String, BanEntry> e : op2Bans.entrySet()) {
            if (IPUtils.matches(ip, e.getKey())) {
                return e.getValue();
            }
        }

        return null;
    }

    private void rebuildCache() {
        cacheLock.writeLock().lock();
        try {
            bannedIpCache.clear();
            bannedIpCache.addAll(singleBans.keySet());


            for (String pattern : op1Bans.keySet()) {
                List<String> ips = IPUtils.generateIpRange(pattern);
                bannedIpCache.addAll(ips);
            }


        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public boolean hasPendingOp2Bans() {
        return !pendingOp2Bans.isEmpty();
    }

    public boolean hasPendingUnbans() {
        return !pendingUnbans.isEmpty();
    }

    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("singleBans", singleBans.size());
        stats.put("op1Bans", op1Bans.size());
        stats.put("op2Bans", op2Bans.size());
        stats.put("pendingOp2", pendingOp2Bans.size());
        stats.put("pendingUnbans", pendingUnbans.size());
        stats.put("cachedIps", bannedIpCache.size());
        stats.put("totalProcessed", totalBansProcessed);
        stats.put("totalUnbanned", totalUnbansProcessed);
        return stats;
    }


    public static class BanResult {
        public final boolean success;
        public final String message;
        public final int affectedIps;

        public BanResult(boolean success, String message) {
            this(success, message, 0);
        }

        public BanResult(boolean success, String message, int affectedIps) {
            this.success = success;
            this.message = message;
            this.affectedIps = affectedIps;
        }
    }

    public static class UnbanResult {
        public final boolean success;
        public final String message;
        public final int affectedIps;

        public UnbanResult(boolean success, String message) {
            this(success, message, 0);
        }

        public UnbanResult(boolean success, String message, int affectedIps) {
            this.success = success;
            this.message = message;
            this.affectedIps = affectedIps;
        }
    }

    private static class UnbanRequest {
        final String pattern;
        final BanEntry.BanType type;
        int processedCount = 0;
        final int totalCount;

        UnbanRequest(String pattern, BanEntry.BanType type) {
            this.pattern = pattern;
            this.type = type;
            this.totalCount = IPUtils.countAffectedIps(pattern);
        }

        boolean isComplete() {
            return processedCount >= totalCount;
        }
    }
}