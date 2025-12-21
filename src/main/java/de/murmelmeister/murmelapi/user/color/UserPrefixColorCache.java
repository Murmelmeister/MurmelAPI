package de.murmelmeister.murmelapi.user.color;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserPrefixColorCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*userId=(\\d+), colorId=([^,]+).*");
    private final Database database;
    private final String tableName;
    private final LoadingCache<@NotNull ColorKey, UserPrefixColor> cache;
    private final LoadingCache<@NotNull String, List<UserPrefixColor>> listCache;
    private final Long fetchLimit;

    public UserPrefixColorCache(Database database, String tableName, Long fetchLimit, long cacheCapacity, java.time.Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.USER_PREFIX_COLORS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_USER_PREFIX_COLOR.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof ColorKey colorKey)
                    refreshSingle(colorKey);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    int userId = Integer.parseInt(matcher.group(1));
                    String colorId = matcher.group(2);
                    refreshSingle(new ColorKey(userId, colorId));
                } else {
                    throw new IllegalArgumentException("Invalid key format: " + key);
                }
            }
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        clear();
    }

    private void refreshAll() {
        clear();
        List<UserPrefixColor> colors = loadAllFromDatabase();
        if (colors.isEmpty())
            return;
        colors.forEach(this::put);
    }

    private void refreshSingle(ColorKey key) {
        remove(key.userId(), key.colorId());
        UserPrefixColor color = loadById(key);
        if (color != null) put(color);
    }

    private List<UserPrefixColor> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userPrefixColor());
    }

    private UserPrefixColor loadById(ColorKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE userId = ? AND colorId = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userPrefixColor(), stmt -> {
            stmt.setInt(1, key.userId());
            stmt.setString(2, key.colorId());
        });
    }

    public UserPrefixColor get(int userId, String colorId) {
        return cache.get(new ColorKey(userId, colorId));
    }

    public List<UserPrefixColor> getAll() {
        List<UserPrefixColor> colors = listCache.get(ALL_KEY);
        if (colors == null || colors.isEmpty())
            return Collections.emptyList();
        return colors;
    }

    public void put(UserPrefixColor color) {
        cache.put(new ColorKey(color.userId(), color.colorId()), color);
        CacheUtil.put(listCache, ALL_KEY, color, v -> v.userId() == color.userId() && v.colorId().equals(color.colorId()));
    }

    public void remove(int userId, String colorId) {
        cache.invalidate(new ColorKey(userId, colorId));
        CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == userId && v.colorId().equals(colorId));
    }

    public void clear() {
        cache.invalidateAll();
        listCache.invalidateAll();
    }

    protected record ColorKey(int userId, String colorId) {
    }
}
