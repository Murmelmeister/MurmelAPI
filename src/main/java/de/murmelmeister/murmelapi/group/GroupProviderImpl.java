package de.murmelmeister.murmelapi.group;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.group.GroupException;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

/**
 * GroupProvider is a class that provides methods to manage groups in the database.
 * It implements the Group interface and uses the Database class to interact with the database.
 */
final class GroupProviderImpl implements GroupProvider {
    private static final String TABLE_NAME = "groups";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (group_name, priority, created_by)
            VALUES (?, ?, ?)
            RETURNING id, group_name, priority, is_default, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s
            SET group_name = ?,
                priority = ?,
                changed_by = ?
            WHERE id = ?
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SELECT_SQL = "SELECT changed_at FROM %s WHERE id = ?".formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final GroupCache cache;
    private final RefreshType all = RefreshType.GROUPS;
    private final RefreshType single = RefreshType.SINGLE_GROUP;

    public GroupProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new GroupCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @NotNull Optional<Group> findById(int id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull Optional<Group> findByName(@NotNull String groupName) {
        return cache.getByName(groupName);
    }

    @Override
    public @NotNull @Unmodifiable List<Group> findAll() {
        return cache.getAll();
    }

    @Override
    public @NotNull @Unmodifiable List<String> findAllGroupNames() {
        return findAll().stream().map(Group::groupName).collect(Collectors.toList());
    }

    @Override
    public @NotNull Optional<Group> create(@NotNull String groupName, int priority, int createdBy) {
        Objects.requireNonNull(groupName, "groupName cannot be null");
        String normalizedGroupName = StringUtil.normalize(groupName);
        if (normalizedGroupName == null || normalizedGroupName.isBlank())
            throw new IllegalArgumentException("groupName cannot be blank");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);

        Group group = MurmelExceptionWrapper.dbWrap(
                "Failed to create Group (groupName=" + normalizedGroupName + ")",
                () -> database.query(CREATE_SQL, null, GroupAdapter::resultSet, stmt -> {
                    stmt.setString(1, normalizedGroupName);
                    stmt.setInt(2, priority);
                    stmt.setInt(3, createdBy);
                }),
                GroupException::new
        );

        if (group == null) return Optional.empty();
        refreshProvider.fireSingle(single, group);
        return Optional.of(group);
    }

    @Override
    public int delete(int groupId) {
        if (groupId < 1) throw new IllegalArgumentException("groupId must be >= 1");

        Optional<Group> existing = cache.getById(groupId);
        if (existing.isEmpty()) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete Group (groupId=" + groupId + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setInt(1, groupId)),
                GroupException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, existing.get());
        return row;
    }

    @Override
    public @NotNull Optional<Group> update(int groupId, @NotNull String groupName, int priority, int changedBy) {
        Objects.requireNonNull(groupName, "groupName cannot be null");
        String normalizedGroupName = StringUtil.normalize(groupName);
        if (normalizedGroupName == null || normalizedGroupName.isBlank())
            throw new IllegalArgumentException("groupName cannot be blank");
        if (changedBy < CONSOLE_USER_ID) throw new IllegalArgumentException("changedBy must be >= " + CONSOLE_USER_ID);

        Optional<Group> existingOpt = cache.getById(groupId);
        if (existingOpt.isEmpty()) return Optional.empty();
        Group existing = existingOpt.get();

        if (Objects.equals(normalizedGroupName, existing.groupName())
                && priority == existing.priority())
            return existingOpt;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update Group (groupId=" + groupId + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setString(1, normalizedGroupName);
                    stmt.setInt(2, priority);
                    stmt.setInt(3, changedBy);
                    stmt.setInt(4, groupId);
                }),
                GroupException::new
        );
        if (row != 1) return Optional.empty();

        LocalDateTime changedAt = MurmelExceptionWrapper.dbWrap(
                "Failed to select changed_at for Group (groupId=" + groupId + ")",
                () -> database.query(UPDATE_SELECT_SQL, null,
                        resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                        stmt -> stmt.setInt(1, groupId)),
                GroupException::new
        );
        if (changedAt == null) return Optional.empty();

        Group group = existing.builder()
                .groupName(groupName)
                .priority(priority)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, group);
        return Optional.of(group);
    }
}
