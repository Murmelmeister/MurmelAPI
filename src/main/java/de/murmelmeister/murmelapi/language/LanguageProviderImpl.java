package de.murmelmeister.murmelapi.language;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.util.List;
import java.util.Objects;

/**
 * Provides CRUD operations for {@link Language} entities backed by a relational database
 * and an in-memory cache.
 * <p>
 * This class manages all SQL interactions for the "languages" table, maintains
 * a {@link LanguageCache} for fast lookups, and exposes methods to create, read,
 * update, and delete language records. It also offers a convenience method to
 * initialize the table schema and seed default languages.
 * </p>
 */
public final class LanguageProviderImpl implements LanguageProvider {
    private static final String TABLE_NAME = "languages";

    private final Database database;
    private final LanguageCache cache;
    private final RefreshType all = RefreshType.LANGUAGES;
    private final RefreshType single = RefreshType.SINGLE_LANGUAGE;

    public LanguageProviderImpl(Database database, long cacheCapacity) {
        this.database = database;
        this.cache = new LanguageCache(database, TABLE_NAME, cacheCapacity);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) UNIQUE");
    }

    public static void createDefaultLanguages(Database database) {
        database.update("INSERT IGNORE INTO " + TABLE_NAME + " (id, name) VALUES (1, 'English'), (2, 'German')");
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
    public Language get(int id) {
        return cache.getById(id);
    }

    @Override
    public Language get(String name) {
        return cache.getByName(name);
    }

    @Override
    public List<Language> getLanguages() {
        return cache.getCachedLanguages();
    }

    @Override
    public Language create(String name) {
        if (name == null) return null;

        name = name.strip();
        if (name.isEmpty()) return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (name) VALUES (?)";
        int id = database.updateAndGetAutoIncrement(sql, name);
        if (id < 1) return null;

        Language language = new Language(id, name);
        RefreshUtil.fireSingle(sql, id);
        cache.put(language);
        return language;
    }

    @Override
    public int delete(int id) {
        if (id < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql, id);
        if (row < 1) return 0;

        cache.remove(id);
        RefreshUtil.fireSingle(single, id);
        return row;
    }

    @Override
    public Language update(int id, String name) {
        if (id < 1 || name == null) return null;

        name = name.strip();
        if (name.isEmpty()) return null;

        Language existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(name, existing.name()))
            return existing; // No changes, return existing

        String sql = "UPDATE " + TABLE_NAME + " SET name = ? WHERE id = ?";
        int rows = database.update(sql, name, id);
        if (rows < 1) return null;

        Language language = existing.withName(name);
        RefreshUtil.fireSingle(single, id);
        cache.put(language);
        return language;
    }
}
