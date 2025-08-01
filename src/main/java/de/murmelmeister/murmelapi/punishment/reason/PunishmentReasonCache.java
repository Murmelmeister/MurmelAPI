package de.murmelmeister.murmelapi.punishment.reason;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshListener;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.List;

public class PunishmentReasonCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<Integer, PunishmentReason> cacheById;
    private final LoadingCache<Integer, List<PunishmentReason>> cacheByType;
    private final LoadingCache<String, List<PunishmentReason>> listCache;
    private final Long fetchLimit;

    public PunishmentReasonCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapcity, refreshInterval);
        this.cacheByType = CacheUtil.buildCacheRefresh(this::loadByType, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.getType();
        if (RefreshType.PUNISHMENT_REASONS.getName().equalsIgnoreCase(cacheName)
            || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_PUNISHMENT_REASON.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.getKey();
            if (key instanceof Integer reasonId)
                remove(reasonId);
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        clear();
    }

    private void refreshAll() {
        clear();
        List<PunishmentReason> reasons = loadAllFromDatabase();
        reasons.forEach(this::put);
    }

    private List<PunishmentReason> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentReason());
    }

    private List<PunishmentReason> loadByType(int typeId) {
        String sql = "SELECT * FROM " + tableName + " WHERE type_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentReason(), typeId);
    }

    private PunishmentReason loadById(int reasonId) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.punishmentReason(), reasonId);
    }

    public PunishmentReason getById(int reasonId) {
        return cacheById.get(reasonId);
    }

    public List<PunishmentReason> getByType(int typeId) {
        return cacheByType.get(typeId);
    }

    public void put(PunishmentReason reason) {
        int reasonId = reason.id();
        cacheById.put(reasonId, reason);
        CacheUtil.put(cacheByType, reason.typeId(), reason, v -> v.id() == reasonId);
        CacheUtil.put(listCache, ALL_KEY, reason, v -> v.id() == reasonId);
    }

    public void remove(int reasonId) {
        PunishmentReason reason = cacheById.getIfPresent(reasonId);
        if (reason != null) {
            cacheById.invalidate(reasonId);
            CacheUtil.remove(cacheByType, reason.typeId(), v -> v.id() == reasonId);
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == reasonId);
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByType.invalidateAll();
        listCache.invalidateAll();
    }

    public List<PunishmentReason> getCachedPunishReasons() {
        return listCache.get(ALL_KEY);
    }
}
