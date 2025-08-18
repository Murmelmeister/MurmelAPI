package de.murmelmeister.murmelapi.punishment.ip;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshListener;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            if (!(key instanceof String)) {
                if (key instanceof IpTypeKey ipTypeKey)
                    refreshSingle(ipTypeKey);
            } else {
                Matcher matcher = Pattern.compile(".*ipAddress=([^,]+), typeId=(\\d+).*").matcher((String) key);
                if (matcher.matches()) {
                    String ipAddress = matcher.group(1);
                    int typeId = Integer.parseInt(matcher.group(2));
                    refreshSingle(new IpTypeKey(ipAddress, typeId));
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
        List<PunishmentCurrentIp> punishments = loadAllFromDatabase();
        punishments.forEach(this::put);
    }

    private void refreshSingle(IpTypeKey key) {
        remove(key.ipAddress(), key.typeId());
        PunishmentCurrentIp punishment = loadFromDatabase(key);
        if (punishment != null)
            put(punishment);
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
