package de.murmelmeister.murmelapi.language;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.util.List;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.ENGLISH_CODE;
import static de.murmelmeister.murmelapi.MurmelAPI.GERMAN_CODE;

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
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY AUTO_INCREMENT, code VARCHAR(32) UNIQUE");
    }

    public static void createDefaultLanguages(Database database) {
        // Use an upsert that does not delete the row, otherwise ON DELETE CASCADE wipes messages.
        String upsertDefaults = "INSERT INTO " + TABLE_NAME + " (id, code) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE code = VALUES(code)";
        database.updateBatch(upsertDefaults, stmt -> {
            stmt.setInt(1, 1);
            stmt.setString(2, ENGLISH_CODE);
            stmt.addBatch();

            stmt.setInt(1, 2);
            stmt.setString(2, GERMAN_CODE);
            stmt.addBatch();
        });

        /*String insertOthers = "INSERT IGNORE INTO " + TABLE_NAME + " (code) VALUES (?)";
        Set<String> defaultTags = Set.of(ENGLISH_CODE, GERMAN_CODE);

        database.updateBatch(insertOthers, stmt -> {
            Set<String> locales = new LinkedHashSet<>();
            for (Locale locale : Locale.getAvailableLocales()) {
                String tag = locale.toLanguageTag();
                if (tag.isEmpty() || defaultTags.contains(tag)) continue;
                locales.add(tag);
            }

            for (String tag : locales) {
                stmt.setString(1, tag);
                stmt.addBatch();
            }
        });*/
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
    public Language get(String code) {
        String normalized = normalize(code);
        return normalized != null ? cache.getByCode(normalized) : null;
    }

    @Override
    public List<Language> getLanguages() {
        return cache.getCachedLanguages();
    }

    @Override
    public Language create(String code) {
        String normalized = normalize(code);
        if (normalized == null) return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (code) VALUES (?)";
        int id = (int) database.updateAndGetGeneratedKeys(sql,
                stmt -> stmt.setString(1, normalized));
        if (id < 1) return null;

        Language language = new Language(id, normalized);
        RefreshUtil.fireSingle(single, id);
        return language;
    }

    @Override
    public int delete(int id) {
        if (id < 1 || id == 1 || id == 2) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql,
                stmt -> stmt.setInt(1, id));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, id);
        return row;
    }

    @Override
    public Language update(int id, String code) {
        if (id < 1 || code == null) return null;
        String normalized = normalize(code);
        if (normalized == null) return null;
        if ((id == 1 && !ENGLISH_CODE.equals(normalized))
                || (id == 2 && !GERMAN_CODE.equals(normalized))) {
            return null;
        }

        Language existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(normalized, existing.code()))
            return existing; // No changes, return existing

        String sql = "UPDATE " + TABLE_NAME + " SET code = ? WHERE id = ?";
        int rows = database.update(sql, stmt -> {
            stmt.setString(1, normalized);
            stmt.setInt(2, id);
        });
        if (rows < 1) return null;

        Language language = existing.withCode(normalized);
        RefreshUtil.fireSingle(single, id);
        return language;
    }

    private String normalize(String code) {
        if (code == null) return null;
        if (code.isBlank()) return null;
        return code;
    }
}
