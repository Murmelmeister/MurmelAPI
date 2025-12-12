package de.murmelmeister.murmelapi.punishment.ip;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PunishmentCurrentIpCache implements MurmelCache {
    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*ipAddress=([^,]+), typeId=(\\d+).*");
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
        String cacheName = event.type();
        if (RefreshType.PUNISHMENT_IPS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_PUNISHMENT_IP.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof IpTypeKey ipTypeKey)
                    refreshSingle(ipTypeKey);
            } else {
                Matcher matcher = KEY_PATTERN.matcher((String) key);
                if (matcher.matches()) {
                    String ipAddress = matcher.group(1);
                    InetAddress inetAddress;
                    try {
                        inetAddress = InetAddress.getByName(ipAddress);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }

                    int typeId = Integer.parseInt(matcher.group(2));
                    refreshSingle(new IpTypeKey(inetAddress, typeId));
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
        if (punishments.isEmpty())
            return;

        punishments.forEach(punish -> cache.put(new IpTypeKey(punish.inetAddress(), punish.typeId()), punish));
        listCache.put(ALL_KEY, List.copyOf(punishments));
    }

    private void refreshSingle(IpTypeKey key) {
        remove(key.inetAddress(), key.typeId());
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
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.punishmentCurrentIp(),
                stmt -> {
                    stmt.setString(1, key.inetAddress().getHostAddress());
                    stmt.setInt(2, key.typeId());
                });
    }

    public PunishmentCurrentIp get(InetAddress inetAddress, int typeId) {
        return cache.get(new IpTypeKey(inetAddress, typeId));
    }

    public void put(PunishmentCurrentIp punish) {
        IpTypeKey key = new IpTypeKey(punish.inetAddress(), punish.typeId());
        cache.put(key, punish);
        CacheUtil.put(listCache, ALL_KEY, punish,
                v -> v.inetAddress().equals(key.inetAddress()) && v.typeId() == key.typeId());
    }

    public void remove(InetAddress inetAddress, int typeId) {
        IpTypeKey key = new IpTypeKey(inetAddress, typeId);
        cache.invalidate(key);
        CacheUtil.remove(listCache, ALL_KEY,
                v -> v.inetAddress().equals(inetAddress) && v.typeId() == typeId);
    }

    public void clear() {
        cache.invalidateAll();
        listCache.invalidateAll();
    }

    public List<PunishmentCurrentIp> getCachedPunishIPs() {
        List<PunishmentCurrentIp> ips = listCache.get(ALL_KEY);
        if (ips == null || ips.isEmpty())
            return Collections.emptyList();
        return List.copyOf(ips);
    }

    protected record IpTypeKey(InetAddress inetAddress, int typeId) {
    }
}
