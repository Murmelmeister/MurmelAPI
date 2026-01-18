package de.murmelmeister.murmelapi.punishment.reason;

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

import java.time.Duration;
import java.util.*;

public class PunishmentReasonCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_TYPE_ID = "SELECT * FROM %s WHERE type_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull Integer, Optional<PunishmentReason>> cacheById;
    private final LoadingCache<@NotNull Integer, List<PunishmentReason>> cacheByType;
    private final LoadingCache<@NotNull String, List<PunishmentReason>> listCache;

    public PunishmentReasonCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapcity, refreshInterval);
        this.cacheByType = CacheUtil.buildCacheRefresh(this::loadByType, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.PUNISHMENT_REASONS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_PUNISHMENT_REASON.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof Integer reasonId)
                    remove(reasonId);
            } else {
                int reasonId = Integer.parseInt((String) key);
                remove(reasonId);
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<PunishmentReason> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentReason());
    }

    private @NotNull List<PunishmentReason> loadByType(int typeId) {
        String sql = SELECT_BY_TYPE_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentReason(),
                stmt -> stmt.setInt(1, typeId));
    }

    private @NotNull Optional<PunishmentReason> loadById(int reasonId) {
        String sql = SELECT_BY_ID.formatted(tableName);
        PunishmentReason punishmentReason = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.punishmentReason(),
                stmt -> stmt.setInt(1, reasonId));

        return Optional.ofNullable(punishmentReason);
    }

    public @Nullable PunishmentReason getById(int reasonId) {
        Optional<PunishmentReason> optReason = cacheById.get(reasonId);
        return optReason != null && optReason.isPresent() ? optReason.orElse(null) : null;
    }

    public @Nullable List<PunishmentReason> getByType(int typeId) {
        return cacheByType.get(typeId);
    }

    public void put(@Nullable PunishmentReason reason) {
        if (reason == null) return;
        int reasonId = reason.id();
        cacheById.put(reasonId, Optional.of(reason));
        CacheUtil.put(cacheByType, reason.typeId(), reason, v -> v.id() == reasonId);
        CacheUtil.put(listCache, ALL_KEY, reason, v -> v.id() == reasonId);
    }

    public void remove(int reasonId) {
        Optional<PunishmentReason> optReason = cacheById.getIfPresent(reasonId);
        cacheById.invalidate(reasonId);

        if (optReason != null && optReason.isPresent()) {
            PunishmentReason reason = optReason.get();
            CacheUtil.remove(cacheByType, reason.typeId(), v -> v.id() == reasonId);
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == reasonId);
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByType.invalidateAll();
        listCache.invalidateAll();
    }

    public @NotNull List<PunishmentReason> getCachedPunishReasons() {
        List<PunishmentReason> reasons = listCache.get(ALL_KEY);
        if (reasons == null || reasons.isEmpty())
            return Collections.emptyList();
        return List.copyOf(reasons);
    }
}
