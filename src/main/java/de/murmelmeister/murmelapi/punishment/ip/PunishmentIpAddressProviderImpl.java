package de.murmelmeister.murmelapi.punishment.ip;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.punishment.PunishmentIpAddressException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PunishmentIpAddressProviderImpl implements PunishmentIpAddressProvider {
    public static final String TABLE_NAME = "punishment_ip_address";

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (ip_address, type_id, log_id, expires_at)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                log_id = VALUES(log_id),
                expires_at = VALUES(expires_at)
            RETURNING ip_address, type_id, log_id, expires_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE ip_address = ? AND type_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String EXPIRES_SQL = """
            DELETE FROM %s
            WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()
            RETURNING ip_address, type_id
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
    public @NotNull Optional<PunishmentIpAddress> findPunishedIpAddress(@NotNull InetAddress inetAddress, int typeId) {
        return cache.getByKey(inetAddress, typeId);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentIpAddress> findPunishedIpAddresses(int typeId) {
        return cache.getByTypeId(typeId);
    }

    @Override
    public @NotNull Optional<PunishmentIpAddress> upsert(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID logId, @Nullable Long durationSecs) {
        Objects.requireNonNull(inetAddress, "inetAddress must not be null");
        Objects.requireNonNull(logId, "logId must not be null");
        if (durationSecs != null && durationSecs < 0) throw new IllegalArgumentException("durationSecs must be null or >= 0");

        LocalDateTime expiresAt = durationSecs != null ? LocalDateTime.now().plusSeconds(durationSecs) : null;
        Optional<PunishmentIpAddress> optExisting = cache.getByKey(inetAddress, typeId);
        if (optExisting.isPresent()) {
            if (Objects.equals(logId, optExisting.get().auditId())
                    && Objects.equals(expiresAt, optExisting.get().expiresAt()))
                return optExisting;
        }

        PunishmentIpAddress punish = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert PunishmentIpAddress (inetAddress=" + inetAddress.getHostAddress() + ", typeId=" + typeId + ")",
                () -> database.query(UPSERT_SQL, null, ResultSetUtil.punishmentIpAddress(), stmt -> {
                    stmt.setString(1, inetAddress.getHostAddress());
                    stmt.setInt(2, typeId);
                    stmt.setString(3, logId.toString());
                    stmt.setObject(4, expiresAt, Types.TIMESTAMP);
                }),
                PunishmentIpAddressException::new
        );

        if (punish == null) return Optional.empty();
        refreshProvider.fireSingle(single, new PunishmentIpAddressCache.PunishKey(inetAddress, typeId));
        return Optional.of(punish);
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
                PunishmentIpAddressException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new PunishmentIpAddressCache.PunishKey(inetAddress, typeId));
        return row;
    }

    @Override
    public int loadExpired() {
        List<PunishmentIpAddressCache.PunishKey> expiredKeys = MurmelExceptionWrapper.dbWrap(
                "Failed to remove expired PunishmentIpAddresses",
                () -> database.queryList(
                        EXPIRES_SQL,
                        resultSet -> {
                            InetAddress inetAddress;
                            try {
                                inetAddress = InetAddress.getByName(resultSet.getString("ip_address"));
                            } catch (UnknownHostException e) {
                                throw new RuntimeException(e);
                            }
                            int typeId = resultSet.getInt("type_id");
                            return new PunishmentIpAddressCache.PunishKey(inetAddress, typeId);
                        },
                        null
                ),
                PunishmentIpAddressException::new
        );

        if (expiredKeys == null || expiredKeys.isEmpty())
            return 0;

        expiredKeys.forEach(key -> refreshProvider.fireSingle(single, key));
        return expiredKeys.size();
    }
}
