package de.murmelmeister.murmelapi.punishment.audit;

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
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PunishmentAuditCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PunishmentAuditCache.class);

    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_MOJANG_ID = "SELECT * FROM %s WHERE mojang_id = ? ORDER BY created_at DESC";
    @Language("MariaDB")
    private static final String SELECT_BY_IP_ADDRESS = "SELECT * FROM %s WHERE ip_address = ? ORDER BY created_at DESC";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull UUID, Optional<PunishmentAudit>> cacheById;
    private final LoadingCache<@NotNull UUID, List<PunishmentAudit>> cacheByMojangId;
    private final LoadingCache<@NotNull InetAddress, List<PunishmentAudit>> cacheByIpAddress;

    public PunishmentAuditCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByMojangId = CacheUtil.buildCacheRefresh(this::loadByMojangId, cacheCapacity, refreshInterval);
        this.cacheByIpAddress = CacheUtil.buildCacheRefresh(this::loadByIpAddress, cacheCapacity, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.PUNISHMENT_AUDITS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_PUNISHMENT_AUDIT.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (key instanceof PunishmentAudit audit)
                remove(audit);
            else if (key instanceof String json) {
                try {
                    final PunishmentAudit audit = gson.fromJson(json, PunishmentAudit.class);

                    if (audit == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(audit);
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

    private @NotNull Optional<PunishmentAudit> loadById(UUID id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        PunishmentAudit punishmentAudit = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.punishmentAudit(),
                stmt -> stmt.setString(1, id.toString()));

        return Optional.ofNullable(punishmentAudit);
    }

    private @NotNull List<PunishmentAudit> loadByMojangId(UUID mojangId) {
        String sql = SELECT_BY_MOJANG_ID.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentAudit(),
                stmt -> stmt.setString(1, mojangId.toString()));
    }

    private @NotNull List<PunishmentAudit> loadByIpAddress(InetAddress inetAddress) {
        String sql = SELECT_BY_IP_ADDRESS.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.punishmentAudit(),
                stmt -> stmt.setString(1, inetAddress.getHostAddress()));
    }

    public @NotNull Optional<PunishmentAudit> getById(@NotNull UUID logId) {
        return cacheById.get(logId);
    }

    public @NotNull @Unmodifiable List<PunishmentAudit> getByMojangId(@NotNull UUID mojangId) {
        List<PunishmentAudit> logs = cacheByMojangId.get(mojangId);
        if (logs == null || logs.isEmpty())
            return Collections.emptyList();
        return List.copyOf(logs);
    }

    public @NotNull @Unmodifiable List<PunishmentAudit> getByIpAddress(@NotNull InetAddress inetAddress) {
        List<PunishmentAudit> logs = cacheByIpAddress.get(inetAddress);
        if (logs == null || logs.isEmpty())
            return Collections.emptyList();
        return List.copyOf(logs);
    }

    public void remove(@NotNull PunishmentAudit audit) {
        cacheById.invalidate(audit.id());
        if (audit.mojangId() != null) cacheByMojangId.invalidate(audit.mojangId());
        if (audit.inetAddress() != null) cacheByIpAddress.invalidate(audit.inetAddress());
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByMojangId.invalidateAll();
        cacheByIpAddress.invalidateAll();
    }
}
