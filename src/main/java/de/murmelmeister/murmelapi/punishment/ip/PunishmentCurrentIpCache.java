package de.murmelmeister.murmelapi.punishment.ip;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PunishmentCurrentIpCache implements MurmelCache {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PunishmentCurrentIpCache.class);

    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE ip_address = ? AND type_id = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull IpTypeKey, Optional<PunishmentCurrentIp>> cache;
    private final LoadingCache<@NotNull String, List<PunishmentCurrentIp>> listCache;

    public PunishmentCurrentIpCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cache = CacheUtil.buildCacheRefresh(this::loadFromDatabase, cacheCapacity, refreshInterval);
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
            if (key instanceof IpTypeKey ipTypeKey)
                remove(ipTypeKey);
            else if (key instanceof String json) {
                try {
                    final IpTypeKey ipTypeKey = gson.fromJson(json, IpTypeKey.class);

                    if (ipTypeKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(ipTypeKey);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single refresh: {}", json, e);
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

    public @NotNull @Unmodifiable List<PunishmentCurrentIp> getAll() {
        List<PunishmentCurrentIp> ips = listCache.get(ALL_KEY);
        if (ips == null || ips.isEmpty())
            return Collections.emptyList();
        return List.copyOf(ips);
    }

    public void remove(@NotNull IpTypeKey key) {
        cache.invalidate(key);
        CacheUtil.remove(listCache, ALL_KEY, v -> v.inetAddress().equals(key.inetAddress()) && v.typeId() == key.typeId());
    }

    public void clear() {
        cache.invalidateAll();
        listCache.invalidateAll();
    }

    public record IpTypeKey(@NotNull InetAddress inetAddress, int typeId) {
    }
}
