package de.murmelmeister.murmelapi.settings;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class SettingsProviderImpl implements SettingsProvider {
    private static final String TABLE_NAME = "settings";

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
    public @NotNull List<Settings> findAll() {
        return cache.getCachedSettings();
    }

    @Override
    public @Nullable Settings create(@NotNull String tagId, @NotNull String json) {
        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null) return null;

        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (tag_id, value_json)
                VALUES (?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, normalizedTagId);
            stmt.setString(2, json);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT updated_at FROM %s WHERE tag_id = ?".formatted(TABLE_NAME);
        LocalDateTime updatedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("updated_at").toLocalDateTime(),
                stmt -> stmt.setString(1, normalizedTagId));
        if (updatedAt == null) return null;

        Settings settings = new Settings(normalizedTagId, json, updatedAt);
        refreshProvider.fireSingle(single, settings);
        return settings;
    }

    @Override
    public int delete(@NotNull String tagId) {
        Settings existing = cache.get(tagId);
        if (existing == null) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE tag_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> stmt.setString(1, tagId));
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, existing);
        return row;
    }

    @Override
    public @Nullable Settings update(@NotNull String tagId, @NotNull String json) {
        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null) return null;

        Settings existing = cache.get(normalizedTagId);
        if (existing == null) return null;

        if (Objects.equals(json, existing.json()))
            return existing;

        @Language("MariaDB")
        String updateSql = "UPDATE %s SET value_json = ? WHERE tag_id = ?".formatted(TABLE_NAME);
        int row = database.update(updateSql, stmt -> {
            stmt.setString(1, json);
            stmt.setString(2, normalizedTagId);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT updated_at FROM %s WHERE tag_id = ?".formatted(TABLE_NAME);
        LocalDateTime updatedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("updated_at").toLocalDateTime(),
                stmt -> stmt.setString(1, normalizedTagId));
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
        Settings existing = cache.get(tagId);
        if (existing != null && Objects.equals(json, existing.json()))
            return existing;

        @Language("MariaDB")
        String sql = """
                INSERT INTO %s (tag_id, value_json) VALUES (?, ?)
                ON DUPLICATE KEY UPDATE value_json = VALUES(value_json)
                RETURNING tag_id, value_json, updated_at
                """.formatted(TABLE_NAME);
        Settings saved = database.query(sql, null, ResultSetUtil.settings(), stmt -> {
            stmt.setString(1, tagId);
            stmt.setString(2, json);
        });

        if (saved == null) return null;
        refreshProvider.fireSingle(single, saved);
        return saved;
    }
}
