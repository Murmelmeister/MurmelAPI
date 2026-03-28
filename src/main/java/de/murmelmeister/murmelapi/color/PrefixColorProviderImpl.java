package de.murmelmeister.murmelapi.color;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.color.PrefixColorException;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

final class PrefixColorProviderImpl implements PrefixColorProvider {
    private static final String TABLE_NAME = "prefix_colors";

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (id, color, animated, created_by)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                color = VALUES(color),
                animated = VALUES(animated),
                changed_by = ?
            RETURNING id, color, animated, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final PrefixColorCache cache;
    private final RefreshType all = RefreshType.PREFIX_COLORS;
    private final RefreshType single = RefreshType.SINGLE_PREFIX_COLOR;

    public PrefixColorProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new PrefixColorCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @NotNull Optional<PrefixColor> findById(@NotNull String id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull @Unmodifiable List<PrefixColor> findAll() {
        return cache.getAll();
    }

    @Override
    public @NotNull Optional<PrefixColor> upsert(@NotNull String id, @NotNull String color, boolean animated, int executorId) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(color, "color cannot be null");
        if (id.length() > 100) throw new IllegalArgumentException("id cannot be longer than 100 characters");
        if (id.isBlank()) throw new IllegalArgumentException("id cannot be blank");
        if (color.isBlank()) throw new IllegalArgumentException("color cannot be blank");
        if (executorId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("executorId must be >= " + CONSOLE_USER_ID);

        Optional<PrefixColor> optExisting = cache.getById(id);
        if (optExisting.isPresent())
            if (Objects.equals(color, optExisting.get().color())
                    && animated == optExisting.get().animated())
                return optExisting;

        PrefixColor saved = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert PrefixColor (id=" + id + ")",
                () -> database.query(UPSERT_SQL, null, PrefixColorAdapter::resultSet, stmt -> {
                    stmt.setString(1, id);
                    stmt.setString(2, color);
                    stmt.setBoolean(3, animated);
                    stmt.setInt(4, executorId);
                    stmt.setInt(5, executorId);
                }),
                PrefixColorException::new
        );

        if (saved == null) return Optional.empty();
        refreshProvider.fireSingle(single, saved);
        return Optional.of(saved);
    }

    @Override
    public @NotNull Optional<PrefixColor> upsert(@NotNull PrefixColor prefixColor, int executorId) {
        return upsert(prefixColor.id(), prefixColor.color(), prefixColor.animated(), executorId);
    }

    @Override
    public int delete(@NotNull String id) {
        Objects.requireNonNull(id, "id cannot be null");
        if (id.length() > 100) throw new IllegalArgumentException("id cannot be longer than 100 characters");
        if (id.isBlank()) throw new IllegalArgumentException("id cannot be blank");

        Optional<PrefixColor> existing = cache.getById(id);
        if (existing.isEmpty()) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete PrefixColor (id=" + id + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setString(1, id)),
                PrefixColorException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, existing.get());
        return row;
    }
}
