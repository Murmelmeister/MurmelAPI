package de.murmelmeister.murmelapi.settings;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.setting.SettingsException;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class SettingsProviderImpl implements SettingsProvider {
    private static final String TABLE_NAME = "settings";

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (tag_id, value_json)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE value_json = VALUES(value_json)
            RETURNING tag_id, value_json, updated_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE tag_id = ?".formatted(TABLE_NAME);


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
    public @NotNull Optional<Settings> findById(@NotNull String tag) {
        return cache.get(tag);
    }

    @Override
    public @NotNull @Unmodifiable List<Settings> findAll() {
        return cache.getAll();
    }

    @Override
    public @NotNull Optional<Settings> upsert(@NotNull String tagId, @NotNull String json) {
        Objects.requireNonNull(tagId, "tagId cannot be null");
        Objects.requireNonNull(json, "json cannot be null");
        if (json.isBlank()) throw new IllegalArgumentException("json cannot be blank");
        if (tagId.length() > 100) throw new IllegalArgumentException("tagId cannot be longer than 100 characters");

        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || normalizedTagId.isBlank())
            throw new IllegalArgumentException("tagId cannot be blank");

        Optional<Settings> existingOpt = cache.get(tagId);
        if (existingOpt.isPresent() && Objects.equals(json, existingOpt.get().json()))
            return existingOpt;

        Settings saved = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert Settings (tagId=" + tagId + ")",
                () -> database.query(UPSERT_SQL, null, SettingsRowMapper::resultSet, stmt -> {
                    stmt.setString(1, tagId);
                    stmt.setString(2, json);
                }),
                SettingsException::new
        );

        if (saved == null) return Optional.empty();
        refreshProvider.fireSingle(single, new SettingsCache.TagKey(saved.tagId()));
        return Optional.of(saved);
    }

    @Override
    public int delete(@NotNull String tagId) {
        Objects.requireNonNull(tagId, "tagId cannot be null");
        if (tagId.isBlank()) throw new IllegalArgumentException("tagId cannot be blank");
        if (tagId.length() > 100) throw new IllegalArgumentException("tagId cannot be longer than 100 characters");

        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || normalizedTagId.isBlank())
            throw new IllegalArgumentException("tagId cannot be blank");

        Optional<Settings> existingOpt = cache.get(tagId);
        if (existingOpt.isEmpty()) return 0;
        Settings existing = existingOpt.get();

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete Settings (tagId=" + tagId + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setString(1, tagId)),
                SettingsException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new SettingsCache.TagKey(existing.tagId()));
        return row;
    }
}
