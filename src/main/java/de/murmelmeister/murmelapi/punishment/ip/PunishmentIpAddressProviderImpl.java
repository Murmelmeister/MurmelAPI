package de.murmelmeister.murmelapi.punishment.ip;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.punishment.PunishmentException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
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

public final class PunishmentIpAddressProviderImpl implements PunishmentIpAddressProvider {
    public static final String TABLE_NAME = "punishment_ip_address";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (ip_address, type_id, log_id)
            VALUES (?, ?, ?)
            RETURNING ip_address, type_id, log_id
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE ip_address = ? AND type_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s
            SET log_id = ?
            WHERE ip_address = ? AND type_id = ?
            """.formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final PunishmentIpAddressCache cache;
    private final RefreshType all = RefreshType.PUNISHMENT_IPS;
    private final RefreshType single = RefreshType.SINGLE_PUNISHMENT_IP;

    public PunishmentIpAddressProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new PunishmentIpAddressCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable PunishmentIpAddress findPunishedIpAddress(@NotNull InetAddress inetAddress, int typeId) {
        return cache.get(inetAddress, typeId);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentIpAddress> findPunishedIpAddresses(int typeId) {
        return cache.getByTypeId(typeId);
    }

    @Override
    public @Nullable PunishmentIpAddress create(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID logId) {
        Objects.requireNonNull(inetAddress, "inetAddress must not be null");
        Objects.requireNonNull(logId, "logId must not be null");

        PunishmentIpAddress punish = MurmelExceptionWrapper.dbWrap(
                "Failed to create PunishmentIpAddress (inetAddress=" + inetAddress.getHostAddress() + ", typeId=" + typeId + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.punishmentIpAddress(), stmt -> {
                    stmt.setString(1, inetAddress.getHostAddress());
                    stmt.setInt(2, typeId);
                    stmt.setString(3, logId.toString());
                }),
                PunishmentException::new
        );

        if (punish == null) return null;
        refreshProvider.fireSingle(single, new PunishmentIpAddressCache.PunishKey(inetAddress, typeId));
        return punish;
    }

    @Override
    public int delete(@NotNull InetAddress inetAddress, int typeId) {
        Objects.requireNonNull(inetAddress, "inetAddress cannot be null");

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete PunishmentIpAddress (inetAddress=" + inetAddress.getHostAddress() + ", typeId=" + typeId + ")",
                () -> database.update(DELETE_SQL, stmt -> {
                    stmt.setString(1, inetAddress.getHostAddress());
                    stmt.setInt(2, typeId);
                }),
                PunishmentException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new PunishmentIpAddressCache.PunishKey(inetAddress, typeId));
        return row;
    }

    @Override
    public @Nullable PunishmentIpAddress update(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID logId) {
        Objects.requireNonNull(inetAddress, "inetAddress cannot be null");
        Objects.requireNonNull(logId, "logId cannot be null");

        PunishmentIpAddress existing = cache.get(inetAddress, typeId);
        if (existing == null) return null;

        if (Objects.equals(logId, existing.logId()))
            return existing;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update PunishmentIpAddress (inetAddress=" + inetAddress.getHostAddress() + ", typeId=" + typeId + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setString(1, logId.toString());
                    stmt.setString(2, inetAddress.getHostAddress());
                    stmt.setInt(3, typeId);
                }),
                PunishmentException::new
        );
        if (row != 1) return null;

        PunishmentIpAddress punish = PunishmentIpAddress.builder(existing)
                .logId(logId)
                .build();
        refreshProvider.fireSingle(single, new PunishmentIpAddressCache.PunishKey(inetAddress, typeId));
        return punish;
    }
}
