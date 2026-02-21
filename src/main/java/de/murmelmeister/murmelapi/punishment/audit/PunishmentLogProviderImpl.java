package de.murmelmeister.murmelapi.punishment.audit;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.punishment.PunishmentLogException;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.net.InetAddress;
import java.sql.Types;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class PunishmentLogProviderImpl implements PunishmentLogProvider {
    private static final String TABLE_NAME = "punishment_logs";

    @Language("MariaDB")
    private static final String SQL = """
            INSERT INTO %s (
                id,
                action,
                user_id,
                ip_address,
                reason_id,
                reason_type_id,
                reason_text,
                reason_duration,
                reason_auto_flag_ip,
                reason_auto_punish,
                created_by
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, action, user_id, ip_address, reason_id, reason_type_id, reason_text, reason_duration, reason_auto_flag_ip, reason_auto_punish, created_by, created_at
            """.formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final PunishmentLogCache cache;
    private final RefreshType all = RefreshType.PUNISHMENT_LOGS;
    private final RefreshType single = RefreshType.SINGLE_PUNISHMENT_LOG;

    public PunishmentLogProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new PunishmentLogCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable PunishmentLog findLog(@Nullable UUID logId) {
        return cache.getById(logId);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentLog> findLogs(@NotNull UUID userId) {
        return cache.getByUser(userId);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentLog> findLogs(@NotNull InetAddress inetAddress) {
        return cache.getByIp(inetAddress);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentLog> findLogs() {
        return cache.getAll();
    }

    private @Nullable PunishmentLog createLog(@NotNull PunishmentLog.Action action, @Nullable UUID userId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy) {
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);

        UUID logId = UUID.randomUUID();
        return MurmelExceptionWrapper.dbWrap(
                "Failed to create or modify PunishmentLog (logId=" + logId + ")",
                () -> database.query(SQL, null, ResultSetUtil.punishmentLog(), stmt -> {
                    stmt.setString(1, logId.toString());
                    stmt.setString(2, action.name());
                    stmt.setString(3, userId == null ? null : userId.toString());
                    stmt.setString(4, inetAddress == null ? null : inetAddress.getHostAddress());
                    stmt.setInt(5, reason.id());
                    stmt.setInt(6, reason.typeId());
                    stmt.setString(7, reason.reasonText());
                    stmt.setObject(8, reason.durationSecs(), Types.BIGINT);
                    stmt.setBoolean(9, reason.autoFlagIp());
                    stmt.setBoolean(10, reason.autoPunish());
                }),
                PunishmentLogException::new
        );
    }

    @Override
    public @Nullable PunishmentLog create(@Nullable UUID userId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy) {
        PunishmentLog.Action action = PunishmentLog.Action.CREATED;
        PunishmentLog log = createLog(action, userId, inetAddress, reason, createdBy);

        if (log == null) return null;
        refreshProvider.fireSingle(single, log);
        return log;
    }

    @Override
    public @Nullable PunishmentLog modify(@Nullable UUID userId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy) {
        PunishmentLog.Action action = PunishmentLog.Action.MODIFIED;
        PunishmentLog log = createLog(action, userId, inetAddress, reason, createdBy);

        if (log == null) return null;
        refreshProvider.fireSingle(single, log);
        return log;
    }

    @Override
    public @Nullable PunishmentLog revoke(@Nullable UUID userId, @Nullable InetAddress inetAddress, @NotNull PunishmentLog log, int createdBy) {
        Objects.requireNonNull(log, "log cannot be null");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);

        UUID logId = UUID.randomUUID();
        PunishmentLog.Action action = PunishmentLog.Action.REVOKED;

        PunishmentLog newLog = MurmelExceptionWrapper.dbWrap(
                "Failed to revoke PunishmentLog (logId=" + logId + ")",
                () -> database.query(SQL, null, ResultSetUtil.punishmentLog(), stmt -> {
                    stmt.setString(1, logId.toString());
                    stmt.setString(2, action.name());
                    stmt.setString(3, userId == null ? null : userId.toString());
                    stmt.setString(4, inetAddress == null ? null : inetAddress.getHostAddress());
                    stmt.setInt(5, log.reasonId() == null ? Types.NULL : log.reasonId());
                    stmt.setInt(6, log.reasonTypeId());
                    stmt.setString(7, log.reasonText());
                    stmt.setLong(8, log.reasonDuration() == null ? Types.NULL : log.reasonDuration());
                    stmt.setBoolean(9, log.reasonAutoFlagIp());
                    stmt.setBoolean(10, log.reasonAutoPunish());
                }),
                PunishmentLogException::new
        );

        if (newLog == null) return null;
        refreshProvider.fireSingle(single, newLog);
        return newLog;
    }
}
