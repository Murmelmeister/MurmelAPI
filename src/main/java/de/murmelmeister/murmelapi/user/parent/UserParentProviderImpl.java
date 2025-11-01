package de.murmelmeister.murmelapi.user.parent;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static de.murmelmeister.murmelapi.user.UserProviderImpl.CONSOLE_USER_ID;

/**
 * UserParentProvider is a class that provides methods to manage user-parent relationships in the database.
 * It implements the UserParent interface and uses the Database class to interact with the database.
 */
public final class UserParentProviderImpl implements UserParentProvider {
    private static final String TABLE_NAME = "user_parent";

    private final Database database;
    private final UserParentCache cache;
    private final RefreshType all = RefreshType.USER_PARENTS;
    private final RefreshType single = RefreshType.SINGLE_USER_PARENT;

    public UserParentProviderImpl(Database database, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.cache = new UserParentCache(database, TABLE_NAME, fetchLimit, cacheCapcity, refreshInterval);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "user_id INT, parent_id INT, " +
                "PRIMARY KEY (user_id, parent_id), " +
                "expires_at DATETIME NULL, " +
                "created_by INT NOT NULL, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(), " +
                "changed_by INT NULL, " +
                "changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(), " +
                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                "FOREIGN KEY (parent_id) REFERENCES groups(id)," +
                "FOREIGN KEY (created_by) REFERENCES users(id), " +
                "FOREIGN KEY (changed_by) REFERENCES users(id)");
        database.update("CREATE INDEX IF NOT EXISTS idx_user_parent_userId_exp ON " + TABLE_NAME + " (user_id, expires_at)");
    }

    @Override
    public void closeCache() {
        cache.close();
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public UserParent getParent(int userId, int parentId) {
        return cache.get(userId, parentId);
    }

    @Override
    public List<UserParent> getParents(int userId) {
        return cache.getParents(userId);
    }

    @Override
    public UserParent add(int userId, int parentId, long duration, int createdBy) {
        if (userId < 1 || parentId < 1 || duration < -1 || createdBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        String insertSql = "INSERT INTO " + TABLE_NAME + " (user_id, parent_id, expires_at, created_by) VALUES (?, ?, ?, ?)";
        int row = database.update(insertSql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setInt(2, parentId);
            stmt.setTimestamp(3, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(4, createdBy);
        });
        if (row < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE user_id = ? AND parent_id = ?";
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, parentId);
                });
        if (createdAt == null) return null;

        UserParent userParent = new UserParent(userId, parentId, expiresAt, createdBy, createdAt, null, null);
        RefreshUtil.fireSingle(single, new UserParentCache.ParentKey(userId, parentId));
        cache.put(userParent);
        return userParent;
    }

    @Override
    public int remove(int userId, int parentId) {
        if (userId < 1 || parentId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE user_id = ? AND parent_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setInt(2, parentId);
        });
        if (row < 1) return 0;

        cache.remove(userId, parentId);
        RefreshUtil.fireSingle(single, new UserParentCache.ParentKey(userId, parentId));
        return row;
    }

    @Override
    public int clear(int userId) {
        if (userId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE user_id = ?";
        int row = database.update(sql,
                stmt -> stmt.setInt(1, userId));
        if (row < 1) return 0;

        cache.remove(userId);
        RefreshUtil.fireSingle(single, userId);
        return row;
    }

    @Override
    public UserParent update(int userId, int parentId, long duration, int changedBy) {
        if (userId < 1 || parentId < 1 || duration < -1 || changedBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        UserParent existing = cache.get(userId, parentId);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing; // No changes, return existing

        String updateSql = "UPDATE " + TABLE_NAME + " SET expires_at = ?, changed_by = ? WHERE user_id = ? AND parent_id = ?";
        int row = database.update(updateSql, stmt -> {
            stmt.setTimestamp(1, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(2, changedBy);
            stmt.setInt(3, userId);
            stmt.setInt(4, parentId);
        });
        if (row < 1) return null;

        String selectSql = "SELECT changed_at FROM " + TABLE_NAME + " WHERE user_id = ? AND parent_id = ?";
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, parentId);
                });
        if (changedAt == null) return null;

        UserParent userParent = existing.withUpdateMeta(expiresAt, changedBy, changedAt);
        RefreshUtil.fireSingle(single, new UserParentCache.ParentKey(userId, parentId));
        cache.put(userParent);
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
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()";
        int removed = database.update(sql);

        // Refresh the cache
        expiredParents.forEach(key -> RefreshUtil.fireSingle(single, key));
        return removed;
    }
}
