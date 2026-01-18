package de.murmelmeister.murmelapi.punishment.audit;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.Duration;
import java.util.*;

public class PunishmentLogCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s ORDER BY created_at DESC";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_USER_ID = "SELECT * FROM %s WHERE user_id = ? ORDER BY created_at DESC";
    @Language("MariaDB")
    private static final String SELECT_BY_IP_ADDRESS = "SELECT * FROM %s WHERE ip_address = ? ORDER BY created_at DESC";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull UUID, Optional<PunishmentLog>> cacheById;
    private final LoadingCache<@NotNull Integer, List<PunishmentLog>> cacheByUser;
    private final LoadingCache<@NotNull InetAddress, List<PunishmentLog>> cacheByIp;
    private final LoadingCache<@NotNull String, List<PunishmentLog>> listCache;

    public PunishmentLogCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapcity, refreshInterval);
        this.cacheByUser = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapcity, refreshInterval);
        this.cacheByIp = CacheUtil.buildCacheRefresh(this::loadByIpAddress, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.PUNISHMENT_LOGS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_PUNISHMENT_LOG.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof UUID logId)
                    remove(logId);
            } else {
                UUID logId = UUID.fromString((String) key);
                remove(logId);
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<PunishmentLog> loadAllFromDatabase() {
        // Note: IDK if this is the best order, but it makes sense to have the latest logs first
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentLog());
    }

    private @NotNull List<PunishmentLog> loadByUserId(int userId) {
        // Note: IDK if this is the best order, but it makes sense to have the latest logs first
        String sql = SELECT_BY_USER_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentLog(),
                stmt -> stmt.setInt(1, userId));
    }

    private @NotNull List<PunishmentLog> loadByIpAddress(InetAddress inetAddress) {
        // Note: IDK if this is the best order, but it makes sense to have the latest logs first
        String sql = SELECT_BY_IP_ADDRESS.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentLog(),
                stmt -> stmt.setString(1, inetAddress.getHostAddress()));
    }

    private @NotNull Optional<PunishmentLog> loadById(UUID id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        PunishmentLog punishmentLog = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.punishmentLog(),
                stmt -> stmt.setString(1, id.toString()));

        return Optional.ofNullable(punishmentLog);
    }

    public @Nullable PunishmentLog getById(@Nullable UUID logId) {
        if (logId == null) return null;
        Optional<PunishmentLog> optLog = cacheById.get(logId);
        return optLog != null && optLog.isPresent() ? optLog.orElse(null) : null;
    }

    public @Nullable List<PunishmentLog> getByUser(int userId) {
        return cacheByUser.get(userId);
    }

    public @Nullable List<PunishmentLog> getByIp(@NotNull InetAddress inetAddress) {
        return cacheByIp.get(inetAddress);
    }

    public void put(@Nullable PunishmentLog log) {
        if (log == null) return;
        UUID logId = log.id();
        cacheById.put(logId, Optional.of(log));
        if (log.userId() != null) CacheUtil.put(cacheByUser, log.userId(), log, v -> v.id().equals(logId));
        if (log.inetAddress() != null) CacheUtil.put(cacheByIp, log.inetAddress(), log, v -> v.id().equals(logId));
        CacheUtil.put(listCache, ALL_KEY, log, v -> v.id().equals(logId));
    }

    public void remove(@NotNull UUID logId) {
        Optional<PunishmentLog> optLog = cacheById.getIfPresent(logId);
        cacheById.invalidate(logId);

        if (optLog != null && optLog.isPresent()) {
            PunishmentLog log = optLog.get();
            if (log.userId() != null) CacheUtil.remove(cacheByUser, log.userId(), v -> v.id().equals(logId));
            if (log.inetAddress() != null) CacheUtil.remove(cacheByIp, log.inetAddress(), v -> v.id().equals(logId));
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id().equals(logId));
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByUser.invalidateAll();
        cacheByIp.invalidateAll();
        listCache.invalidateAll();
    }

    public @NotNull List<PunishmentLog> getCachedPunishLogs() {
        List<PunishmentLog> logs = listCache.get(ALL_KEY);
        if (logs == null || logs.isEmpty())
            return Collections.emptyList();
        return List.copyOf(logs);
    }
}
