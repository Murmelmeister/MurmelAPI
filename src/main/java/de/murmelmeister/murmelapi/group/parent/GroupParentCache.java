package de.murmelmeister.murmelapi.group.parent;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupParentCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP_ID = "SELECT * FROM %s WHERE group_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE group_id = ? AND parent_id = ?";

    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*groupId=(\\d+), parentId=(\\d+).*");

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull ParentKey, Optional<GroupParent>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<GroupParent>> cacheByGroupId;
    private final LoadingCache<@NotNull String, List<GroupParent>> listCache;

    public GroupParentCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
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
            if (!(key instanceof String)) {
                if (key instanceof ParentKey(int groupId, int parentId))
                    remove(groupId, parentId);
                else if (key instanceof Integer groupId)
                    remove(groupId);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    int groupId = Integer.parseInt(matcher.group(1));
                    int parentId = Integer.parseInt(matcher.group(2));
                    remove(groupId, parentId);
                } else {
                    int groupId = Integer.parseInt((String) key);
                    remove(groupId);
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
                    stmt.setInt(2, key.parentId());
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

    public void put(@Nullable GroupParent groupParent) {
        if (groupParent == null) return;
        ParentKey key = new ParentKey(groupParent.groupId(), groupParent.parentId());
        cacheByKey.put(key, Optional.of(groupParent));
        CacheUtil.put(cacheByGroupId, groupParent.groupId(), groupParent,
                v -> v.groupId() == groupParent.groupId() && v.parentId() == groupParent.parentId());
        CacheUtil.put(listCache, ALL_KEY, groupParent,
                v -> v.groupId() == groupParent.groupId() && v.parentId() == groupParent.parentId());
    }

    public void remove(int groupId, int parentId) {
        ParentKey key = new ParentKey(groupId, parentId);
        cacheByKey.invalidate(key);
        CacheUtil.remove(cacheByGroupId, groupId, v -> v.groupId() == groupId && v.parentId() == parentId);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId() == groupId && v.parentId() == parentId);
    }

    public void remove(int groupId) {
        cacheByKey.asMap().keySet().stream().filter(key -> key.groupId() == groupId)
                .forEach(cacheByKey::invalidate);
        cacheByGroupId.invalidate(groupId);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId() == groupId);
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

    protected record ParentKey(int groupId, int parentId) {
    }
}
