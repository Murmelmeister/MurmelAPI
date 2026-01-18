package de.murmelmeister.murmelapi.user.parent;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserParentCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_USER_ID = "SELECT * FROM %s WHERE user_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE user_id = ? AND parent_id = ?";

    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*userId=(\\d+), parentId=(\\d+).*");

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull ParentKey, Optional<UserParent>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<UserParent>> cacheByUserId;
    private final LoadingCache<@NotNull String, List<UserParent>> listCache;

    public UserParentCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
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
            if (!(key instanceof String)) {
                if (key instanceof ParentKey(int userId, int parentId))
                    remove(userId, parentId);
                else if (key instanceof Integer userId)
                    remove(userId);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    int userId = Integer.parseInt(matcher.group(1));
                    int parentId = Integer.parseInt(matcher.group(2));
                    remove(userId, parentId);
                } else {
                    int userId = Integer.parseInt((String) key);
                    remove(userId);
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

    private Optional<UserParent> loadByKey(ParentKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        UserParent userParent = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userParent(),
                stmt -> {
                    stmt.setInt(1, key.userId());
                    stmt.setInt(2, key.parentId());
                });

        return Optional.ofNullable(userParent);
    }

    public @Nullable UserParent get(int userId, int parentId) {
        Optional<UserParent> optParent = cacheByKey.get(new ParentKey(userId, parentId));
        return optParent != null && optParent.isPresent() ? optParent.orElse(null) : null;
    }

    public @Nullable List<UserParent> getParents(int userId) {
        return cacheByUserId.get(userId);
    }

    public void put(@Nullable UserParent userParent) {
        if (userParent == null) return;
        ParentKey key = new ParentKey(userParent.userId(), userParent.parentId());
        cacheByKey.put(key, Optional.of(userParent));
        CacheUtil.put(cacheByUserId, userParent.userId(), userParent,
                v -> v.userId() == userParent.userId() && v.parentId() == userParent.parentId());
        CacheUtil.put(listCache, ALL_KEY, userParent,
                v -> v.userId() == userParent.userId() && v.parentId() == userParent.parentId());
    }

    public void remove(int userId, int parentId) {
        ParentKey key = new ParentKey(userId, parentId);
        cacheByKey.invalidate(key);
        CacheUtil.remove(cacheByUserId, userId, v -> v.userId() == userId && v.parentId() == parentId);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == userId && v.parentId() == parentId);
    }

    public void remove(int userId) {
        cacheByKey.asMap().keySet().stream().filter(key -> key.userId() == userId)
                .forEach(cacheByKey::invalidate);
        cacheByUserId.invalidate(userId);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == userId);
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByUserId.invalidateAll();
        listCache.invalidateAll();
    }

    public @NotNull List<UserParent> getCachedParents() {
        List<UserParent> parents = listCache.get(ALL_KEY);
        if (parents == null || parents.isEmpty())
            return Collections.emptyList();
        return List.copyOf(parents);
    }

    protected record ParentKey(int userId, int parentId) {
    }
}
