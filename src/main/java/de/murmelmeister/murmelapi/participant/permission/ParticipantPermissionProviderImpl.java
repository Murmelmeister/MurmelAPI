package de.murmelmeister.murmelapi.participant.permission;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.participant.ParticipantPermissionException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class ParticipantPermissionProviderImpl implements ParticipantPermissionProvider {
    private static final String TABLE_NAME = "participant_permissions";

    @Language("MariaDB")
    private static final String ADD_SQL = """
            INSERT INTO %s (participant_id, permission, expires_at, created_by)
            VALUES (?, ?, ?, ?)
            RETURNING id, participant_id, permission, expires_at, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String REMOVE_SQL = "DELETE FROM %s WHERE participant_id = ? AND permission = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String CLEAR_SQL = "DELETE FROM %s WHERE participant_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s SET
                expires_at = ?,
                changed_by = ?
            WHERE participant_id = ? AND permission = ?
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SELECT_SQL = "SELECT changed_at FROM %s WHERE participant_id = ? AND permission = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String EXPIRES_SQL = "DELETE FROM %s WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()".formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final ParticipantPermissionCache cache;
    private final RefreshType all = RefreshType.PARTICIPANT_PERMISSIONS;
    private final RefreshType single = RefreshType.SINGLE_PARTICIPANT_PERMISSION;

    public ParticipantPermissionProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new ParticipantPermissionCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable ParticipantPermission findPermission(int participantId, @NotNull String permission) {
        return cache.getByKey(participantId, permission);
    }

    @Override
    public @NotNull @Unmodifiable List<ParticipantPermission> findPermissions(int participantId) {
        return cache.getByParticipant(participantId);
    }

    @Override
    public @Nullable ParticipantPermission add(int participantId, @NotNull String permission, long duration, int createdBy) {
        Objects.requireNonNull(permission, "permission cannot be null");
        if (participantId < 1) throw new IllegalArgumentException("participantId must be >= 1");
        if (duration < -1) throw new IllegalArgumentException("duration must be >= -1");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        String normalizedPermission = StringUtil.normalize(permission);
        if (normalizedPermission == null || normalizedPermission.isBlank())
            throw new IllegalArgumentException("permission cannot be blank");

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        ParticipantPermission permissionEntry = MurmelExceptionWrapper.dbWrap(
                "Failed to add ParticipantPermission (participantId=" + participantId + ", permission=" + normalizedPermission + ")",
                () -> database.query(ADD_SQL, null, ResultSetUtil.participantPermission(), stmt -> {
                    stmt.setInt(1, participantId);
                    stmt.setString(2, normalizedPermission);
                    stmt.setObject(3, expiresAt, Types.TIMESTAMP);
                    stmt.setInt(4, createdBy);
                }),
                ParticipantPermissionException::new
        );

        if (permissionEntry == null) return null;
        refreshProvider.fireSingle(single, new ParticipantPermissionCache.PermissionKey(participantId, normalizedPermission));
        return permissionEntry;
    }

    @Override
    public int remove(int participantId, @NotNull String permission) {
        Objects.requireNonNull(permission, "permission cannot be null");
        if (participantId < 1) throw new IllegalArgumentException("participantId must be >= 1");
        String normalizedPermission = StringUtil.normalize(permission);
        if (normalizedPermission == null || normalizedPermission.isBlank())
            throw new IllegalArgumentException("permission cannot be blank");

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to remove ParticipantPermission (participantId=" + participantId + ", permission=" + normalizedPermission + ")",
                () -> database.update(REMOVE_SQL, stmt -> {
                    stmt.setInt(1, participantId);
                    stmt.setString(2, normalizedPermission);
                }),
                ParticipantPermissionException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new ParticipantPermissionCache.PermissionKey(participantId, normalizedPermission));
        return row;
    }

    @Override
    public int clear(int participantId) {
        if (participantId < 1) throw new IllegalArgumentException("participantId must be >= 1");

        int rows = MurmelExceptionWrapper.dbWrap(
                "Failed to clear ParticipantPermissions (participantId=" + participantId + ")",
                () -> database.update(CLEAR_SQL, stmt -> stmt.setInt(1, participantId)),
                ParticipantPermissionException::new
        );

        if (rows < 1) return 0;
        refreshProvider.fireSingle(single, new ParticipantPermissionCache.PermissionKey(participantId, null));
        return rows;
    }

    @Override
    public @Nullable ParticipantPermission update(int participantId, @NotNull String permission, long duration, int changedBy) {
        Objects.requireNonNull(permission, "permission cannot be null");
        if (participantId < 1) throw new IllegalArgumentException("participantId must be >= 1");
        if (duration < -1) throw new IllegalArgumentException("duration must be >= -1");
        if (changedBy < CONSOLE_USER_ID) throw new IllegalArgumentException("changedBy must be >= " + CONSOLE_USER_ID);
        String normalizedPermission = StringUtil.normalize(permission);
        if (normalizedPermission == null || normalizedPermission.isBlank())
            throw new IllegalArgumentException("permission cannot be blank");

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        ParticipantPermission existing = cache.getByKey(participantId, normalizedPermission);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update ParticipantPermission (participantId=" + participantId + ", permission=" + normalizedPermission + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setObject(1, expiresAt, Types.TIMESTAMP);
                    stmt.setInt(2, changedBy);
                    stmt.setInt(3, participantId);
                    stmt.setString(4, normalizedPermission);
                }),
                ParticipantPermissionException::new
        );
        if (row != 1) return null;

        LocalDateTime changedAt = MurmelExceptionWrapper.dbWrap(
                "Failed to get changedAt for ParticipantPermission (participantId=" + participantId + ", permission=" + normalizedPermission + ")",
                () -> database.query(UPDATE_SELECT_SQL, null,
                        resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                        stmt -> {
                            stmt.setInt(1, participantId);
                            stmt.setString(2, normalizedPermission);
                        }),
                ParticipantPermissionException::new
        );
        if (changedAt == null) return null;

        ParticipantPermission updated = ParticipantPermission.builder(existing)
                .expiresAt(expiresAt)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, new ParticipantPermissionCache.PermissionKey(participantId, normalizedPermission));
        return updated;
    }

    @Override
    public int loadExpired() {
        List<ParticipantPermissionCache.PermissionKey> expiredPermission = cache.getAll().stream()
                .filter(ParticipantPermission::isExpired)
                .map(permission -> new ParticipantPermissionCache.PermissionKey(permission.participantId(), permission.permission()))
                .toList();
        if (expiredPermission.isEmpty()) return 0;

        int removed = MurmelExceptionWrapper.dbWrap(
                "Failed to remove expired ParticipantPermissions",
                () -> database.update(EXPIRES_SQL),
                ParticipantPermissionException::new
        );

        if (removed < 1) return 0;
        expiredPermission.forEach(key -> refreshProvider.fireSingle(single, key));
        return removed;
    }
}
