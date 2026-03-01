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
import java.util.Optional;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class PunishmentAuditProviderImpl implements PunishmentAuditProvider {
    private static final String TABLE_NAME = "punishment_audit";

    @Language("MariaDB")
    private static final String SQL = """
            INSERT INTO %s (
                id,
                action,
                mojang_id,
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
            RETURNING id, action, mojang_id, ip_address, reason_id, reason_type_id, reason_text, reason_duration, reason_auto_flag_ip, reason_auto_punish, created_by, created_at
            """.formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final PunishmentAuditCache cache;
    private final RefreshType all = RefreshType.PUNISHMENT_LOGS;
    private final RefreshType single = RefreshType.SINGLE_PUNISHMENT_LOG;

    public PunishmentAuditProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new PunishmentAuditCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @NotNull Optional<PunishmentAudit> findLog(@NotNull UUID logId) {
        return cache.getByLogId(logId);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentAudit> findLogs(@NotNull UUID userId) {
        return cache.getByMojangId(userId);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentAudit> findLogs(@NotNull InetAddress inetAddress) {
        return cache.getByIpAddress(inetAddress);
    }

    private @NotNull Optional<PunishmentAudit> createLog(@NotNull PunishmentAudit.Action action, @Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy) {
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);

        UUID logId = UUID.randomUUID();
        PunishmentAudit log = MurmelExceptionWrapper.dbWrap(
                "Failed to create or modify PunishmentAudit (logId=" + logId + ")",
                () -> database.query(SQL, null, ResultSetUtil.punishmentLog(), stmt -> {
                    stmt.setString(1, logId.toString());
                    stmt.setString(2, action.name());
                    stmt.setString(3, mojangId == null ? null : mojangId.toString());
                    stmt.setString(4, inetAddress == null ? null : inetAddress.getHostAddress());
                    stmt.setObject(5, reason.id(), Types.INTEGER);
                    stmt.setObject(6, reason.typeId(), Types.INTEGER);
                    stmt.setString(7, reason.reasonText());
                    stmt.setObject(8, reason.durationSecs(), Types.BIGINT);
                    stmt.setBoolean(9, reason.autoFlagIp());
                    stmt.setBoolean(10, reason.autoPunish());
                }),
                PunishmentLogException::new
        );

        return Optional.ofNullable(log);
    }

    @Override
    public @NotNull Optional<PunishmentAudit> create(@Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy) {
        PunishmentAudit.Action action = PunishmentAudit.Action.CREATED;
        Optional<PunishmentAudit> log = createLog(action, mojangId, inetAddress, reason, createdBy);

        if (log.isEmpty()) return Optional.empty();
        refreshProvider.fireSingle(single, log.get());
        return log;
    }

    @Override
    public @NotNull Optional<PunishmentAudit> modify(@Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy) {
        PunishmentAudit.Action action = PunishmentAudit.Action.MODIFIED;
        Optional<PunishmentAudit> log = createLog(action, mojangId, inetAddress, reason, createdBy);

        if (log.isEmpty()) return Optional.empty();
        refreshProvider.fireSingle(single, log.get());
        return log;
    }

    @Override
    public @NotNull Optional<PunishmentAudit> revoke(@Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentAudit log, int createdBy) {
        Objects.requireNonNull(log, "log cannot be null");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);

        UUID logId = UUID.randomUUID();
        PunishmentAudit.Action action = PunishmentAudit.Action.REVOKED;

        PunishmentAudit newLog = MurmelExceptionWrapper.dbWrap(
                "Failed to revoke PunishmentAudit (logId=" + logId + ")",
                () -> database.query(SQL, null, ResultSetUtil.punishmentLog(), stmt -> {
                    stmt.setString(1, logId.toString());
                    stmt.setString(2, action.name());
                    stmt.setString(3, mojangId == null ? null : mojangId.toString());
                    stmt.setString(4, inetAddress == null ? null : inetAddress.getHostAddress());
                    stmt.setObject(5, log.reasonId(), Types.INTEGER);
                    stmt.setObject(6, log.reasonTypeId(), Types.INTEGER);
                    stmt.setString(7, log.reasonText());
                    stmt.setObject(8, log.reasonDuration(), Types.BIGINT);
                    stmt.setBoolean(9, log.reasonAutoFlagIp());
                    stmt.setBoolean(10, log.reasonAutoPunish());
                }),
                PunishmentLogException::new
        );

        if (newLog == null) return Optional.empty();
        refreshProvider.fireSingle(single, newLog);
        return Optional.of(newLog);
    }
}
