package de.murmelmeister.murmelapi.group;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private final RefreshProvider refreshProvider;
    private final GroupCache cache;
    private final RefreshType all = RefreshType.GROUPS;
    private final RefreshType single = RefreshType.SINGLE_GROUP;

    public GroupProviderImpl(Database database, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new GroupCache(database, refreshProvider, TABLE_NAME, fetchLimit, cacheCapcity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable Group findById(int id) {
        return cache.getById(id);
    }

    @Override
    public @Nullable Group findByName(@Nullable String groupName) {
        return cache.getByName(groupName);
    }

    @Override
    public @NotNull List<Group> findAll() {
        return cache.getCachedGroups();
    }

    @Override
    public @NotNull List<String> findAllGroupNames() {
        return findAll().stream().map(Group::groupName).collect(Collectors.toList());
    }

    @Override
    public @Nullable Group create(@NotNull String groupName, int priority, int createdBy) {
        String normalizedGroupName = StringUtil.normalize(groupName);
        if (normalizedGroupName == null || priority < 0 || createdBy < CONSOLE_USER_ID)
            return null;

        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (group_name, priority, created_by)
                VALUES (?, ?, ?)
                """.formatted(TABLE_NAME);
        int groupId = (int) database.updateAndGetGeneratedKeys(insertSql, stmt -> {
            stmt.setString(1, normalizedGroupName);
            stmt.setInt(2, priority);
            stmt.setInt(3, createdBy);
        });
        if (groupId < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT created_at FROM %s WHERE id = ?".formatted(TABLE_NAME);
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> stmt.setInt(1, groupId));
        if (createdAt == null) return null;

        Group group = new Group(groupId, normalizedGroupName, priority, false, createdBy, createdAt, null, null);
        refreshProvider.fireSingle(single, groupId);
        return group;
    }

    @Override
    public int delete(int groupId) {
        if (groupId < 1) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);
        int row = database.update(sql,
                stmt -> stmt.setInt(1, groupId));
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, groupId);
        return row;
    }

    @Override
    public @Nullable Group update(int groupId, @NotNull String groupName, int priority, int changedBy) {
        String normalizedGroupName = StringUtil.normalize(groupName);
        if (normalizedGroupName == null || priority < 0 || changedBy < CONSOLE_USER_ID)
            return null;

        Group existing = cache.getById(groupId);
        if (existing == null) return null;

        if (Objects.equals(normalizedGroupName, existing.groupName()) &&
                priority == existing.priority())
            return existing; // No changes, return an existing group

        @Language("MariaDB")
        String updateSql = "UPDATE %s SET group_name = ?, priority = ?, changed_by = ? WHERE id = ?".formatted(TABLE_NAME);
        int row = database.update(updateSql, stmt -> {
            stmt.setString(1, normalizedGroupName);
            stmt.setInt(2, priority);
            stmt.setInt(3, changedBy);
            stmt.setInt(4, groupId);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT changed_at FROM %s WHERE id = ?".formatted(TABLE_NAME);
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> stmt.setInt(1, groupId));
        if (changedAt == null) return null;

        Group group = Group.builder(existing)
                .groupName(groupName)
                .priority(priority)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, groupId);
        return group;
    }
}
