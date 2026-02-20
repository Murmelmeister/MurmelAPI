package de.murmelmeister.murmelapi.maintenance;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class MaintenanceProviderImpl implements MaintenanceProvider {
    private static final String TABLE_NAME = "maintenances";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final MaintenanceCache cache;
    private final RefreshType all = RefreshType.MAINTENANCES;
    private final RefreshType single = RefreshType.SINGLE_MAINTENANCE;

    public MaintenanceProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new MaintenanceCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable Maintenance findById(int id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull @Unmodifiable List<Maintenance> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable Maintenance create(@Nullable String title, @Nullable String reason, @NotNull MaintenanceType status, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, int createdBy) {
        @Language("MariaDB")
        String sql = """
                INSERT INTO %s (title, reason, status, start_at, end_at, created_by)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id, title, reason, status, start_at, end_at, created_at, created_by, changed_at, changed_by
                """.formatted(TABLE_NAME);
        Maintenance maintenance = database.query(sql, null, ResultSetUtil.maintenance(), stmt -> {
            stmt.setString(1, title);
            stmt.setString(2, reason);
            stmt.setString(3, status.name());
            stmt.setTimestamp(4, Timestamp.valueOf(startAt));
            stmt.setTimestamp(5, Timestamp.valueOf(endAt));
            stmt.setInt(6, createdBy);
        });

        if (maintenance == null) return null;
        refreshProvider.fireSingle(single, maintenance);
        return maintenance;
    }

    @Override
    public @Nullable Maintenance update(int id, @Nullable String title, @Nullable String reason, @NotNull MaintenanceType status, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, int changedBy) {
        Maintenance existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(title, existing.title()) &&
                Objects.equals(reason, existing.reason()) &&
                status == existing.status() &&
                Objects.equals(startAt, existing.startAt()) &&
                Objects.equals(endAt, existing.endAt()))
            return existing;

        @Language("MariaDB")
        String sql = """
                UPDATE %s
                SET title = ?,
                    reason = ?,
                    status = ?,
                    start_at = ?,
                    end_at = ?,
                    changed_by = ?
                WHERE id = ?
                RETURNING id, title, reason, status, start_at, end_at, created_at, created_by, changed_at, changed_by
                """.formatted(TABLE_NAME);
        Maintenance updated = database.query(sql, null, ResultSetUtil.maintenance(), stmt -> {
            stmt.setString(1, title);
            stmt.setString(2, reason);
            stmt.setString(3, status.name());
            stmt.setTimestamp(4, Timestamp.valueOf(startAt));
            stmt.setTimestamp(5, Timestamp.valueOf(endAt));
            stmt.setInt(6, changedBy);
            stmt.setInt(7, id);
        });

        if (updated == null) return null;
        refreshProvider.fireSingle(single, updated);
        return updated;
    }
}
