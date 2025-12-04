package de.murmelmeister.murmelapi.settings;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
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

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "tag_id VARCHAR(100) NOT NULL PRIMARY KEY, " +
                "value_json JSON NOT NULL, " +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
    }

    @Override
    public void closeCache() {
        cache.close();
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public Settings get(String tag) {
        return cache.get(tag);
    }

    @Override
    public List<Settings> getAll() {
        return cache.getCachedSettings();
    }

    @Override
    public Settings upsert(Settings settings) {
        if (settings == null) return null;

        Settings existing = cache.get(settings.tagId());
        if (existing != null && Objects.equals(settings.json(), existing.json()))
            return existing;

        String sql = "INSERT INTO " + TABLE_NAME + " (tag_id, value_json) VALUES (?, ?) ON DUPLICATE KEY UPDATE value_json = VALUES(value_json)";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, settings.tagId());
            stmt.setString(2, settings.json());
        });
        if (row < 1) return null;

        RefreshUtil.fireSingle(single, settings.tagId());
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
}
