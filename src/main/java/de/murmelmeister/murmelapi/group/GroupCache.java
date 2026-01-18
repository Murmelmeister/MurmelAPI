package de.murmelmeister.murmelapi.group;

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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GroupCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_NAME = "SELECT * FROM %s WHERE group_name = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull Integer, Optional<Group>> cacheById;
    private final LoadingCache<@NotNull String, Optional<Group>> cacheByName;
    private final LoadingCache<@NotNull String, List<Group>> listCache;

    public GroupCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByName = CacheUtil.buildCacheRefresh(this::loadByName, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), cacheCapacity, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.GROUPS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_GROUP.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof Integer id)
                    remove(id);
            } else {
                int id = Integer.parseInt((String) key);
                remove(id);
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<Group> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.group());
    }

    private @NotNull Optional<Group> loadByName(String name) {
        String sql = SELECT_BY_NAME.formatted(tableName);
        Group group = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.group(),
                stmt -> stmt.setString(1, name));

        return Optional.ofNullable(group);
    }

    private @NotNull Optional<Group> loadById(int id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        Group group = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.group(),
                stmt -> stmt.setInt(1, id));

        return Optional.ofNullable(group);
    }

    public @Nullable Group getById(int id) {
        Optional<Group> optGroup = cacheById.get(id);
        return optGroup != null && optGroup.isPresent() ? optGroup.orElse(null) : null;
    }

    public @Nullable Group getByName(@Nullable String name) {
        if (name == null) return null;
        Optional<Group> optGroup = cacheByName.get(name);
        return optGroup != null && optGroup.isPresent() ? optGroup.orElse(null) : null;
    }

    public void put(@Nullable Group group) {
        if (group == null) return;
        cacheById.put(group.id(), Optional.of(group));
        cacheByName.put(group.groupName(), Optional.of(group));
        CacheUtil.put(listCache, ALL_KEY, group, v -> v.id() == group.id());
    }

    public void remove(int id) {
        Optional<Group> optGroup = cacheById.getIfPresent(id);
        cacheById.invalidate(id);

        if (optGroup != null && optGroup.isPresent()) {
            Group group = optGroup.get();
            cacheByName.invalidate(group.groupName());
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == id);
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByName.invalidateAll();
        listCache.invalidateAll();
    }

    public @NotNull List<Group> getCachedGroups() {
        List<Group> groups = listCache.get(ALL_KEY);
        if (groups == null || groups.isEmpty())
            return Collections.emptyList();
        return List.copyOf(groups);
    }
}
