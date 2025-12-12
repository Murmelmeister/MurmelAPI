package de.murmelmeister.murmelapi.group;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

/**
 * GroupProvider is a class that provides methods to manage groups in the database.
 * It implements the Group interface and uses the Database class to interact with the database.
 */
public final class GroupProviderImpl implements GroupProvider {
    private static final String TABLE_NAME = "groups";

    private final Database database;
    private final GroupCache cache;
    private final RefreshType all = RefreshType.GROUPS;
    private final RefreshType single = RefreshType.SINGLE_GROUP;

    public GroupProviderImpl(Database database, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.cache = new GroupCache(database, TABLE_NAME, fetchLimit, cacheCapcity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public Group findById(int id) {
        return cache.getById(id);
    }

    @Override
    public Group findByName(String groupName) {
        return cache.getByName(groupName);
    }

    @Override
    public List<Group> findAll() {
        return cache.getCachedGroups();
    }

    @Override
    public List<String> findAllGroupNames() {
        return findAll().stream().map(Group::groupName).collect(Collectors.toList());
    }

    @Override
    public Group create(String groupName, int priority, int createdBy) {
        String normalizedGroupName = StringUtil.normalize(groupName);
        if (normalizedGroupName == null || priority < 0 || createdBy < CONSOLE_USER_ID)
            return null;

        String insertSql = "INSERT INTO " + TABLE_NAME + " (group_name, priority, created_by) " +
                "VALUES (?, ?, ?)";
        int groupId = (int) database.updateAndGetGeneratedKeys(insertSql, stmt -> {
            stmt.setString(1, normalizedGroupName);
            stmt.setInt(2, priority);
            stmt.setInt(3, createdBy);
        });
        if (groupId < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> stmt.setInt(1, groupId));
        if (createdAt == null) return null;

        Group group = new Group(groupId, normalizedGroupName, priority, false, createdBy, createdAt, null, null);
        RefreshUtil.fireSingle(single, groupId);
        return group;
    }

    @Override
    public int delete(int groupId) {
        if (groupId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql,
                stmt -> stmt.setInt(1, groupId));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, groupId);
        return row;
    }

    @Override
    public Group update(int groupId, String groupName, int priority, int changedBy) {
        String normalizedGroupName = StringUtil.normalize(groupName);
        if (normalizedGroupName == null || priority < 0 || changedBy < CONSOLE_USER_ID)
            return null;

        Group existing = cache.getById(groupId);
        if (existing == null) return null;

        if (Objects.equals(normalizedGroupName, existing.groupName()) &&
                priority == existing.priority())
            return existing; // No changes, return an existing group

        String updateSql = "UPDATE " + TABLE_NAME + " SET group_name = ?, priority = ?, changed_by = ? WHERE id = ?";
        int row = database.update(updateSql, stmt -> {
            stmt.setString(1, normalizedGroupName);
            stmt.setInt(2, priority);
            stmt.setInt(3, changedBy);
            stmt.setInt(4, groupId);
        });
        if (row < 1) return null;

        String selectSql = "SELECT changed_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> stmt.setInt(1, groupId));
        if (changedAt == null) return null;

        Group group = existing.withUpdateMeta(normalizedGroupName, priority, changedBy, changedAt);
        RefreshUtil.fireSingle(single, groupId);
        return group;
    }
}
