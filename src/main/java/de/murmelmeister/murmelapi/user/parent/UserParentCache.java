package de.murmelmeister.murmelapi.user.parent;

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

import java.sql.Types;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class UserParentCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserParentCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_USER_ID = "SELECT * FROM %s WHERE user_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE user_id = ? AND parent_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull ParentKey, Optional<UserParent>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<UserParent>> cacheByUserId;
    private final LoadingCache<@NotNull String, List<UserParent>> listCache;

    public UserParentCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByUserId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.USER_PARENTS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_USER_PARENT.getName().equalsIgnoreCase(cacheName)) {
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

    private @NotNull List<UserParent> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userParent());
    }

    private @NotNull List<UserParent> loadByUserId(int userId) {
        String sql = SELECT_BY_USER_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userParent(),
                stmt -> stmt.setInt(1, userId));
    }

    private @NotNull Optional<UserParent> loadByKey(ParentKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        UserParent userParent = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userParent(),
                stmt -> {
                    stmt.setInt(1, key.userId());
                    stmt.setObject(2, key.parentId(), Types.INTEGER);
                });

        return Optional.ofNullable(userParent);
    }

    public @Nullable UserParent get(int userId, int parentId) {
        Optional<UserParent> optParent = cacheByKey.get(new ParentKey(userId, parentId));
        return optParent != null && optParent.isPresent() ? optParent.orElse(null) : null;
    }

    public @NotNull @Unmodifiable List<UserParent> getParents(int userId) {
        List<UserParent> parents = cacheByUserId.get(userId);
        if (parents == null || parents.isEmpty())
            return Collections.emptyList();
        return List.copyOf(parents);
    }

    public @NotNull @Unmodifiable List<UserParent> getAll() {
        List<UserParent> parents = listCache.get(ALL_KEY);
        if (parents == null || parents.isEmpty())
            return Collections.emptyList();
        return List.copyOf(parents);
    }

    public void remove(@NotNull ParentKey key) {
        cacheByKey.invalidate(key);
        if (key.parentId() == null) {
            cacheByUserId.invalidate(key.userId());
            CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == key.userId());
        } else {
            CacheUtil.remove(cacheByUserId, key.userId(), v -> v.userId() == key.userId() && v.parentId() == key.parentId());
            CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == key.userId() && v.parentId() == key.parentId());
        }
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByUserId.invalidateAll();
        listCache.invalidateAll();
    }

    public record ParentKey(int userId, @Nullable Integer parentId) {
    }
}
