package de.murmelmeister.murmelapi.language;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

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
        String upsertDefaults = "REPLACE INTO " + TABLE_NAME + " (id, code) VALUES (?, ?)";
        database.updateBatch(upsertDefaults, stmt -> {
            stmt.setInt(1, 1);
            stmt.setString(2, Locale.ENGLISH.toLanguageTag());
            stmt.addBatch();

            stmt.setInt(1, 2);
            stmt.setString(2, Locale.GERMAN.toLanguageTag());
            stmt.addBatch();
        });

        String insertOthers = "INSERT IGNORE INTO " + TABLE_NAME + " (code) VALUES (?)";
        Set<String> defaultTags = Set.of(
                Locale.ENGLISH.toLanguageTag(),
                Locale.GERMAN.toLanguageTag()
        );

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
        });
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
    public Language get(Locale locale) {
        Locale normalized = normalize(locale);
        return normalized != null ? cache.getByLocale(normalized) : null;
    }

    @Override
    public List<Language> getLanguages() {
        return cache.getCachedLanguages();
    }

    @Override
    public Language create(Locale locale) {
        Locale normalized = normalize(locale);
        if (normalized == null) return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (code) VALUES (?)";
        String localeTag = normalized.toLanguageTag();
        int id = (int) database.updateAndGetGeneratedKeys(sql,
                stmt -> stmt.setString(1, localeTag));
        if (id < 1) return null;

        Language language = new Language(id, normalized);
        RefreshUtil.fireSingle(single, id);
        cache.put(language);
        return language;
    }

    @Override
    public int delete(int id) {
        if (id < 1 || id == 1 || id == 2) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql,
                stmt -> stmt.setInt(1, id));
        if (row < 1) return 0;

        cache.remove(id);
        RefreshUtil.fireSingle(single, id);
        return row;
    }

    @Override
    public Language update(int id, Locale locale) {
        if (id < 1 || locale == null) return null;
        Locale normalized = normalize(locale);
        if (normalized == null) return null;
        if ((id == 1 && !Locale.ENGLISH.equals(normalized))
                || (id == 2 && !Locale.GERMAN.equals(normalized))) {
            return null;
        }

        Language existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(normalized, existing.locale()))
            return existing; // No changes, return existing

        String sql = "UPDATE " + TABLE_NAME + " SET code = ? WHERE id = ?";
        String localeTag = normalized.toLanguageTag();
        int rows = database.update(sql, stmt -> {
            stmt.setString(1, localeTag);
            stmt.setInt(2, id);
        });
        if (rows < 1) return null;

        Language language = existing.withLocale(normalized);
        RefreshUtil.fireSingle(single, id);
        cache.put(language);
        return language;
    }

    private static Locale normalize(Locale locale) {
        if (locale == null) return null;
        String tag = locale.toLanguageTag();
        if (tag.isBlank()) return null;
        return Locale.forLanguageTag(tag);
    }
}
