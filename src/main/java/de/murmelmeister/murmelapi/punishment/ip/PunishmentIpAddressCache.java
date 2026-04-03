package de.murmelmeister.murmelapi.punishment.ip;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

final class PunishmentIpAddressCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PunishmentIpAddressCache.class);

    @Language("MariaDB")
    private static final String SELECT_BY_KEY = "SELECT * FROM %s WHERE ip_address = ? AND type_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_TYPE_ID = "SELECT * FROM %s WHERE type_id = ?";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull PunishKey, Optional<PunishmentIpAddress>> cacheByKey;
    private final LoadingCache<@NotNull Integer, List<PunishmentIpAddress>> cacheByType;

    public PunishmentIpAddressCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheByKey = CacheUtil.buildCacheRefresh(this::loadByKey, cacheCapacity, refreshInterval);
        this.cacheByType = CacheUtil.buildCacheRefresh(this::loadByType, cacheCapacity, refreshInterval);
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
            if (key instanceof PunishKey punishKey)
                remove(punishKey);
            else if (key instanceof String json) {
                try {
                    final PunishKey punishKey = gson.fromJson(json, PunishKey.class);

                    if (punishKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(punishKey);
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

    private @NotNull Optional<PunishmentIpAddress> loadByKey(PunishKey key) {
        String sql = SELECT_BY_KEY.formatted(tableName);
        PunishmentIpAddress punishmentIpAddress = CacheUtil.loadSingle(database, sql, fetchLimit, PunishmentIpAddressRowMapper::resultSet,
                stmt -> {
                    stmt.setString(1, key.inetAddress().getHostAddress());
                    stmt.setInt(2, key.typeId());
                });

        return Optional.ofNullable(punishmentIpAddress);
    }

    private @NotNull List<PunishmentIpAddress> loadByType(int typeId) {
        String sql = SELECT_BY_TYPE_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, PunishmentIpAddressRowMapper::resultSet,
                stmt -> stmt.setInt(1, typeId));
    }

    public @NotNull Optional<PunishmentIpAddress> getByKey(@NotNull InetAddress inetAddress, int typeId) {
        return cacheByKey.get(new PunishKey(inetAddress, typeId));
    }

    public @NotNull @Unmodifiable List<PunishmentIpAddress> getByTypeId(int typeId) {
        List<PunishmentIpAddress> ips = cacheByType.get(typeId);
        if (ips == null || ips.isEmpty())
            return Collections.emptyList();
        return List.copyOf(ips);
    }

    public void remove(@NotNull PunishKey key) {
        cacheByKey.invalidate(key);
        cacheByType.invalidate(key.typeId());
    }

    public void clear() {
        cacheByKey.invalidateAll();
        cacheByType.invalidateAll();
    }

    record PunishKey(@NotNull InetAddress inetAddress, int typeId) {
    }
}
