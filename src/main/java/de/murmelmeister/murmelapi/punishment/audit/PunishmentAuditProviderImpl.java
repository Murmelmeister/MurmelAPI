package de.murmelmeister.murmelapi.punishment.audit;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.punishment.PunishmentAuditException;
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
                created_by
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, action, mojang_id, ip_address, reason_id, reason_type_id, reason_text, reason_duration, reason_auto_flag_ip, created_by, created_at
            """.formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final PunishmentAuditCache cache;
    private final RefreshType all = RefreshType.PUNISHMENT_AUDITS;
    private final RefreshType single = RefreshType.SINGLE_PUNISHMENT_AUDIT;

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
    public @NotNull Optional<PunishmentAudit> findAudit(@NotNull UUID id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentAudit> findAudits(@NotNull UUID userId) {
        return cache.getByMojangId(userId);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentAudit> findAudits(@NotNull InetAddress inetAddress) {
        return cache.getByIpAddress(inetAddress);
    }

    private @NotNull Optional<PunishmentAudit> createLog(@NotNull PunishmentAudit.Action action, @Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int executorId) {
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        if (executorId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("executorId must be >= " + CONSOLE_USER_ID);

        UUID id = UUID.randomUUID();
        PunishmentAudit audit = MurmelExceptionWrapper.dbWrap(
                "Failed to create or modify PunishmentAudit (logId=" + id + ")",
                () -> database.query(SQL, null, ResultSetUtil.punishmentAudit(), stmt -> {
                    stmt.setString(1, id.toString());
                    stmt.setString(2, action.name());
                    stmt.setString(3, mojangId == null ? null : mojangId.toString());
                    stmt.setString(4, inetAddress == null ? null : inetAddress.getHostAddress());
                    stmt.setObject(5, reason.id(), Types.INTEGER);
                    stmt.setObject(6, reason.typeId(), Types.INTEGER);
                    stmt.setString(7, reason.reasonText());
                    stmt.setObject(8, reason.durationSecs(), Types.BIGINT);
                    stmt.setBoolean(9, reason.autoFlagIp());
                    stmt.setInt(10, executorId);
                }),
                PunishmentAuditException::new
        );

        return Optional.ofNullable(audit);
    }

    @Override
    public @NotNull Optional<PunishmentAudit> create(@Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int executorId) {
        PunishmentAudit.Action action = PunishmentAudit.Action.CREATED;
        Optional<PunishmentAudit> audit = createLog(action, mojangId, inetAddress, reason, executorId);

        if (audit.isEmpty()) return Optional.empty();
        refreshProvider.fireSingle(single, audit.get());
        return audit;
    }

    @Override
    public @NotNull Optional<PunishmentAudit> modify(@Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int executorId) {
        PunishmentAudit.Action action = PunishmentAudit.Action.MODIFIED;
        Optional<PunishmentAudit> audit = createLog(action, mojangId, inetAddress, reason, executorId);

        if (audit.isEmpty()) return Optional.empty();
        refreshProvider.fireSingle(single, audit.get());
        return audit;
    }

    @Override
    public @NotNull Optional<PunishmentAudit> revoke(@Nullable UUID mojangId, @Nullable InetAddress inetAddress, @NotNull PunishmentAudit audit, int executorId) {
        Objects.requireNonNull(audit, "audit cannot be null");
        if (executorId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("executorId must be >= " + CONSOLE_USER_ID);

        UUID id = UUID.randomUUID();
        PunishmentAudit.Action action = PunishmentAudit.Action.REVOKED;

        PunishmentAudit newAudit = MurmelExceptionWrapper.dbWrap(
                "Failed to revoke PunishmentAudit (id=" + id + ")",
                () -> database.query(SQL, null, ResultSetUtil.punishmentAudit(), stmt -> {
                    stmt.setString(1, id.toString());
                    stmt.setString(2, action.name());
                    stmt.setString(3, mojangId == null ? null : mojangId.toString());
                    stmt.setString(4, inetAddress == null ? null : inetAddress.getHostAddress());
                    stmt.setObject(5, audit.reasonId(), Types.INTEGER);
                    stmt.setObject(6, audit.reasonTypeId(), Types.INTEGER);
                    stmt.setString(7, audit.reasonText());
                    stmt.setObject(8, audit.reasonDuration(), Types.BIGINT);
                    stmt.setBoolean(9, audit.reasonAutoFlagIp());
                    stmt.setInt(10, executorId);
                }),
                PunishmentAuditException::new
        );

        if (newAudit == null) return Optional.empty();
        refreshProvider.fireSingle(single, newAudit);
        return Optional.of(newAudit);
    }
}
