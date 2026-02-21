package de.murmelmeister.murmelapi.maintenance.whitelist;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.maintenance.MaintenanceException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class MaintenanceWhitelistProviderImpl implements MaintenanceWhitelistProvider {
    private static final String TABLE_NAME = "maintenance_whitelist";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (maintenance_id, user_id, start_at, end_at, note, created_by)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id, maintenance_id, user_id, start_at, end_at, note, created_at, created_by, changed_at, changed_by
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s
            SET start_at = ?,
                end_at = ?,
                note = ?,
                changed_by = ?
            WHERE id = ?
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SELECT_SQL = "SELECT changed_at FROM %s WHERE id = ?".formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final MaintenanceWhitelistCache cache;
    private final RefreshType all = RefreshType.MAINTENANCE_WHITELISTS;
    private final RefreshType single = RefreshType.SINGLE_MAINTENANCE_WHITELIST;

    public MaintenanceWhitelistProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new MaintenanceWhitelistCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable MaintenanceWhitelist findById(int id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull @Unmodifiable List<MaintenanceWhitelist> findByMaintenanceId(int maintenanceId) {
        return cache.getByMaintenanceId(maintenanceId);
    }

    @Override
    public @NotNull @Unmodifiable List<MaintenanceWhitelist> findByUserId(int userId) {
        return cache.getByUserId(userId);
    }

    @Override
    public @NotNull @Unmodifiable List<MaintenanceWhitelist> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable MaintenanceWhitelist create(int maintenanceId, int userId, @Nullable LocalDateTime startAt, @Nullable LocalDateTime endAt, @Nullable String note, int createdBy) {
        MaintenanceWhitelist whitelist = MurmelExceptionWrapper.dbWrap(
                "Failed to create MaintenanceWhitelist",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.maintenanceWhitelist(), stmt -> {
                    stmt.setInt(1, maintenanceId);
                    stmt.setInt(2, userId);
                    stmt.setObject(3, startAt, Types.TIMESTAMP);
                    stmt.setObject(4, endAt, Types.TIMESTAMP);
                    stmt.setString(5, note);
                    stmt.setInt(6, createdBy);
                }),
                MaintenanceException::new
        );

        if (whitelist == null) return null;
        refreshProvider.fireSingle(single, whitelist);
        return whitelist;
    }

    @Override
    public int delete(int id) {
        MaintenanceWhitelist whitelist = cache.getById(id);
        if (whitelist == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete MaintenanceWhitelist (id=" + id + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setInt(1, id)),
                MaintenanceException::new
        );

        if (row < 1) return 0;
        refreshProvider.fireSingle(single, whitelist);
        return row;
    }

    @Override
    public @Nullable MaintenanceWhitelist update(int id, @Nullable LocalDateTime startAt, @Nullable LocalDateTime endAt, @Nullable String note, int changedBy) {
        MaintenanceWhitelist existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(startAt, existing.startAt()) &&
                Objects.equals(endAt, existing.endAt()) &&
                Objects.equals(note, existing.note()))
            return existing;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update MaintenanceWhitelist (id=" + id + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setObject(1, startAt, Types.TIMESTAMP);
                    stmt.setObject(2, endAt, Types.TIMESTAMP);
                    stmt.setString(3, note);
                    stmt.setInt(4, changedBy);
                    stmt.setInt(5, id);
                }),
                MaintenanceException::new
        );
        if (row != 1) return null;

        LocalDateTime changedAt = MurmelExceptionWrapper.dbWrap(
                "Failed to get changedAt for MaintenanceWhitelist (id=" + id + ")",
                () -> database.query(UPDATE_SELECT_SQL, null,
                        resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                        stmt -> stmt.setInt(1, id)),
                MaintenanceException::new
        );
        if (changedAt == null) return null;

        MaintenanceWhitelist updated = MaintenanceWhitelist.builder(existing)
                .startAt(startAt)
                .endAt(endAt)
                .note(note)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, updated);
        return updated;
    }
}
