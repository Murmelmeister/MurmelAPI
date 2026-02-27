package de.murmelmeister.murmelapi.permission;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.permission.PermissionException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class PermissionProviderImpl implements PermissionProvider {
    private static final String TABLE_NAME = "permissions";

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (user_id, group_id, permission, expires_at, created_by)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                expires_at = VALUES(expires_at),
                changed_by = ?
            RETURNING id, user_id, group_id, permission, expires_at, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String REMOVE_USER_SQL = "DELETE FROM %s WHERE user_id = ? AND permission = ?".formatted(TABLE_NAME);
    @Language("MariaDB")
    private static final String REMOVE_GROUP_SQL = "DELETE FROM %s WHERE group_id = ? AND permission = ?".formatted(TABLE_NAME);
    @Language("MariaDB")
    private static final String CLEAR_USER_SQL = "DELETE FROM %s WHERE user_id = ?".formatted(TABLE_NAME);
    @Language("MariaDB")
    private static final String CLEAR_GROUP_SQL = "DELETE FROM %s WHERE group_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String EXPIRES_SQL = """
            DELETE FROM %s
            WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()
            RETURNING user_id, group_id, permission
            """.formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final PermissionCache cache;
    private final RefreshType all = RefreshType.PERMISSIONS;
    private final RefreshType single = RefreshType.SINGLE_PERMISSION;

    public PermissionProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new PermissionCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @NotNull Optional<Permission> findPermission(@NotNull PermissionTarget target, @NotNull String permission) {
        return cache.getByKey(target, permission);
    }

    @Override
    public @NotNull @Unmodifiable List<Permission> findPermissions(@NotNull PermissionTarget target) {
        return cache.getByTarget(target);
    }

    @Override
    public @NotNull Optional<Permission> upsert(@NotNull PermissionTarget target, @NotNull String permission, long duration, int executorId) {
        Objects.requireNonNull(target, "target cannot be null");
        Objects.requireNonNull(permission, "permission cannot be null");
        if (target.id() < 1) throw new IllegalArgumentException("target id must be >= 1");
        if (duration < -1) throw new IllegalArgumentException("duration must be >= -1");
        if (executorId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("executorId must be >= " + CONSOLE_USER_ID);

        String normalizedPermission = StringUtil.normalize(permission);
        if (normalizedPermission == null || normalizedPermission.isBlank())
            throw new IllegalArgumentException("permission cannot be blank");

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        Optional<Permission> optExisting = cache.getByKey(target, normalizedPermission);
        if (optExisting.isPresent()) {
            if (Objects.equals(expiresAt, optExisting.get().expiresAt()))
                return optExisting;
        }

        Permission permissionEntry = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert Permission (target=" + target + ", permission=" + normalizedPermission + ")",
                () -> database.query(UPSERT_SQL, null, ResultSetUtil.permission(), stmt -> {
                    if (target.type() == PermissionTarget.TargetType.USER) {
                        stmt.setInt(1, target.id());
                        stmt.setNull(2, Types.INTEGER);
                    } else {
                        stmt.setNull(1, Types.INTEGER);
                        stmt.setInt(2, target.id());
                    }
                    stmt.setString(3, normalizedPermission);
                    stmt.setObject(4, expiresAt, Types.TIMESTAMP);
                    stmt.setInt(5, executorId);
                    stmt.setInt(6, executorId);
                }),
                PermissionException::new
        );

        if (permissionEntry == null) return Optional.empty();
        refreshProvider.fireSingle(single, new PermissionCache.PermissionKey(target, normalizedPermission));
        return Optional.of(permissionEntry);
    }

    @Override
    public int remove(@NotNull PermissionTarget target, @NotNull String permission) {
        Objects.requireNonNull(target, "target cannot be null");
        Objects.requireNonNull(permission, "permission cannot be null");
        if (target.id() < 1) throw new IllegalArgumentException("target id must be >= 1");

        String normalizedPermission = StringUtil.normalize(permission);
        if (normalizedPermission == null || normalizedPermission.isBlank())
            throw new IllegalArgumentException("permission cannot be blank");

        String sql = target.type() == PermissionTarget.TargetType.USER
                ? REMOVE_USER_SQL
                : REMOVE_GROUP_SQL;
        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to remove Permission (target=" + target + ", permission=" + normalizedPermission + ")",
                () -> database.update(sql, stmt -> {
                    stmt.setInt(1, target.id());
                    stmt.setString(2, normalizedPermission);
                }),
                PermissionException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new PermissionCache.PermissionKey(target, normalizedPermission));
        return row;
    }

    @Override
    public int clear(@NotNull PermissionTarget target) {
        Objects.requireNonNull(target, "target cannot be null");
        if (target.id() < 1) throw new IllegalArgumentException("target id must be >= 1");

        String sql = target.type() == PermissionTarget.TargetType.USER
                ? CLEAR_USER_SQL
                : CLEAR_GROUP_SQL;
        int rows = MurmelExceptionWrapper.dbWrap(
                "Failed to clear Permissions (target=" + target + ")",
                () -> database.update(sql, stmt -> stmt.setInt(1, target.id())),
                PermissionException::new
        );

        if (rows < 1) return 0;
        refreshProvider.fireSingle(single, new PermissionCache.PermissionKey(target, null));
        return rows;
    }

    @Override
    public int loadExpired() {
        List<PermissionCache.PermissionKey> expiredKeys = MurmelExceptionWrapper.dbWrap(
                "Failed to remove expired Permissions",
                () -> database.queryList(
                        EXPIRES_SQL,
                        resultSet -> {
                            Integer userId = resultSet.getObject("user_id", Integer.class);
                            Integer groupId = resultSet.getObject("group_id", Integer.class);
                            String permission = resultSet.getString("permission");
                            PermissionTarget target = (userId != null)
                                    ? PermissionTarget.user(userId)
                                    : PermissionTarget.group(groupId);
                            return new PermissionCache.PermissionKey(target, permission);
                        },
                        null
                ),
                PermissionException::new
        );

        if (expiredKeys == null || expiredKeys.isEmpty())
            return 0;

        expiredKeys.forEach(key -> refreshProvider.fireSingle(single, key));
        return expiredKeys.size();
    }
}
