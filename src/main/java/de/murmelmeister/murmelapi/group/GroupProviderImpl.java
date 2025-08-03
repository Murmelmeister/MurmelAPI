package de.murmelmeister.murmelapi.group;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.murmelmeister.murmelapi.user.UserProviderImpl.CONSOLE_USER_ID;

/**
 * GroupProvider is a class that provides methods to manage groups in the database.
 * It implements the Group interface and uses the Database class to interact with the database.
 */
public final class GroupProviderImpl implements GroupProvider {
    public static final int DEFAULT_GROUP_ID = 1;
    private static final String TABLE_NAME = "groups";

    private final Database database;
    private final GroupCache cache;
    private final RefreshType all = RefreshType.GROUPS;
    private final RefreshType single = RefreshType.SINGLE_GROUP;

    public GroupProviderImpl(Database database, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.cache = new GroupCache(database, TABLE_NAME, fetchLimit, cacheCapcity, refreshInterval);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY AUTO_INCREMENT, " +
                                         "group_name VARCHAR(100) NOT NULL UNIQUE, " +
                                         "team_tag_id VARCHAR(110) NOT NULL UNIQUE, " +
                                         "priority INT NOT NULL DEFAULT 0, " +
                                         "is_default BOOLEAN NOT NULL DEFAULT FALSE, " +
                                         "created_by INT NOT NULL, " +
                                         "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(), " +
                                         "changed_by INT NULL, " +
                                         "changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(), " +
                                         "FOREIGN KEY (created_by) REFERENCES users(id), " +
                                         "FOREIGN KEY (changed_by) REFERENCES users(id)");
    }

    public static void createDefaultGroup(Database database) {
        String sql = "INSERT IGNORE INTO " + TABLE_NAME + " (id, group_name, team_tag_id, priority, is_default, created_by) VALUES (?, ?, ?, ?, ?, ?)";
        database.update(sql, DEFAULT_GROUP_ID, "default", "9999Default", 1, true, CONSOLE_USER_ID);
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
    public Group create(String groupName, int priority, String tag, int createdBy) {
        if (groupName == null || priority < 0 || tag == null || createdBy < CONSOLE_USER_ID)
            return null;

        groupName = groupName.strip();
        tag = tag.strip();
        if (groupName.isEmpty() || tag.isEmpty()) return null;
        String teamId = tag + groupName;

        String insertSql = "INSERT INTO " + TABLE_NAME + " (group_name, priority, team_tag_id, created_by) " +
                           "VALUES (?, ?, ?, ?)";
        int groupId = database.updateAndGetAutoIncrement(insertSql, groupName, priority, teamId, createdBy);
        if (groupId < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime createdAt = database.query(selectSql, null, resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(), groupId);
        if (createdAt == null) return null;

        Group group = new Group(groupId, groupName, teamId, priority, false, createdBy, createdAt, null, null);
        RefreshUtil.fireSingle(single, groupId);
        cache.put(group);
        return group;
    }

    @Override
    public int delete(int groupId) {
        if (groupId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql, groupId);
        if (row < 1) return 0;

        cache.remove(groupId);
        RefreshUtil.fireSingle(single, groupId);
        return row;
    }

    @Override
    public Group update(int groupId, String groupName, int priority, String tag, int changedBy) {
        if (groupName == null || priority < 0 || tag == null || changedBy < CONSOLE_USER_ID)
            return null;

        groupName = groupName.strip();
        tag = tag.strip();
        if (groupName.isEmpty() || tag.isEmpty()) return null;
        String teamTagId = tag + groupName;

        Group existing = cache.getById(groupId);
        if (existing == null) return null;

        if (Objects.equals(groupName, existing.groupName()) &&
            Objects.equals(teamTagId, existing.teamTagId()) &&
            priority == existing.priority())
            return existing; // No changes, return an existing group

        String updateSql = "UPDATE " + TABLE_NAME + " SET group_name = ?, priority = ?, team_tag_id = ?, changed_by = ? WHERE id = ?";
        int row = database.update(updateSql, groupName, priority, teamTagId, changedBy, groupId);
        if (row < 1) return null;

        String selectSql = "SELECT changed_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime changedAt = database.query(selectSql, null, resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(), groupId);
        if (changedAt == null) return null;

        Group group = existing.withUpdateMeta(groupName, teamTagId, priority, changedBy, changedAt);
        RefreshUtil.fireSingle(single, groupId);
        cache.put(group);
        return group;
    }
}
