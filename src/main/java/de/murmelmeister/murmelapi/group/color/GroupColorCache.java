package de.murmelmeister.murmelapi.group.color;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshListener;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupColorCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*groupId=(\\d+), typeId=(\\d+).*");
    private final Database database;
    private final String tableName;
    private final LoadingCache<ColorKey, GroupColor> cacheByKey;
    private final LoadingCache<Integer, List<GroupColor>> cacheByGroupId;
    private final LoadingCache<String, List<GroupColor>> listCache;
    private final Long fetchLimit;

    public GroupColorCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheExpired(this::loadByKey, cacheCapcity, refreshInterval);
        this.cacheByGroupId = CacheUtil.buildCacheExpired(this::loadByGroupId, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheExpired(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.GROUP_COLORS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_GROUP_COLOR.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof ColorKey colorKey)
                    refreshSingle(colorKey);
                else if (key instanceof Integer groupId)
                    refreshSingle(groupId);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    int groupId = Integer.parseInt(matcher.group(1));
                    int typeId = Integer.parseInt(matcher.group(2));
                    refreshSingle(new ColorKey(groupId, typeId));
                } else {
                    int groupId = Integer.parseInt((String) key);
                    refreshSingle(groupId);
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
        List<GroupColor> colors = loadAllFromDatabase();
        if (colors.isEmpty())
            return;

        Map<Integer, List<GroupColor>> byGroup = new HashMap<>();
        for (GroupColor color : colors) {
            ColorKey key = new ColorKey(color.groupId(), color.typeId());
            cacheByKey.put(key, color);
            byGroup.computeIfAbsent(color.groupId(), ignored -> new ArrayList<>()).add(color);
        }

        byGroup.forEach((groupId, values) -> cacheByGroupId.put(groupId, List.copyOf(values)));
        listCache.put(ALL_KEY, List.copyOf(colors));
    }

    private void refreshSingle(int groupId) {
        remove(groupId);
        List<GroupColor> groupColors = loadByGroupId(groupId);
        groupColors.forEach(this::put);
    }

    private void refreshSingle(ColorKey key) {
        remove(key.groupId(), key.typeId());
        GroupColor groupColor = loadByKey(key);
        if (groupColor != null)
            put(groupColor);
    }

    private List<GroupColor> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupColor());
    }

    private List<GroupColor> loadByGroupId(int groupId) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.groupColor(),
                stmt -> {
                    stmt.setInt(1, groupId);
                });
    }

    private GroupColor loadByKey(ColorKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE group_id = ? AND type_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.groupColor(),
                stmt -> {
                    stmt.setInt(1, key.groupId());
                    stmt.setInt(2, key.typeId());
                });
    }

    public GroupColor get(int groupId, int typeId) {
        return cacheByKey.get(new ColorKey(groupId, typeId));
    }

    public List<GroupColor> getByGroupId(int groupId) {
        return cacheByGroupId.get(groupId);
    }

    public void put(GroupColor groupColor) {
        ColorKey key = new ColorKey(groupColor.groupId(), groupColor.typeId());
        cacheByKey.put(key, groupColor);
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

    public List<GroupColor> getCachedColors() {
        List<GroupColor> colors = listCache.get(ALL_KEY);
        if (colors == null || colors.isEmpty())
            return Collections.emptyList();
        return List.copyOf(colors);
    }

    protected record ColorKey(int groupId, int typeId) {
    }
}
