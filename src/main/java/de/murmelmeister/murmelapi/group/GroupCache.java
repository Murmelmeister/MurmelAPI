package de.murmelmeister.murmelapi.group;

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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GroupCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_NAME = "SELECT * FROM %s WHERE group_name = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull Integer, Optional<Group>> cacheById;
    private final LoadingCache<@NotNull String, Optional<Group>> cacheByName;
    private final LoadingCache<@NotNull String, List<Group>> listCache;

    public GroupCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
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
            if (key instanceof Group group)
                remove(group);
            else if (key instanceof String json) {
                try {
                    final Group group = gson.fromJson(json, Group.class);

                    if (group == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(group);
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

    public void remove(@NotNull Group group) {
        cacheById.invalidate(group.id());
        cacheByName.invalidate(group.groupName());
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == group.id());
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
