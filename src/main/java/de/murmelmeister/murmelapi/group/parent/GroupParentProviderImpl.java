package de.murmelmeister.murmelapi.group.parent;

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
 * GroupParentProvider is a class that provides methods to manage group parents in the database.
 * It implements the GroupParent interface and uses the Database class to interact with the database.
 */
public final class GroupParentProviderImpl implements GroupParentProvider {
    private static final String TABLE_NAME = "group_parent";

    private final Database database;
    private final GroupParentCache cache;
    private final RefreshType all = RefreshType.GROUP_PARENTS;
    private final RefreshType single = RefreshType.SINGLE_GROUP_PARENT;

    public GroupParentProviderImpl(Database database, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.cache = new GroupParentCache(database, TABLE_NAME, fetchLimit, cacheCapcity, refreshInterval);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "group_id INT, parent_id INT, " +
                "PRIMARY KEY (group_id, parent_id), " +
                "expires_at DATETIME NULL, " +
                "created_by INT NOT NULL, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(), " +
                "changed_by INT NULL, " +
                "changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(), " +
                "FOREIGN KEY (group_id) REFERENCES groups(id), " +
                "FOREIGN KEY (parent_id) REFERENCES groups(id), " +
                "FOREIGN KEY (created_by) REFERENCES users(id), " +
                "FOREIGN KEY (changed_by) REFERENCES users(id)");
        database.update("CREATE INDEX IF NOT EXISTS idx_group_parent_groupId_exp ON " + TABLE_NAME + " (group_id, expires_at)");
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
    public GroupParent getParent(int groupId, int parentId) {
        return cache.get(groupId, parentId);
    }

    @Override
    public List<GroupParent> getParents(int groupId) {
        return cache.getParents(groupId);
    }

    @Override
    public GroupParent add(int groupId, int parentId, long duration, int createdBy) {
        if (groupId < 1 || parentId < 1 || duration < -1 || createdBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiredAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        String insertSql = "INSERT INTO " + TABLE_NAME + " (group_id, parent_id, expires_at, created_by) VALUES (?, ?, ?, ?)";
        int row = database.update(insertSql, stmt -> {
            stmt.setInt(1, groupId);
            stmt.setInt(2, parentId);
            stmt.setTimestamp(3, expiredAt == null ? null : Timestamp.valueOf(expiredAt));
            stmt.setInt(4, createdBy);
        });
        if (row < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE group_id = ? AND parent_id = ?";
        LocalDateTime createAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setInt(2, parentId);
                });
        if (createAt == null) return null;

        GroupParent groupParent = new GroupParent(groupId, parentId, expiredAt, createdBy, createAt, null, null);
        RefreshUtil.fireSingle(single, new GroupParentCache.ParentKey(groupId, parentId));
        cache.put(groupParent);
        return groupParent;
    }

    @Override
    public int remove(int groupId, int parentId) {
        if (groupId < 1 || parentId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE group_id = ? AND parent_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, groupId);
            stmt.setInt(2, parentId);
        });
        if (row < 1) return 0;

        cache.remove(groupId, parentId);
        RefreshUtil.fireSingle(single, new GroupParentCache.ParentKey(groupId, parentId));
        return row;
    }

    @Override
    public int clear(int groupId) {
        if (groupId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE group_id = ?";
        int row = database.update(sql,
                stmt -> stmt.setInt(1, groupId));
        if (row < 1) return 0;

        cache.remove(groupId);
        RefreshUtil.fireSingle(single, groupId);
        return row;
    }

    @Override
    public GroupParent update(int groupId, int parentId, long duration, int changedBy) {
        if (groupId < 1 || parentId < 1 || duration < -1 || changedBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        GroupParent existing = cache.get(groupId, parentId);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing; // No changes, return existing

        String updateSql = "UPDATE " + TABLE_NAME + " SET expires_at = ?, changed_by = ? WHERE group_id = ? AND parent_id = ?";
        int row = database.update(updateSql, stmt -> {
            stmt.setTimestamp(1, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(2, changedBy);
            stmt.setInt(3, groupId);
            stmt.setInt(4, parentId);
        });
        if (row < 1) return null;

        String selectSql = "SELECT changed_at FROM " + TABLE_NAME + " WHERE group_id = ? AND parent_id = ?";
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setInt(2, parentId);
                });
        if (changedAt == null) return null;

        GroupParent groupParent = existing.withUpdateMeta(expiresAt, changedBy, changedAt);
        RefreshUtil.fireSingle(single, new GroupParentCache.ParentKey(groupId, parentId));
        cache.put(groupParent);
        return groupParent;
    }

    @Override
    public int loadExpired() {
        // Get all expired parents from the cache
        List<GroupParentCache.ParentKey> expiredParents = cache.getCachedParents().stream()
                .filter(GroupParent::isExpired)
                .map(parent -> new GroupParentCache.ParentKey(parent.groupId(), parent.parentId()))
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
