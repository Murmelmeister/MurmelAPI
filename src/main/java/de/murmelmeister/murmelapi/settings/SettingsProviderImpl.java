package de.murmelmeister.murmelapi.settings;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.setting.SettingsException;
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

public final class SettingsProviderImpl implements SettingsProvider {
    private static final String TABLE_NAME = "settings";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (tag_id, value_json)
            VALUES (?, ?)
            RETURNING tag_id, value_json, updated_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE tag_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s
            SET value_json = ?
            WHERE tag_id = ?
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SELECT_SQL = "SELECT updated_at FROM %s WHERE tag_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (tag_id, value_json)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE value_json = VALUES(value_json)
            RETURNING tag_id, value_json, updated_at
            """.formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final SettingsCache cache;
    private final RefreshType all = RefreshType.SETTINGS;
    private final RefreshType single = RefreshType.SINGLE_SETTING;

    public SettingsProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new SettingsCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable Settings findById(@Nullable String tag) {
        return cache.get(tag);
    }

    @Override
    public @NotNull @Unmodifiable List<Settings> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable Settings create(@NotNull String tagId, @NotNull String json) {
        Objects.requireNonNull(tagId, "tagId cannot be null");
        Objects.requireNonNull(json, "json cannot be null");
        if (json.isBlank()) throw new IllegalArgumentException("json cannot be blank");
        if (tagId.length() > 100) throw new IllegalArgumentException("tagId cannot be longer than 100 characters");

        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || normalizedTagId.isBlank())
            throw new IllegalArgumentException("tagId cannot be blank");

        Settings settings = MurmelExceptionWrapper.dbWrap(
                "Failed to create Settings (tagId=" + tagId + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.settings(), stmt -> {
                    stmt.setString(1, normalizedTagId);
                    stmt.setString(2, json);
                }),
                SettingsException::new
        );

        if (settings == null) return null;
        refreshProvider.fireSingle(single, settings);
        return settings;
    }

    @Override
    public int delete(@NotNull String tagId) {
        Objects.requireNonNull(tagId, "tagId cannot be null");
        if (tagId.isBlank()) throw new IllegalArgumentException("tagId cannot be blank");
        if (tagId.length() > 100) throw new IllegalArgumentException("tagId cannot be longer than 100 characters");

        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || normalizedTagId.isBlank())
            throw new IllegalArgumentException("tagId cannot be blank");

        Settings existing = cache.get(tagId);
        if (existing == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete Settings (tagId=" + tagId + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setString(1, tagId)),
                SettingsException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, existing);
        return row;
    }

    @Override
    public @Nullable Settings update(@NotNull String tagId, @NotNull String json) {
        Objects.requireNonNull(tagId, "tagId cannot be null");
        Objects.requireNonNull(json, "json cannot be null");
        if (json.isBlank()) throw new IllegalArgumentException("json cannot be blank");
        if (tagId.length() > 100) throw new IllegalArgumentException("tagId cannot be longer than 100 characters");

        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || normalizedTagId.isBlank())
            throw new IllegalArgumentException("tagId cannot be blank");

        Settings existing = cache.get(normalizedTagId);
        if (existing == null) return null;

        if (Objects.equals(json, existing.json()))
            return existing;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update Settings (tagId=" + tagId + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setString(1, json);
                    stmt.setString(2, normalizedTagId);
                }),
                SettingsException::new
        );
        if (row != 1) return null;

        LocalDateTime updatedAt = MurmelExceptionWrapper.dbWrap(
                "Failed to select updated_at for Settings (tagId=" + tagId + ")",
                () -> database.query(UPDATE_SELECT_SQL, null,
                        resultSet -> resultSet.getTimestamp("updated_at").toLocalDateTime(),
                        stmt -> stmt.setString(1, normalizedTagId)),
                SettingsException::new
        );
        if (updatedAt == null) return null;

        Settings settings = Settings.builder(existing)
                .json(json)
                .updatedAt(updatedAt)
                .build();
        refreshProvider.fireSingle(single, settings);
        return settings;
    }

    @Override
    public @Nullable Settings upsert(@NotNull String tagId, @NotNull String json) {
        Objects.requireNonNull(tagId, "tagId cannot be null");
        Objects.requireNonNull(json, "json cannot be null");
        if (json.isBlank()) throw new IllegalArgumentException("json cannot be blank");
        if (tagId.length() > 100) throw new IllegalArgumentException("tagId cannot be longer than 100 characters");

        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || normalizedTagId.isBlank())
            throw new IllegalArgumentException("tagId cannot be blank");

        Settings existing = cache.get(tagId);
        if (existing != null && Objects.equals(json, existing.json()))
            return existing;

        Settings saved = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert Settings (tagId=" + tagId + ")",
                () -> database.query(UPSERT_SQL, null, ResultSetUtil.settings(), stmt -> {
                    stmt.setString(1, tagId);
                    stmt.setString(2, json);
                }),
                SettingsException::new
        );

        if (saved == null) return null;
        refreshProvider.fireSingle(single, saved);
        return saved;
    }
}
