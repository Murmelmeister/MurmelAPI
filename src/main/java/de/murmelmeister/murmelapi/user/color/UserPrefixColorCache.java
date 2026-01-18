package de.murmelmeister.murmelapi.user.color;

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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserPrefixColorCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE user_id = ? AND color_id = ?";

    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*userId=(\\d+), colorId=([^,]+).*");

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull ColorKey, Optional<UserPrefixColor>> cache;
    private final LoadingCache<@NotNull String, List<UserPrefixColor>> listCache;

    public UserPrefixColorCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, java.time.Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.USER_PREFIX_COLORS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_USER_PREFIX_COLOR.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof ColorKey(int userId, String colorId))
                    remove(userId, colorId);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    int userId = Integer.parseInt(matcher.group(1));
                    String colorId = matcher.group(2);
                    remove(userId, colorId);
                } else {
                    throw new IllegalArgumentException("Invalid key format: " + key);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<UserPrefixColor> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.userPrefixColor());
    }

    private @NotNull Optional<UserPrefixColor> loadById(ColorKey key) {
        String sql = SELECT_BY_ID.formatted(tableName);
        UserPrefixColor userPrefixColor = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.userPrefixColor(), stmt -> {
            stmt.setInt(1, key.userId());
            stmt.setString(2, key.colorId());
        });

        return Optional.ofNullable(userPrefixColor);
    }

    public @Nullable UserPrefixColor get(int userId, @NotNull String colorId) {
        Optional<UserPrefixColor> optColor = cache.get(new ColorKey(userId, colorId));
        return optColor != null && optColor.isPresent() ? optColor.orElse(null) : null;
    }

    public @NotNull List<UserPrefixColor> getAll() {
        List<UserPrefixColor> colors = listCache.get(ALL_KEY);
        if (colors == null || colors.isEmpty())
            return Collections.emptyList();
        return colors;
    }

    public void put(@Nullable UserPrefixColor color) {
        if (color == null) return;
        cache.put(new ColorKey(color.userId(), color.colorId()), Optional.of(color));
        CacheUtil.put(listCache, ALL_KEY, color, v -> v.userId() == color.userId() && v.colorId().equals(color.colorId()));
    }

    public void remove(int userId, @NotNull String colorId) {
        cache.invalidate(new ColorKey(userId, colorId));
        CacheUtil.remove(listCache, ALL_KEY, v -> v.userId() == userId && v.colorId().equals(colorId));
    }

    public void clear() {
        cache.invalidateAll();
        listCache.invalidateAll();
    }

    protected record ColorKey(int userId, @NotNull String colorId) {
    }
}
