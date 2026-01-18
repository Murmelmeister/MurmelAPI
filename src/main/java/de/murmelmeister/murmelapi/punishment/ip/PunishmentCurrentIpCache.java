package de.murmelmeister.murmelapi.punishment.ip;

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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PunishmentCurrentIpCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE ip_address = ? AND type_id = ?";

    private static final String ALL_KEY = "ALL";
    private static final Pattern KEY_PATTERN = Pattern.compile(".*ipAddress=([^,]+), typeId=(\\d+).*");

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull IpTypeKey, Optional<PunishmentCurrentIp>> cache;
    private final LoadingCache<@NotNull String, List<PunishmentCurrentIp>> listCache;

    public PunishmentCurrentIpCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadFromDatabase, cacheCapcity, refreshInterval);
        this.listCache = CacheUtil.buildCacheRefresh(key -> loadAllFromDatabase(), 1, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.PUNISHMENT_IPS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_PUNISHMENT_IP.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof IpTypeKey(InetAddress inetAddress, int typeId))
                    remove(inetAddress, typeId);
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
                    remove(inetAddress, typeId);
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

    private @NotNull List<PunishmentCurrentIp> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentCurrentIp());
    }

    private @NotNull Optional<PunishmentCurrentIp> loadFromDatabase(IpTypeKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        PunishmentCurrentIp punishmentCurrentIp = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.punishmentCurrentIp(),
                stmt -> {
                    stmt.setString(1, key.inetAddress().getHostAddress());
                    stmt.setInt(2, key.typeId());
                });

        return Optional.ofNullable(punishmentCurrentIp);
    }

    public @Nullable PunishmentCurrentIp get(@NotNull InetAddress inetAddress, int typeId) {
        Optional<PunishmentCurrentIp> optIp = cache.get(new IpTypeKey(inetAddress, typeId));
        return optIp != null && optIp.isPresent() ? optIp.orElse(null) : null;
    }

    public void put(@Nullable PunishmentCurrentIp punish) {
        if (punish == null) return;
        IpTypeKey key = new IpTypeKey(punish.inetAddress(), punish.typeId());
        cache.put(key, Optional.of(punish));
        CacheUtil.put(listCache, ALL_KEY, punish,
                v -> v.inetAddress().equals(key.inetAddress()) && v.typeId() == key.typeId());
    }

    public void remove(@NotNull InetAddress inetAddress, int typeId) {
        IpTypeKey key = new IpTypeKey(inetAddress, typeId);
        cache.invalidate(key);
        CacheUtil.remove(listCache, ALL_KEY,
                v -> v.inetAddress().equals(inetAddress) && v.typeId() == typeId);
    }

    public void clear() {
        cache.invalidateAll();
        listCache.invalidateAll();
    }

    public @NotNull List<PunishmentCurrentIp> getCachedPunishIPs() {
        List<PunishmentCurrentIp> ips = listCache.get(ALL_KEY);
        if (ips == null || ips.isEmpty())
            return Collections.emptyList();
        return List.copyOf(ips);
    }

    protected record IpTypeKey(@NotNull InetAddress inetAddress, int typeId) {
    }
}
