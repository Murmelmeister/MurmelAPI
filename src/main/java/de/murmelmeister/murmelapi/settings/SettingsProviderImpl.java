package de.murmelmeister.murmelapi.settings;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class SettingsProviderImpl implements SettingsProvider {
    private static final String TABLE_NAME = "settings";

    private final Database database;
    private final SettingsCache cache;
    private final RefreshType all = RefreshType.SETTINGS;
    private final RefreshType single = RefreshType.SINGLE_SETTING;

    public SettingsProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new SettingsCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public Settings findById(String tag) {
        return cache.get(tag);
    }

    @Override
    public List<Settings> findAll() {
        return cache.getCachedSettings();
    }

    @Override
    public Settings create(String tagId, String json) {
        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || json == null) return null;

        String insertSql = "INSERT INTO " + TABLE_NAME + " (tag_id, value_json) VALUES (?, ?)";
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, normalizedTagId);
            stmt.setString(2, json);
        });
        if (row < 1) return null;

        String selectSql = "SELECT updated_at FROM " + TABLE_NAME + " WHERE tag_id = ?";
        LocalDateTime updatedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("updated_at").toLocalDateTime(),
                stmt -> stmt.setString(1, normalizedTagId));
        if (updatedAt == null) return null;

        Settings settings = new Settings(normalizedTagId, json, updatedAt);
        RefreshUtil.fireSingle(single, normalizedTagId);
        return settings;
    }

    @Override
    public int delete(String tagId) {
        if (tagId == null) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE tag_id = ?";
        int row = database.update(sql, stmt -> stmt.setString(1, tagId));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, tagId);
        return row;
    }

    @Override
    public Settings update(String tagId, String json) {
        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || json == null) return null;

        Settings existing = cache.get(normalizedTagId);
        if (existing == null) return null;

        if (Objects.equals(json, existing.json()))
            return existing;

        String updateSql = "UPDATE " + TABLE_NAME + " SET value_json = ? WHERE tag_id = ?";
        int row = database.update(updateSql, stmt -> {
            stmt.setString(1, json);
            stmt.setString(2, normalizedTagId);
        });
        if (row < 1) return null;

        String selectSql = "SELECT updated_at FROM " + TABLE_NAME + " WHERE tag_id = ?";
        LocalDateTime updatedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("updated_at").toLocalDateTime(),
                stmt -> stmt.setString(1, normalizedTagId));
        if (updatedAt == null) return null;

        Settings settings = existing.withUpdateMeta(json, updatedAt);
        RefreshUtil.fireSingle(single, normalizedTagId);
        return settings;
    }

    @Override
    public Settings upsert(Settings settings) {
        if (settings == null) return null;

        Settings existing = cache.get(settings.tagId());
        if (existing != null && Objects.equals(settings.json(), existing.json()))
            return existing;

        String sql = "INSERT INTO " + TABLE_NAME + " (tag_id, value_json) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE value_json = VALUES(value_json) " +
                "RETURNING tag_id, value_json, updated_at";
        Settings saved = database.query(sql, null, ResultSetUtil.settings(), stmt -> {
            stmt.setString(1, settings.tagId());
            stmt.setString(2, settings.json());
        });

        if (saved == null) return null;
        RefreshUtil.fireSingle(single, saved.tagId());
        return saved;
    }
}
