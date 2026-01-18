package de.murmelmeister.murmelapi.user.permission;

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

public class UserPermissionCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_USER_ID = "SELECT * FROM %s WHERE user_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE user_id = ? AND permission = ?";

    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*userId=(\\d+), permission=([^,\\]]+).*");

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull PermissionKey, Optional<UserPermission>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<UserPermission>> cacheByUserId;
    private final LoadingCache<@NotNull String, List<UserPermission>> listCache;

    public UserPermissionCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapcity, refreshInterval);
        this.cacheByUserId = CacheUtil.buildCacheRefresh(this::loadByUserId, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.USER_PERMISSIONS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_USER_PERMISSION.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof PermissionKey(int userId, String permission))
                    remove(userId, permission);
                else if (key instanceof Integer userId)
                    remove(userId);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    int userId = Integer.parseInt(matcher.group(1));
                    String permission = matcher.group(2);
                    remove(userId, permission);
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

    private @NotNull List<UserPermission> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userPermission());
    }

    private @NotNull List<UserPermission> loadByUserId(int userId) {
        String sql = SELECT_BY_USER_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userPermission(),
                stmt -> stmt.setInt(1, userId));
    }

    private @NotNull Optional<UserPermission> loadByKey(PermissionKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        UserPermission userPermission = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userPermission(),
                stmt -> {
                    stmt.setInt(1, key.userId());
                    stmt.setString(2, key.permission());
                });

        return Optional.ofNullable(userPermission);
    }

    public @Nullable UserPermission get(int userId, @NotNull String permission) {
        Optional<UserPermission> optPermission = cacheByKey.get(new PermissionKey(userId, permission));
        return optPermission != null && optPermission.isPresent() ? optPermission.orElse(null) : null;
    }

    public @Nullable List<UserPermission> getPermissions(int userId) {
        return cacheByUserId.get(userId);
    }

    public void put(@Nullable UserPermission permission) {
        if (permission == null) return;
        PermissionKey key = new PermissionKey(permission.userId(), permission.permission());
        cacheByKey.put(key, Optional.of(permission));
        CacheUtil.put(cacheByUserId, permission.userId(), permission,
                v -> v.userId() == permission.userId() && v.permission().equals(permission.permission()));
        CacheUtil.put(listCache, ALL_KEY, permission, v -> v.userId() == permission.userId() && v.permission().equals(permission.permission()));
    }

    public void remove(int userId, @NotNull String permission) {
        PermissionKey key = new PermissionKey(userId, permission);
        cacheByKey.invalidate(key);
        CacheUtil.remove(cacheByUserId, userId, v -> v.userId() == userId && v.permission().equals(permission));
        CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == userId && v.permission().equals(permission));
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

    public @NotNull List<UserPermission> getCachedPermissions() {
        List<UserPermission> permissions = listCache.get(ALL_KEY);
        if (permissions == null || permissions.isEmpty())
            return Collections.emptyList();
        return List.copyOf(permissions);
    }

    protected record PermissionKey(int userId, @NotNull String permission) {
    }
}
