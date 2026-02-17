package de.murmelmeister.murmelapi.group.color;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private final RefreshProvider refreshProvider;
    private final GroupColorCache cache;
    private final RefreshType all = RefreshType.GROUP_COLORS;
    private final RefreshType single = RefreshType.SINGLE_GROUP_COLOR;

    public GroupColorProviderImpl(Database database, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new GroupColorCache(database, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable GroupColor getGroupColor(int groupId, int typeId) {
        return cache.get(groupId, typeId);
    }

    @Override
    public @Nullable List<GroupColor> getGroupColors(int groupId) {
        return cache.getByGroupId(groupId);
    }

    @Override
    public @NotNull List<GroupColor> getGroupColors() {
        return cache.getCachedColors();
    }

    @Override
    public @Nullable GroupColor add(int groupId, int typeId, @NotNull String value, int createdBy) {
        if (groupId < 1 || typeId < 1 || createdBy < CONSOLE_USER_ID)
            return null;

        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (group_id, type_id, value, created_by)
                VALUES (?, ?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setInt(1, groupId);
            stmt.setInt(2, typeId);
            stmt.setString(3, value);
            stmt.setInt(4, createdBy);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT created_at FROM %s WHERE group_id = ? AND type_id = ?".formatted(TABLE_NAME);
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setInt(2, typeId);
                });
        if (createdAt == null) return null;

        GroupColor groupColor = new GroupColor(groupId, typeId, value, createdBy, createdAt, null, null);
        refreshProvider.fireSingle(single, new GroupColorCache.ColorKey(groupId, typeId));
        return groupColor;
    }

    @Override
    public int remove(int groupId, int typeId) {
        if (groupId < 1 || typeId < 1) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE group_id = ? AND type_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, groupId);
            stmt.setInt(2, typeId);
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new GroupColorCache.ColorKey(groupId, typeId));
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

        refreshProvider.fireSingle(single, new GroupColorCache.ColorKey(groupId, null));
        return row;
    }

    @Override
    public @Nullable GroupColor update(int groupId, int typeId, @NotNull String value, int changedBy) {
        if (groupId < 1 || typeId < 1 || changedBy < CONSOLE_USER_ID)
            return null;

        GroupColor existing = cache.get(groupId, typeId);
        if (existing == null) return null;

        if (Objects.equals(value, existing.value()))
            return existing; // No changes, return existing

        @Language("MariaDB")
        String updateSql = "UPDATE %s SET value = ?, changed_by = ? WHERE group_id = ? AND type_id = ?".formatted(TABLE_NAME);
        int row = database.update(updateSql, stmt -> {
            stmt.setString(1, value);
            stmt.setInt(2, changedBy);
            stmt.setInt(3, groupId);
            stmt.setInt(4, typeId);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT changed_at FROM %s WHERE group_id = ? AND type_id = ?".formatted(TABLE_NAME);
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setInt(2, typeId);
                });
        if (changedAt == null) return null;

        GroupColor groupColor = GroupColor.builder(existing)
                .value(value)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, new GroupColorCache.ColorKey(groupId, typeId));
        return groupColor;
    }
}
