package de.murmelmeister.murmelapi.punishment.audit;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.net.InetAddress;
import java.time.Duration;
import java.util.*;

public class PunishmentLogCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<UUID, PunishmentLog> cacheById;
    private final LoadingCache<Integer, List<PunishmentLog>> cacheByUser;
    private final LoadingCache<InetAddress, List<PunishmentLog>> cacheByIp;
    private final LoadingCache<String, List<PunishmentLog>> listCache;
    private final Long fetchLimit;

    public PunishmentLogCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapcity, refreshInterval);
        this.cacheByUser = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapcity, refreshInterval);
        this.cacheByIp = CacheUtil.buildCacheRefresh(this::loadByIpAddress, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.PUNISHMENT_LOGS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_PUNISHMENT_LOG.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof UUID logId)
                    refreshSingle(logId);
            } else {
                UUID logId = UUID.fromString((String) key);
                refreshSingle(logId);
            }
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        clear();
    }

    private void refreshAll() {
        clear();
        List<PunishmentLog> logs = loadAllFromDatabase();
        if (logs.isEmpty())
            return;

        Map<Integer, List<PunishmentLog>> byUser = new HashMap<>();
        Map<InetAddress, List<PunishmentLog>> byIp = new HashMap<>();

        for (PunishmentLog log : logs) {
            cacheById.put(log.id(), log);
            Integer userId = log.userId();
            if (userId != null)
                byUser.computeIfAbsent(userId, ignored -> new ArrayList<>()).add(log);
            InetAddress inetAddress = log.inetAddress();
            if (inetAddress != null)
                byIp.computeIfAbsent(inetAddress, ignored -> new ArrayList<>()).add(log);
        }

        byUser.forEach((userId, userLogs) -> cacheByUser.put(userId, List.copyOf(userLogs)));
        byIp.forEach((ip, ipLogs) -> cacheByIp.put(ip, List.copyOf(ipLogs)));
        listCache.put(ALL_KEY, List.copyOf(logs));
    }

    private void refreshSingle(UUID logId) {
        remove(logId);
        PunishmentLog log = loadById(logId);
        if (log != null)
            put(log);
    }

    private List<PunishmentLog> loadAllFromDatabase() {
        // Note: IDK if this is the best order, but it makes sense to have the latest logs first
        String sql = "SELECT * FROM " + tableName + " ORDER BY created_at DESC";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentLog());
    }

    private List<PunishmentLog> loadByUserId(int userId) {
        // Note: IDK if this is the best order, but it makes sense to have the latest logs first
        String sql = "SELECT * FROM " + tableName + " WHERE user_id = ? ORDER BY created_at DESC";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentLog(),
                stmt -> stmt.setInt(1, userId));
    }

    private List<PunishmentLog> loadByIpAddress(InetAddress inetAddress) {
        // Note: IDK if this is the best order, but it makes sense to have the latest logs first
        String sql = "SELECT * FROM " + tableName + " WHERE ip_address = ? ORDER BY created_at DESC";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentLog(),
                stmt -> stmt.setString(1, inetAddress.getHostAddress()));
    }

    private PunishmentLog loadById(UUID id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.punishmentLog(),
                stmt -> stmt.setString(1, id.toString()));
    }

    public PunishmentLog getById(UUID logId) {
        return cacheById.get(logId);
    }

    public List<PunishmentLog> getByUser(int userId) {
        return cacheByUser.get(userId);
    }

    public List<PunishmentLog> getByIp(InetAddress inetAddress) {
        return cacheByIp.get(inetAddress);
    }

    public void put(PunishmentLog log) {
        UUID logId = log.id();
        cacheById.put(logId, log);
        CacheUtil.put(cacheByUser, log.userId(), log, v -> v.id().equals(logId));
        CacheUtil.put(cacheByIp, log.inetAddress(), log, v -> v.id().equals(logId));
        CacheUtil.put(listCache, ALL_KEY, log, v -> v.id().equals(logId));
    }

    public void remove(UUID logId) {
        PunishmentLog log = cacheById.getIfPresent(logId);
        if (log != null) {
            cacheById.invalidate(logId);
            CacheUtil.remove(cacheByUser, log.userId(), v -> v.id().equals(logId));
            CacheUtil.remove(cacheByIp, log.inetAddress(), v -> v.id().equals(logId));
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id().equals(logId));
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByUser.invalidateAll();
        cacheByIp.invalidateAll();
        listCache.invalidateAll();
    }

    public List<PunishmentLog> getCachedPunishLogs() {
        List<PunishmentLog> logs = listCache.get(ALL_KEY);
        if (logs == null || logs.isEmpty())
            return Collections.emptyList();
        return List.copyOf(logs);
    }
}
