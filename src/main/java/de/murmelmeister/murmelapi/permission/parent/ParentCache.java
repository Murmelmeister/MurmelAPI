package de.murmelmeister.murmelapi.permission.parent;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.permission.PermissionTarget;
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

final class ParentCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParentCache.class);

    @Language("MariaDB")
    private static final String SELECT_BY_USER_AND_PARENT = "SELECT * FROM %s WHERE user_id = ? AND parent_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP_AND_PARENT = "SELECT * FROM %s WHERE group_id = ? AND parent_id = ?";

    @Language("MariaDB")
    private static final String SELECT_BY_USER = "SELECT * FROM %s WHERE user_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP = "SELECT * FROM %s WHERE group_id = ?";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull ParentKey, Optional<Parent>> cacheByKey;
    private final LoadingCache<@NotNull PermissionTarget, List<Parent>> cacheByTarget;

    public ParentCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByTarget = CacheUtil.buildCacheRefresh(this::loadByTarget, cacheCapacity, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.PARENTS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_PARENT.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof ParentKey parentKey)
                remove(parentKey);
            else if (key instanceof String json) {
                try {
                    final ParentKey parentKey = gson.fromJson(json, ParentKey.class);

                    if (parentKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(parentKey);
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

    private @NotNull Optional<Parent> loadByKey(@NotNull ParentKey key) {
        String sql;
        if (key.target().type() == PermissionTarget.TargetType.USER)
            sql = SELECT_BY_USER_AND_PARENT.formatted(tableName);
        else sql = SELECT_BY_GROUP_AND_PARENT.formatted(tableName);

        Parent parent = CacheUtil.loadSingle(database, sql, fetchLimit, ParentRowMapper::resultSet, stmt -> {
            stmt.setInt(1, key.target().id());
            stmt.setObject(2, key.parentId(), Types.INTEGER);
        });

        return Optional.ofNullable(parent);
    }

    private @NotNull List<Parent> loadByTarget(@NotNull PermissionTarget target) {
        String sql;
        if (target.type() == PermissionTarget.TargetType.USER)
            sql = SELECT_BY_USER.formatted(tableName);
        else sql = SELECT_BY_GROUP.formatted(tableName);

        return CacheUtil.loadList(database, sql, fetchLimit, ParentRowMapper::resultSet, stmt -> stmt.setInt(1, target.id()));
    }

    public @NotNull Optional<Parent> getByKey(@NotNull PermissionTarget target, int parentId) {
        return cacheByKey.get(new ParentKey(target, parentId));
    }

    public @NotNull @Unmodifiable List<Parent> getByTarget(@NotNull PermissionTarget target) {
        List<Parent> parents = cacheByTarget.get(target);
        if (parents == null || parents.isEmpty())
            return Collections.emptyList();
        return List.copyOf(parents);
    }

    public void remove(@NotNull ParentKey key) {
        cacheByKey.invalidate(key);
        cacheByTarget.invalidate(key.target());
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByTarget.invalidateAll();
    }

    record ParentKey(@NotNull PermissionTarget target, @Nullable Integer parentId) {
    }
}
