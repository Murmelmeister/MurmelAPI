package de.murmelmeister.murmelapi.group.color;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

/**
 * The GroupColorProvider class provides methods to manage group colors in the database.
 * It allows creating, deleting, and updating group colors, as well as retrieving their properties.
 */
public final class GroupColorProviderImpl implements GroupColorProvider {
    private static final String TABLE_NAME = "group_color";

    private final Database database;
    private final GroupColorCache cache;
    private final RefreshType all = RefreshType.GROUP_COLORS;
    private final RefreshType single = RefreshType.SINGLE_GROUP_COLOR;

    public GroupColorProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new GroupColorCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "group_id INT, type_id INT, " +
                "PRIMARY KEY (group_id, type_id), " +
                "value TEXT NOT NULL, " +
                "created_by INT NOT NULL, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(), " +
                "changed_by INT NULL, " +
                "changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(), " +
                "FOREIGN KEY (group_id) REFERENCES groups(id), " +
                "FOREIGN KEY (type_id) REFERENCES group_color_type(id), " +
                "FOREIGN KEY (created_by) REFERENCES users(id), " +
                "FOREIGN KEY (changed_by) REFERENCES users(id)");
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public GroupColor getGroupColor(int groupId, int typeId) {
        return cache.get(groupId, typeId);
    }

    @Override
    public List<GroupColor> getGroupColors(int groupId) {
        return cache.getByGroupId(groupId);
    }

    @Override
    public List<GroupColor> getGroupColors() {
        return cache.getCachedColors();
    }

    @Override
    public GroupColor add(int groupId, int typeId, String value, int createdBy) {
        if (groupId < 1 || typeId < 1 || value == null || createdBy < CONSOLE_USER_ID)
            return null;

        String insertSql = "INSERT INTO " + TABLE_NAME + " (group_id, type_id, value, created_by) VALUES (?, ?, ?, ?)";
        int row = database.update(insertSql, stmt -> {
            stmt.setInt(1, groupId);
            stmt.setInt(2, typeId);
            stmt.setString(3, value);
            stmt.setInt(4, createdBy);
        });
        if (row < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE group_id = ? AND type_id = ?";
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setInt(2, typeId);
                });
        if (createdAt == null) return null;

        GroupColor groupColor = new GroupColor(groupId, typeId, value, createdBy, createdAt, null, null);
        RefreshUtil.fireSingle(single, new GroupColorCache.ColorKey(groupId, typeId));
        return groupColor;
    }

    @Override
    public int remove(int groupId, int typeId) {
        if (groupId < 1 || typeId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE group_id = ? AND type_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, groupId);
            stmt.setInt(2, typeId);
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new GroupColorCache.ColorKey(groupId, typeId));
        return row;
    }

    @Override
    public int clear(int groupId) {
        if (groupId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE group_id = ?";
        int row = database.update(sql,
                stmt -> stmt.setInt(1, groupId));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, groupId);
        return row;
    }

    @Override
    public GroupColor update(int groupId, int typeId, String value, int changedBy) {
        if (groupId < 1 || typeId < 1 || value == null || changedBy < CONSOLE_USER_ID)
            return null;

        GroupColor existing = cache.get(groupId, typeId);
        if (existing == null) return null;

        if (Objects.equals(value, existing.value()))
            return existing; // No changes, return existing

        String updateSql = "UPDATE " + TABLE_NAME + " SET value = ?, changed_by = ? WHERE group_id = ? AND type_id = ?";
        int row = database.update(updateSql, stmt -> {
            stmt.setString(1, value);
            stmt.setInt(2, changedBy);
            stmt.setInt(3, groupId);
            stmt.setInt(4, typeId);
        });
        if (row < 1) return null;

        String selectSql = "SELECT changed_at FROM " + TABLE_NAME + " WHERE group_id = ? AND type_id = ?";
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setInt(2, typeId);
                });
        if (changedAt == null) return null;

        GroupColor groupColor = existing.withUpdateMeta(value, changedBy, changedAt);
        RefreshUtil.fireSingle(single, new GroupColorCache.ColorKey(groupId, typeId));
        return groupColor;
    }
}
