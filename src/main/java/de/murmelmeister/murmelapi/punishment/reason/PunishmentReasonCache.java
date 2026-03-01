package de.murmelmeister.murmelapi.punishment.reason;

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
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PunishmentReasonCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PunishmentReasonCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_TYPE_ID = "SELECT * FROM %s WHERE type_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull Integer, Optional<PunishmentReason>> cacheById;
    private final LoadingCache<@NotNull Integer, List<PunishmentReason>> cacheByType;
    private final LoadingCache<@NotNull String, List<PunishmentReason>> listCache;

    public PunishmentReasonCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByType = CacheUtil.buildCacheRefresh(this::loadByType, cacheCapacity, refreshInterval);
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
            if (key instanceof PunishmentReason reason)
                remove(reason);
            else if (key instanceof String json) {
                try {
                    final PunishmentReason reason = gson.fromJson(json, PunishmentReason.class);

                    if (reason == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(reason);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single refresh: {}", json, e);
                }
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

    public @NotNull Optional<PunishmentReason> getById(int reasonId) {
        return cacheById.get(reasonId);
    }

    public @NotNull @Unmodifiable List<PunishmentReason> getByType(int typeId) {
        List<PunishmentReason> reasons = cacheByType.get(typeId);
        if (reasons == null || reasons.isEmpty())
            return Collections.emptyList();
        return List.copyOf(reasons);
    }

    public @NotNull @Unmodifiable List<PunishmentReason> getAll() {
        List<PunishmentReason> reasons = listCache.get(ALL_KEY);
        if (reasons == null || reasons.isEmpty())
            return Collections.emptyList();
        return List.copyOf(reasons);
    }

    public void remove(@NotNull PunishmentReason reason) {
        cacheById.invalidate(reason.id());
        cacheByType.invalidate(reason.typeId());
        listCache.invalidate(ALL_KEY);
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByType.invalidateAll();
        listCache.invalidateAll();
    }
}
