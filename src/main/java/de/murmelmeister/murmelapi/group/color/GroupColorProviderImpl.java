package de.murmelmeister.murmelapi.group.color;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.group.GroupColorException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

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

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (group_id, type_id, value, created_by)
            VALUES (?, ?, ?, ?)
            RETURNING group_id, type_id, value, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String REMOVE_SQL = "DELETE FROM %s WHERE group_id = ? AND type_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String CLEAR_SQL = "DELETE FROM %s WHERE group_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s
            SET value = ?,
                changed_by = ?
            WHERE group_id = ? AND type_id = ?
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SELECT_SQL = "SELECT changed_at FROM %s WHERE group_id = ? AND type_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (group_id, type_id, value, created_by)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                value = VALUES(value),
                changed_by = ?
            RETURNING group_id, type_id, value, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final GroupColorCache cache;
    private final RefreshType all = RefreshType.GROUP_COLORS;
    private final RefreshType single = RefreshType.SINGLE_GROUP_COLOR;

    public GroupColorProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new GroupColorCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable GroupColor findGroupColor(int groupId, int typeId) {
        return cache.get(groupId, typeId);
    }

    @Override
    public @NotNull @Unmodifiable List<GroupColor> findGroupColors(int groupId) {
        return cache.getByGroupId(groupId);
    }

    @Override
    public @NotNull @Unmodifiable List<GroupColor> findGroupColors() {
        return cache.getAll();
    }

    @Override
    public @Nullable GroupColor add(int groupId, int typeId, @NotNull String value, int createdBy) {
        Objects.requireNonNull(value, "value cannot be null");
        if (groupId < 1) throw new IllegalArgumentException("groupId must be >= 1");
        if (typeId < 1) throw new IllegalArgumentException("typeId must be >= 1");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);

        GroupColor groupColor = MurmelExceptionWrapper.dbWrap(
                "Failed to create GroupColor (groupId=" + groupId + ", typeId=" + typeId + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.groupColor(), stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setInt(2, typeId);
                    stmt.setString(3, value);
                    stmt.setInt(4, createdBy);
                }),
                GroupColorException::new
        );

        if (groupColor == null) return null;
        refreshProvider.fireSingle(single, new GroupColorCache.ColorKey(groupId, typeId));
        return groupColor;
    }

    @Override
    public int remove(int groupId, int typeId) {
        if (groupId < 1) throw new IllegalArgumentException("groupId must be >= 1");
        if (typeId < 1) throw new IllegalArgumentException("typeId must be >= 1");

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to remove GroupColor (groupId=" + groupId + ", typeId=" + typeId + ")",
                () -> database.update(REMOVE_SQL, stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setInt(2, typeId);
                }),
                GroupColorException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new GroupColorCache.ColorKey(groupId, typeId));
        return row;
    }

    @Override
    public int clear(int groupId) {
        if (groupId < 1) throw new IllegalArgumentException("groupId must be >= 1");

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to clear GroupColors (groupId=" + groupId + ")",
                () -> database.update(CLEAR_SQL,
                        stmt -> stmt.setInt(1, groupId)),
                GroupColorException::new
        );

        if (row < 1) return 0;
        refreshProvider.fireSingle(single, new GroupColorCache.ColorKey(groupId, null));
        return row;
    }

    @Override
    public @Nullable GroupColor update(int groupId, int typeId, @NotNull String value, int changedBy) {
        Objects.requireNonNull(value, "value cannot be null");
        if (groupId < 1) throw new IllegalArgumentException("groupId must be >= 1");
        if (typeId < 1) throw new IllegalArgumentException("typeId must be >= 1");
        if (changedBy < CONSOLE_USER_ID) throw new IllegalArgumentException("changedBy must be >= " + CONSOLE_USER_ID);

        GroupColor existing = cache.get(groupId, typeId);
        if (existing == null) return null;

        if (Objects.equals(value, existing.value()))
            return existing;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update GroupColor (groupId=" + groupId + ", typeId=" + typeId + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setString(1, value);
                    stmt.setInt(2, changedBy);
                    stmt.setInt(3, groupId);
                    stmt.setInt(4, typeId);
                }),
                GroupColorException::new
        );
        if (row != 1) return null;

        LocalDateTime changedAt = MurmelExceptionWrapper.dbWrap(
                "Failed to retrieve changed_at for GroupColor (groupId=" + groupId + ", typeId=" + typeId + ")",
                () -> database.query(UPDATE_SELECT_SQL, null,
                        resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                        stmt -> {
                            stmt.setInt(1, groupId);
                            stmt.setInt(2, typeId);
                        }),
                GroupColorException::new
        );
        if (changedAt == null) return null;

        GroupColor groupColor = GroupColor.builder(existing)
                .value(value)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, new GroupColorCache.ColorKey(groupId, typeId));
        return groupColor;
    }

    @Override
    public @Nullable GroupColor upsert(int groupId, int typeId, @NotNull String value, int executorId) {
        Objects.requireNonNull(value, "value cannot be null");
        if (groupId < 1) throw new IllegalArgumentException("groupId must be >= 1");
        if (typeId < 1) throw new IllegalArgumentException("typeId must be >= 1");
        if (executorId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("executorId must be >= " + CONSOLE_USER_ID);

        GroupColor existing = cache.get(groupId, typeId);
        if (existing != null && Objects.equals(value, existing.value()))
            return existing;

        GroupColor saved = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert GroupColor (groupId=" + groupId + ", typeId=" + typeId + ")",
                () -> database.query(UPSERT_SQL, null, ResultSetUtil.groupColor(), stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setInt(2, typeId);
                    stmt.setString(3, value);
                    stmt.setInt(4, executorId);
                    stmt.setInt(5, executorId);
                }),
                GroupColorException::new
        );

        if (saved == null) return null;
        refreshProvider.fireSingle(single, new GroupColorCache.ColorKey(groupId, typeId));
        return saved;
    }

    @Override
    public @Nullable GroupColor upsert(@NotNull GroupColor groupColor, int executorId) {
        return upsert(groupColor.groupId(), groupColor.typeId(), groupColor.value(), executorId);
    }
}
