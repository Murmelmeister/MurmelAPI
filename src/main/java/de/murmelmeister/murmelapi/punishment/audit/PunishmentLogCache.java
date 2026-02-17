package de.murmelmeister.murmelapi.punishment.audit;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PunishmentLogCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PunishmentLogCache.class);

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

    public PunishmentLogCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByUser = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapacity, refreshInterval);
        this.cacheByIp = CacheUtil.buildCacheRefresh(this::loadByIpAddress, cacheCapacity, refreshInterval);
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
            if (key instanceof PunishmentLog log)
                remove(log);
            else if (key instanceof String json) {
                final Gson gson = new Gson();
                try {
                    final PunishmentLog log = gson.fromJson(json, PunishmentLog.class);
                    remove(log);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single punishment log refresh: {}", json, e);
                }
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

    public void remove(@NotNull PunishmentLog log) {
        cacheById.invalidate(log.id());
        if (log.userId() != null) CacheUtil.remove(cacheByUser, log.userId(), v -> v.id().equals(log.id()));
        if (log.inetAddress() != null) CacheUtil.remove(cacheByIp, log.inetAddress(), v -> v.id().equals(log.id()));
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id().equals(log.id()));
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
