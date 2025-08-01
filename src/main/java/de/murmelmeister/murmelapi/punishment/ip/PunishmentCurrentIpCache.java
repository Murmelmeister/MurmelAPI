package de.murmelmeister.murmelapi.punishment.ip;

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

public class PunishmentCurrentIpCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<IpTypeKey, PunishmentCurrentIp> cache;
    private final LoadingCache<String, List<PunishmentCurrentIp>> listCache;
    private final Long fetchLimit;

    public PunishmentCurrentIpCache(Database database, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadFromDatabase, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.getType();
        if (RefreshType.PUNISHMENT_IPS.getName().equalsIgnoreCase(cacheName)
            || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_PUNISHMENT_IP.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.getKey();
            if (key instanceof IpTypeKey(String ipAddress, int typeId))
                remove(ipAddress, typeId);
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        clear();
    }

    private void refreshAll() {
        clear();
        List<PunishmentCurrentIp> punishments = loadAllFromDatabase();
        punishments.forEach(this::put);
    }

    private List<PunishmentCurrentIp> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentCurrentIp());
    }

    private PunishmentCurrentIp loadFromDatabase(IpTypeKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE ip_address = ? AND type_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.punishmentCurrentIp(), key.ipAddress(), key.typeId());
    }

    public PunishmentCurrentIp get(String ipAddress, int typeId) {
        return cache.get(new IpTypeKey(ipAddress, typeId));
    }

    public void put(PunishmentCurrentIp punish) {
        IpTypeKey key = new IpTypeKey(punish.ipAddress(), punish.typeId());
        cache.put(key, punish);
        CacheUtil.put(listCache, ALL_KEY, punish,
                v -> v.ipAddress().equals(key.ipAddress()) && v.typeId() == key.typeId());
    }

    public void remove(String ipAddress, int typeId) {
        IpTypeKey key = new IpTypeKey(ipAddress, typeId);
        cache.invalidate(key);
        CacheUtil.remove(listCache, ALL_KEY,
                v -> v.ipAddress().equals(ipAddress) && v.typeId() == typeId);
    }

    public void clear() {
        cache.invalidateAll();
        listCache.invalidateAll();
    }

    public List<PunishmentCurrentIp> getCachedPunishIPs() {
        return listCache.get(ALL_KEY);
    }

    protected record IpTypeKey(String ipAddress, int typeId) {
    }
}
