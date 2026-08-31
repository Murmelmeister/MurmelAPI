package de.murmelmeister.murmelapi.maintenance;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.maintenance.MaintenanceException;
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
import java.util.Optional;
import java.util.function.Consumer;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

final class MaintenanceProviderImpl implements MaintenanceProvider {
    private static final String TABLE_NAME = "maintenances";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (title, reason, start_at, end_at, created_by)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, title, reason, status, start_at, end_at, created_at, created_by, changed_at, changed_by
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s
            SET title = ?,
                reason = ?,
                status = ?,
                start_at = ?,
                end_at = ?,
                changed_by = ?
            WHERE id = ?
            """.formatted(TABLE_NAME);

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
    public @NotNull Optional<Maintenance> findById(int id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull @Unmodifiable List<Maintenance> findAll() {
        return cache.getAll();
    }

    @Override
    public @NotNull Optional<Maintenance> findActive() {
        return findAll().stream()
                .filter(maintenance ->
                        maintenance.isStarted()
                                && !maintenance.isEnded()
                                && maintenance.status() == MaintenanceType.ACTIVE
                )
                .findFirst();
    }

    @Override
    public @NotNull Optional<Maintenance> create(int createdBy, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, @Nullable String title, @Nullable String reason) {
        if (createdBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("createdBy must be greater than or equal to " + CONSOLE_USER_ID);
        Objects.requireNonNull(startAt, "startAt must not be null");
        Objects.requireNonNull(endAt, "endAt must not be null");
        if (!startAt.isBefore(endAt)) throw new IllegalArgumentException("startAt must be before endAt");

        Maintenance maintenance = MurmelExceptionWrapper.dbWrap(
                "Failed to create Maintenance",
                () -> database.query(CREATE_SQL, null, MaintenanceRowMapper::resultSet, stmt -> {
                    stmt.setString(1, title);
                    stmt.setString(2, reason);
                    stmt.setObject(3, startAt, Types.TIMESTAMP);
                    stmt.setObject(4, endAt, Types.TIMESTAMP);
                    stmt.setInt(5, createdBy);
                }),
                MaintenanceException::new
        );

        if (maintenance == null) return Optional.empty();
        refreshProvider.fireSingle(single, new MaintenanceCache.MaintenanceKey(maintenance.id()));
        return Optional.of(maintenance);
    }

    @Override
    public @NotNull Optional<Maintenance> update(int id, int changedBy, @NotNull Consumer<Maintenance.Builder> updater) {
        if (id < 1)
            throw new IllegalArgumentException("id must be greater than or equal to 1");
        if (changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be greater than or equal to " + CONSOLE_USER_ID);
        Objects.requireNonNull(updater, "updater must not be null");

        Optional<Maintenance> existingOpt = cache.getById(id);
        if (existingOpt.isEmpty()) return Optional.empty();
        Maintenance existing = existingOpt.get();

        Maintenance.Builder builder = existing.builder();
        updater.accept(builder);
        Maintenance candidate = builder.build();

        Objects.requireNonNull(candidate.startAt(), "startAt must not be null");
        Objects.requireNonNull(candidate.endAt(), "endAt must not be null");
        if (!candidate.startAt().isBefore(candidate.endAt()))
            throw new IllegalArgumentException("startAt must be before endAt");

        if (Objects.equals(candidate.title(), existing.title()) &&
                Objects.equals(candidate.reason(), existing.reason()) &&
                candidate.status() == existing.status() &&
                Objects.equals(candidate.startAt(), existing.startAt()) &&
                Objects.equals(candidate.endAt(), existing.endAt()))
            return existingOpt;


        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update Maintenance (id=" + id + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setString(1, candidate.title());
                    stmt.setString(2, candidate.reason());
                    stmt.setString(3, candidate.status().name());
                    stmt.setObject(4, candidate.startAt(), Types.TIMESTAMP);
                    stmt.setObject(5, candidate.endAt(), Types.TIMESTAMP);
                    stmt.setInt(6, changedBy);
                    stmt.setInt(7, id);
                }),
                MaintenanceException::new
        );
        if (row != 1) return Optional.empty();

        refreshProvider.fireSingle(single, new MaintenanceCache.MaintenanceKey(id));
        return cache.getById(id);
    }
}
