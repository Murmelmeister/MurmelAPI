package de.murmelmeister.murmelapi.permission.parent;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.permission.ParentException;
import de.murmelmeister.murmelapi.permission.PermissionTarget;
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

final class ParentProviderImpl implements ParentProvider {
    private static final String TABLE_NAME = "parents";

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (user_id, group_id, parent_id, expires_at, created_by)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                expires_at = VALUES(expires_at),
                changed_by = ?
            RETURNING id, user_id, group_id, parent_id, expires_at, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String REMOVE_USER_SQL = "DELETE FROM %s WHERE user_id = ? AND parent_id = ?".formatted(TABLE_NAME);
    @Language("MariaDB")
    private static final String REMOVE_GROUP_SQL = "DELETE FROM %s WHERE group_id = ? AND parent_id = ?".formatted(TABLE_NAME);
    @Language("MariaDB")
    private static final String CLEAR_USER_SQL = "DELETE FROM %s WHERE user_id = ?".formatted(TABLE_NAME);
    @Language("MariaDB")
    private static final String CLEAR_GROUP_SQL = "DELETE FROM %s WHERE group_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String EXPIRES_SQL = """
            DELETE FROM %s
            WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()
            RETURNING user_id, group_id, parent_id
            """.formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final ParentCache cache;
    private final RefreshType all = RefreshType.PARENTS;
    private final RefreshType single = RefreshType.SINGLE_PARENT;

    public ParentProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new ParentCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @NotNull Optional<Parent> findParent(@NotNull PermissionTarget target, int parentId) {
        return cache.getByKey(target, parentId);
    }

    @Override
    public @NotNull @Unmodifiable List<Parent> findParents(@NotNull PermissionTarget target) {
        return cache.getByTarget(target);
    }

    @Override
    public @NotNull Optional<Parent> upsert(@NotNull PermissionTarget target, int parentId, long duration, int executorId) {
        Objects.requireNonNull(target, "target cannot be null");
        if (target.id() < 1) throw new IllegalArgumentException("target id must be >= 1");
        if (duration < -1) throw new IllegalArgumentException("duration must be >= -1");
        if (executorId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("executorId must be >= " + CONSOLE_USER_ID);

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        Optional<Parent> optExisting = cache.getByKey(target, parentId);
        if (optExisting.isPresent()) {
            if (Objects.equals(expiresAt, optExisting.get().expiresAt()))
                return optExisting;
        }

        Parent parentEntry = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert Parent (target=" + target + ", parentId=" + parentId + ")",
                () -> database.query(UPSERT_SQL, null, ParentRowMapper::resultSet, stmt -> {
                    if (target.type() == PermissionTarget.TargetType.USER) {
                        stmt.setInt(1, target.id());
                        stmt.setNull(2, Types.INTEGER);
                    } else {
                        stmt.setNull(1, Types.INTEGER);
                        stmt.setInt(2, target.id());
                    }
                    stmt.setInt(3, parentId);
                    stmt.setObject(4, expiresAt, Types.TIMESTAMP);
                    stmt.setInt(5, executorId);
                    stmt.setInt(6, executorId);
                }),
                ParentException::new
        );

        if (parentEntry == null) return Optional.empty();
        refreshProvider.fireSingle(single, new ParentCache.ParentKey(target, parentId));
        return Optional.of(parentEntry);
    }

    @Override
    public int remove(@NotNull PermissionTarget target, int parentId) {
        Objects.requireNonNull(target, "target cannot be null");
        if (target.id() < 1) throw new IllegalArgumentException("target id must be >= 1");

        String sql = target.type() == PermissionTarget.TargetType.USER
                ? REMOVE_USER_SQL
                : REMOVE_GROUP_SQL;
        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to remove Parent (target=" + target + ", parentId=" + parentId + ")",
                () -> database.update(sql, stmt -> {
                    stmt.setInt(1, target.id());
                    stmt.setInt(2, parentId);
                }),
                ParentException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new ParentCache.ParentKey(target, parentId));
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
                "Failed to clear Parents (target=" + target + ")",
                () -> database.update(sql, stmt -> stmt.setInt(1, target.id())),
                ParentException::new
        );

        if (rows < 1) return 0;
        refreshProvider.fireSingle(single, new ParentCache.ParentKey(target, null));
        return rows;
    }

    @Override
    public int loadExpired() {
        List<ParentCache.ParentKey> expiredKeys = MurmelExceptionWrapper.dbWrap(
                "Failed to remove expired Parents",
                () -> database.queryList(
                        EXPIRES_SQL,
                        resultSet -> {
                            Integer userId = resultSet.getObject("user_id", Integer.class);
                            Integer groupId = resultSet.getObject("group_id", Integer.class);
                            int parentId = resultSet.getInt("parent_id");
                            PermissionTarget target = (userId != null)
                                    ? PermissionTarget.user(userId)
                                    : PermissionTarget.group(groupId);
                            return new ParentCache.ParentKey(target, parentId);
                        },
                        null
                ),
                ParentException::new
        );

        if (expiredKeys == null || expiredKeys.isEmpty())
            return 0;

        expiredKeys.forEach(key -> refreshProvider.fireSingle(single, key));
        return expiredKeys.size();
    }
}
