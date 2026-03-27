package de.murmelmeister.murmelapi.group.color;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

final class GroupColorCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupColorCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP_ID = "SELECT * FROM %s WHERE group_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE group_id = ? AND type_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull ColorKey, Optional<GroupColor>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<GroupColor>> cacheByGroupId;
    private final LoadingCache<@NotNull String, List<GroupColor>> listCache;

    public GroupColorCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByGroupId = CacheUtil.buildCacheRefresh(this::loadByGroupId, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.GROUP_COLORS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_GROUP_COLOR.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof ColorKey colorKey)
                remove(colorKey);
            else if (key instanceof String json) {
                try {
                    final ColorKey colorKey = gson.fromJson(json, ColorKey.class);

                    if (colorKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(colorKey);
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

    private @NotNull List<GroupColor> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, GroupColorAdapter::resultSet);
    }

    private @NotNull List<GroupColor> loadByGroupId(int groupId) {
        String sql = SELECT_BY_GROUP_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, GroupColorAdapter::resultSet,
                stmt -> stmt.setInt(1, groupId));
    }

    private @NotNull Optional<GroupColor> loadByKey(ColorKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        GroupColor groupColor = CacheUtil.loadSingle(database, sql, fetchLimit, GroupColorAdapter::resultSet,
                stmt -> {
                    stmt.setInt(1, key.groupId());
                    if (key.typeId() != null) stmt.setInt(2, key.typeId());
                    else stmt.setNull(2, Types.INTEGER);
                });

        return Optional.ofNullable(groupColor);
    }

    public @NotNull Optional<GroupColor> get(int groupId, int typeId) {
        return cacheByKey.get(new ColorKey(groupId, typeId));
    }

    public @NotNull @Unmodifiable List<GroupColor> getByGroupId(int groupId) {
        List<GroupColor> colors = cacheByGroupId.get(groupId);
        if (colors == null || colors.isEmpty())
            return Collections.emptyList();
        return List.copyOf(colors);
    }

    public @NotNull @Unmodifiable List<GroupColor> getAll() {
        List<GroupColor> colors = listCache.get(ALL_KEY);
        if (colors == null || colors.isEmpty())
            return Collections.emptyList();
        return List.copyOf(colors);
    }

    public void remove(@NotNull ColorKey key) {
        cacheByKey.invalidate(key);
        cacheByGroupId.invalidate(key.groupId());
        listCache.invalidate(ALL_KEY);
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByGroupId.invalidateAll();
        listCache.invalidateAll();
    }

    record ColorKey(int groupId, @Nullable Integer typeId) {
    }
}
