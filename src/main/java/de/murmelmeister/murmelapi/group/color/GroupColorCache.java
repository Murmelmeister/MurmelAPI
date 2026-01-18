package de.murmelmeister.murmelapi.group.color;

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

public class GroupColorCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_GROUP_ID = "SELECT * FROM %s WHERE group_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE group_id = ? AND type_id = ?";

    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*groupId=(\\d+), typeId=(\\d+).*");

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull ColorKey, Optional<GroupColor>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<GroupColor>> cacheByGroupId;
    private final LoadingCache<@NotNull String, List<GroupColor>> listCache;

    public GroupColorCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheExpired(this::loadByKey, cacheCapcity, refreshInterval);
        this.cacheByGroupId = CacheUtil.buildCacheExpired(this::loadByGroupId, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheExpired(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.GROUP_COLORS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_GROUP_COLOR.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof ColorKey(int groupId, int typeId))
                    remove(groupId, typeId);
                else if (key instanceof Integer groupId)
                    remove(groupId);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    int groupId = Integer.parseInt(matcher.group(1));
                    int typeId = Integer.parseInt(matcher.group(2));
                    remove(groupId, typeId);
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

    private @NotNull List<GroupColor> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupColor());
    }

    private @NotNull List<GroupColor> loadByGroupId(int groupId) {
        String sql = SELECT_BY_GROUP_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupColor(),
                stmt -> stmt.setInt(1, groupId));
    }

    private @NotNull Optional<GroupColor> loadByKey(ColorKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        GroupColor groupColor = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.groupColor(),
                stmt -> {
                    stmt.setInt(1, key.groupId());
                    stmt.setInt(2, key.typeId());
                });

        return Optional.ofNullable(groupColor);
    }

    public @Nullable GroupColor get(int groupId, int typeId) {
        Optional<GroupColor> optColor = cacheByKey.get(new ColorKey(groupId, typeId));
        return optColor != null && optColor.isPresent() ? optColor.orElse(null) : null;
    }

    public @Nullable List<GroupColor> getByGroupId(int groupId) {
        return cacheByGroupId.get(groupId);
    }

    public void put(@Nullable GroupColor groupColor) {
        if (groupColor == null) return;
        ColorKey key = new ColorKey(groupColor.groupId(), groupColor.typeId());
        cacheByKey.put(key, Optional.of(groupColor));
        CacheUtil.put(cacheByGroupId, groupColor.groupId(), groupColor,
                v -> v.groupId() == groupColor.groupId() && v.typeId() == groupColor.typeId());
        CacheUtil.put(listCache, ALL_KEY, groupColor,
                v -> v.groupId() == groupColor.groupId() && v.typeId() == groupColor.typeId());
    }

    public void remove(int groupId, int typeId) {
        ColorKey key = new ColorKey(groupId, typeId);
        cacheByKey.invalidate(key);
        CacheUtil.remove(cacheByGroupId, groupId, v -> v.groupId() == groupId && v.typeId() == typeId);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.groupId() == groupId && v.typeId() == typeId);
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

    public @NotNull List<GroupColor> getCachedColors() {
        List<GroupColor> colors = listCache.get(ALL_KEY);
        if (colors == null || colors.isEmpty())
            return Collections.emptyList();
        return List.copyOf(colors);
    }

    protected record ColorKey(int groupId, int typeId) {
    }
}
