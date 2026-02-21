package de.murmelmeister.murmelapi.punishment.user;

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
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PunishmentUserCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PunishmentUserCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_TYPE_ID = "SELECT * FROM %s WHERE type_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE user_id = ? AND type_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull PunishKey, Optional<PunishmentUser>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<PunishmentUser>> cacheByType;
    private final LoadingCache<@NotNull String, List<PunishmentUser>> listCache;

    public PunishmentUserCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadFromDatabase, cacheCapacity, refreshInterval);
        this.cacheByType = CacheUtil.buildCacheRefresh(this::loadByType, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.PUNISHMENT_USERS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_PUNISHMENT_USER.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof PunishKey punishKey)
                remove(punishKey);
            else if (key instanceof String json) {
                try {
                    final PunishKey punishKey = gson.fromJson(json, PunishKey.class);

                    if (punishKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(punishKey);
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

    private @NotNull List<PunishmentUser> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentUser());
    }

    private @NotNull List<PunishmentUser> loadByType(int typeId) {
        String sql = SELECT_BY_TYPE_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentUser(),
                stmt -> stmt.setInt(1, typeId));
    }

    private @NotNull Optional<PunishmentUser> loadFromDatabase(PunishKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        PunishmentUser punishmentUser = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.punishmentUser(),
                stmt -> {
                    stmt.setString(1, key.userId().toString());
                    stmt.setInt(2, key.typeId());
                });

        return Optional.ofNullable(punishmentUser);
    }

    public @Nullable PunishmentUser get(@NotNull UUID userId, int typeId) {
        Optional<PunishmentUser> optUser = cacheByKey.get(new PunishKey(userId, typeId));
        return optUser != null && optUser.isPresent() ? optUser.orElse(null) : null;
    }

    public @NotNull @Unmodifiable List<PunishmentUser> getByTypeId(int typeId) {
        List<PunishmentUser> users = cacheByType.get(typeId);
        if (users == null || users.isEmpty())
            return Collections.emptyList();
        return List.copyOf(users);
    }

    public @NotNull @Unmodifiable List<PunishmentUser> getAll() {
        List<PunishmentUser> users = listCache.get(ALL_KEY);
        if (users == null || users.isEmpty())
            return Collections.emptyList();
        return List.copyOf(users);
    }

    public void remove(@NotNull PunishKey key) {
        cacheByKey.invalidate(key);
        cacheByType.invalidate(key.typeId());
        CacheUtil.remove(listCache, ALL_KEY, v -> v.userId().equals(key.userId()) && v.typeId() == key.typeId());
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByType.invalidateAll();
        listCache.invalidateAll();
    }

    public record PunishKey(@NotNull UUID userId, int typeId) {
    }
}
