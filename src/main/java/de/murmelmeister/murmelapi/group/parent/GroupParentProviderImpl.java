package de.murmelmeister.murmelapi.group.parent;

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
 * GroupParentProvider is a class that provides methods to manage group parents in the database.
 * It implements the GroupParent interface and uses the Database class to interact with the database.
 */
public final class GroupParentProviderImpl implements GroupParentProvider {
    private static final String TABLE_NAME = "group_parent";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final GroupParentCache cache;
    private final RefreshType all = RefreshType.GROUP_PARENTS;
    private final RefreshType single = RefreshType.SINGLE_GROUP_PARENT;

    public GroupParentProviderImpl(Database database, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new GroupParentCache(database, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable GroupParent getParent(int groupId, int parentId) {
        return cache.get(groupId, parentId);
    }

    @Override
    public @Nullable List<GroupParent> getParents(int groupId) {
        return cache.getParents(groupId);
    }

    @Override
    public @Nullable GroupParent add(int groupId, int parentId, long duration, int createdBy) {
        if (groupId < 1 || parentId < 1 || duration < -1 || createdBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiredAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (group_id, parent_id, expires_at, created_by)
                VALUES (?, ?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setInt(1, groupId);
            stmt.setInt(2, parentId);
            stmt.setTimestamp(3, expiredAt == null ? null : Timestamp.valueOf(expiredAt));
            stmt.setInt(4, createdBy);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT created_at FROM %s WHERE group_id = ? AND parent_id = ?".formatted(TABLE_NAME);
        LocalDateTime createAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setInt(2, parentId);
                });
        if (createAt == null) return null;

        GroupParent groupParent = new GroupParent(groupId, parentId, expiredAt, createdBy, createAt, null, null);
        refreshProvider.fireSingle(single, new GroupParentCache.ParentKey(groupId, parentId));
        return groupParent;
    }

    @Override
    public int remove(int groupId, int parentId) {
        if (groupId < 1 || parentId < 1) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE group_id = ? AND parent_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, groupId);
            stmt.setInt(2, parentId);
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new GroupParentCache.ParentKey(groupId, parentId));
        return row;
    }

    @Override
    public int clear(int groupId) {
        if (groupId < 1) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE group_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql,
                stmt -> stmt.setInt(1, groupId));
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new GroupParentCache.ParentKey(groupId, null));
        return row;
    }

    @Override
    public @Nullable GroupParent update(int groupId, int parentId, long duration, int changedBy) {
        if (groupId < 1 || parentId < 1 || duration < -1 || changedBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        GroupParent existing = cache.get(groupId, parentId);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing; // No changes, return existing

        @Language("MariaDB")
        String updateSql = "UPDATE %s SET expires_at = ?, changed_by = ? WHERE group_id = ? AND parent_id = ?".formatted(TABLE_NAME);
        int row = database.update(updateSql, stmt -> {
            stmt.setTimestamp(1, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(2, changedBy);
            stmt.setInt(3, groupId);
            stmt.setInt(4, parentId);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT changed_at FROM %s WHERE group_id = ? AND parent_id = ?".formatted(TABLE_NAME);
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setInt(2, parentId);
                });
        if (changedAt == null) return null;

        GroupParent groupParent = GroupParent.builder(existing)
                .expiresAt(expiresAt)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, new GroupParentCache.ParentKey(groupId, parentId));
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
        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()".formatted(TABLE_NAME);
        int removed = database.update(sql);

        // Refresh the cache
        expiredParents.forEach(key -> refreshProvider.fireSingle(single, key));
        return removed;
    }
}
