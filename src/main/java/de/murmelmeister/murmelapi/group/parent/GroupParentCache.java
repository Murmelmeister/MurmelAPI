package de.murmelmeister.murmelapi.group.parent;

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

import java.sql.Types;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GroupParentCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupParentCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP_ID = "SELECT * FROM %s WHERE group_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE group_id = ? AND parent_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull ParentKey, Optional<GroupParent>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<GroupParent>> cacheByGroupId;
    private final LoadingCache<@NotNull String, List<GroupParent>> listCache;

    public GroupParentCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
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
        if (RefreshType.GROUP_PARENTS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_GROUP_PARENT.getName().equalsIgnoreCase(cacheName)) {
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

    public @NotNull List<GroupParent> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupParent());
    }

    public @NotNull List<GroupParent> loadByGroupId(int groupId) {
        String sql = SELECT_BY_GROUP_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupParent(),
                stmt -> stmt.setInt(1, groupId));
    }

    private @NotNull Optional<GroupParent> loadByKey(ParentKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        GroupParent groupParent = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.groupParent(),
                stmt -> {
                    stmt.setInt(1, key.groupId());
                    if (key.parentId() != null) stmt.setInt(2, key.parentId());
                    else stmt.setNull(2, Types.INTEGER);
                });

        return Optional.ofNullable(groupParent);
    }

    public @Nullable GroupParent get(int groupId, int parentId) {
        Optional<GroupParent> optParent = cacheByKey.get(new ParentKey(groupId, parentId));
        return optParent != null && optParent.isPresent() ? optParent.orElse(null) : null;
    }

    public @Nullable List<GroupParent> getParents(int groupId) {
        return cacheByGroupId.get(groupId);
    }

    public void remove(@NotNull ParentKey key) {
        cacheByKey.invalidate(key);
        if (key.parentId() == null) {
            cacheByGroupId.invalidate(key.groupId());
            CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId() == key.groupId());
        } else {
            CacheUtil.remove(cacheByGroupId, key.groupId(), v -> v.groupId() == key.groupId() && v.parentId() == key.parentId());
            CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId() == key.groupId() && v.parentId() == key.parentId());
        }
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByGroupId.invalidateAll();
        listCache.invalidateAll();
    }

    public @NotNull List<GroupParent> getCachedParents() {
        List<GroupParent> parents = listCache.get(ALL_KEY);
        if (parents == null || parents.isEmpty())
            return Collections.emptyList();
        return List.copyOf(parents);
    }

    public record ParentKey(int groupId, @Nullable Integer parentId) {
    }
}
