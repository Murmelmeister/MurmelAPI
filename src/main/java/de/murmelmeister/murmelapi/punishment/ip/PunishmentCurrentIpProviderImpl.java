package de.murmelmeister.murmelapi.punishment.ip;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PunishmentCurrentIpProviderImpl implements PunishmentCurrentIpProvider {
    public static final String TABLE_NAME = "punishment_current_ip";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final PunishmentCurrentIpCache cache;
    private final RefreshType all = RefreshType.PUNISHMENT_IPS;
    private final RefreshType single = RefreshType.SINGLE_PUNISHMENT_IP;

    public PunishmentCurrentIpProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new PunishmentCurrentIpCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @NotNull @Unmodifiable List<InetAddress> getAllPunishedIps(int typeId) {
        return cache.getAll().stream()
                .filter(punish -> punish.typeId() == typeId)
                .map(PunishmentCurrentIp::inetAddress)
                .toList();
    }

    @Override
    public @Nullable PunishmentCurrentIp getPunishedIp(@NotNull InetAddress inetAddress, int typeId) {
        return cache.get(inetAddress, typeId);
    }

    @Override
    public @Nullable PunishmentCurrentIp create(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID logId) {
        if (typeId < 1)
            return null;

        @Language("MariaDB")
        String sql = """
                INSERT INTO %s (ip_address, type_id, log_id)
                VALUES (?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, inetAddress.getHostAddress());
            stmt.setInt(2, typeId);
            stmt.setString(3, logId.toString());
        });
        if (row < 1) return null;

        PunishmentCurrentIp punish = new PunishmentCurrentIp(inetAddress, typeId, logId);
        refreshProvider.fireSingle(single, new PunishmentCurrentIpCache.IpTypeKey(inetAddress, typeId));
        return punish;
    }

    @Override
    public int delete(@NotNull InetAddress inetAddress, int typeId) {
        if (typeId < 1)
            return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE ip_address = ? AND type_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, inetAddress.getHostAddress());
            stmt.setInt(2, typeId);
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new PunishmentCurrentIpCache.IpTypeKey(inetAddress, typeId));
        return row;
    }

    @Override
    public @Nullable PunishmentCurrentIp update(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID logId) {
        if (typeId < 1)
            return null;

        PunishmentCurrentIp existing = cache.get(inetAddress, typeId);
        if (existing == null) return null;

        if (Objects.equals(logId, existing.logId()))
            return existing; // No update needed if the log ID is the same

        @Language("MariaDB")
        String sql = "UPDATE %s SET log_id = ? WHERE ip_address = ? AND type_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, logId.toString());
            stmt.setString(2, inetAddress.getHostAddress());
            stmt.setInt(3, typeId);
        });
        if (row < 1) return null;

        PunishmentCurrentIp punish = PunishmentCurrentIp.builder(existing)
                .logId(logId)
                .build();
        refreshProvider.fireSingle(single, new PunishmentCurrentIpCache.IpTypeKey(inetAddress, typeId));
        return punish;
    }
}
