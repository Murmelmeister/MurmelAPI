package de.murmelmeister.murmelapi.user.parent;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

/**
 * UserParentProvider is a class that provides methods to manage user-parent relationships in the database.
 * It implements the UserParent interface and uses the Database class to interact with the database.
 */
public final class UserParentProviderImpl implements UserParentProvider {
    private static final String TABLE_NAME = "user_parent";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final UserParentCache cache;
    private final RefreshType all = RefreshType.USER_PARENTS;
    private final RefreshType single = RefreshType.SINGLE_USER_PARENT;

    public UserParentProviderImpl(Database database, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new UserParentCache(database, refreshProvider, TABLE_NAME, fetchLimit, cacheCapcity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable UserParent getParent(int userId, int parentId) {
        return cache.get(userId, parentId);
    }

    @Override
    public @Nullable List<UserParent> getParents(int userId) {
        return cache.getParents(userId);
    }

    @Override
    public @Nullable UserParent add(int userId, int parentId, long duration, int createdBy) {
        if (userId < 1 || parentId < 1 || duration < -1 || createdBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (user_id, parent_id, expires_at, created_by)
                VALUES (?, ?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setInt(2, parentId);
            stmt.setTimestamp(3, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(4, createdBy);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT created_at FROM %s WHERE user_id = ? AND parent_id = ?".formatted(TABLE_NAME);
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, parentId);
                });
        if (createdAt == null) return null;

        UserParent userParent = new UserParent(userId, parentId, expiresAt, createdBy, createdAt, null, null);
        refreshProvider.fireSingle(single, new UserParentCache.ParentKey(userId, parentId));
        return userParent;
    }

    @Override
    public int remove(int userId, int parentId) {
        if (userId < 1 || parentId < 1) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE user_id = ? AND parent_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setInt(2, parentId);
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new UserParentCache.ParentKey(userId, parentId));
        return row;
    }

    @Override
    public int clear(int userId) {
        if (userId < 1) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE user_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql,
                stmt -> stmt.setInt(1, userId));
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, userId);
        return row;
    }

    @Override
    public @Nullable UserParent update(int userId, int parentId, long duration, int changedBy) {
        if (userId < 1 || parentId < 1 || duration < -1 || changedBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        UserParent existing = cache.get(userId, parentId);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing; // No changes, return existing

        @Language("MariaDB")
        String updateSql = "UPDATE %s SET expires_at = ?, changed_by = ? WHERE user_id = ? AND parent_id = ?".formatted(TABLE_NAME);
        int row = database.update(updateSql, stmt -> {
            stmt.setTimestamp(1, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(2, changedBy);
            stmt.setInt(3, userId);
            stmt.setInt(4, parentId);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT changed_at FROM %s WHERE user_id = ? AND parent_id = ?".formatted(TABLE_NAME);
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, parentId);
                });
        if (changedAt == null) return null;

        UserParent userParent = UserParent.builder(existing)
                .expiresAt(expiresAt)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, new UserParentCache.ParentKey(userId, parentId));
        return userParent;
    }

    @Override
    public int loadExpired() {
        // Get all expired parents from the cache
        List<UserParentCache.ParentKey> expiredParents = cache.getCachedParents().stream()
                .filter(UserParent::isExpired)
                .map(parent -> new UserParentCache.ParentKey(parent.userId(), parent.parentId()))
                .toList();
        if (expiredParents.isEmpty()) return 0;

        // Delete expired parents from the database
        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()".formatted(TABLE_NAME);
        int removed = database.update(sql);

        // Refresh the cache
        expiredParents.forEach(key -> refreshProvider.fireSingle(single, key));
        return removed;
    }
}
