package de.murmelmeister.murmelapi.language;

import de.murmelmeister.murmelapi.database.Database;

import java.util.List;
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
public final class LanguageProvider {
    private static final String TABLE_NAME = "languages";
    private final LanguageCache cache = new LanguageCache();
    private final Database database;

    public LanguageProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) UNIQUE");
        database.update("INSERT IGNORE INTO " + TABLE_NAME + " (name) VALUES ('English'), ('German')");
    }

    /**
     * Loads all languages from the database into the cache.
     * <p>
     * Clears any existing cache state, fetches every row from the "languages" table,
     * constructs a {@link Language} for each record, populates the cache, and returns
     * whether any entries were loaded.
     * </p>
     *
     * @return {@code true} if one or more languages were loaded; {@code false} otherwise
     */
    public boolean loadData() {
        cache.clear();
        String sql = "SELECT id, name FROM " + TABLE_NAME;
        List<Language> languages = database.queryList(sql, result -> {
            int id = result.getInt("id");
            String name = result.getString("name");
            return new Language(id, name);
        });

        languages.forEach(cache::put);
        return !languages.isEmpty();
    }

    /**
     * Retrieves all cached languages.
     *
     * @return A list of {@link Language} instances currently in the cache
     */
    public List<Language> getLanguages() {
        return cache.getLanguages();
    }

    /**
     * Retrieves the names of all cached languages.
     *
     * @return A set of language names (case-sensitive) currently in the cache
     */
    public Set<String> getLanguageNames() {
        return cache.getNames();
    }

    /**
     * Retrieves a single language by its primary key from the cache.
     *
     * @param id The unique identifier of the language
     * @return The cached {@link Language}, or {@code null} if not found
     */
    public Language getLanguage(int id) {
        return cache.getById(id);
    }

    /**
     * Retrieves a language by its name from the cache.
     *
     * @param name The language name (case‐insensitive)
     * @return The cached {@link Language}, or null if not found
     */
    public Language getLanguage(String name) {
        return cache.getByName(name);
    }

    /**
     * Checks whether a language with the given ID exists in the cache.
     *
     * @param id The primary key to check
     * @return {@code true} if present; {@code false} otherwise
     */
    public boolean existsLanguage(int id) {
        return cache.containsId(id);
    }

    /**
     * Checks whether a language with the given name exists in the cache.
     *
     * @param name The language name to check (case‐insensitive)
     * @return True if present; false otherwise
     */
    public boolean existsLanguage(String name) {
        return cache.containsName(name);
    }

    /**
     * Inserts a new language record into the database and updates the cache.
     * <p>
     * Validates that {@code name} is non-null and non-empty. Upon successful insertion,
     * returns the newly created {@link Language} with its generated ID and stores it
     * in the cache.
     * </p>
     *
     * @param name The language name (must not be null or empty)
     * @return The newly created {@link Language}, or {@code null} if validation fails or insertion did not succeed
     */
    public Language createLanguage(String name) {
        if (name == null || name.isEmpty()) return null;
        String sql = "INSERT INTO " + TABLE_NAME + " (name) VALUES (?)";
        int id = database.updateAndGetAutoIncrement(sql, name);
        if (id < 1) return null;
        Language language = new Language(id, name);
        cache.put(language);
        return language;
    }

    /**
     * Deletes a language by its ID from both the database and the cache.
     *
     * @param id The primary key of the language to delete
     * @return The number of rows affected (0 if {@code id < 1} or no record was deleted)
     */
    public int deleteLanguage(int id) {
        if (id < 1) return 0;
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id=?";
        int affectedRow = database.update(sql, id);
        cache.remove(id);
        return affectedRow;
    }

    /**
     * Updates an existing language record in the database and refreshes the cache entry.
     * <p>
     * Validates inputs, performs the SQL update, and if successful, updates the cached
     * {@link Language} instance (or creates a new one in the cache if it was not already present).
     * </p>
     *
     * @param id   The primary key of the language to update (must be ≥ 1)
     * @param name The new language name (must not be null or empty)
     * @return The updated {@link Language} from cache, or {@code null} if validation fails
     */
    public Language updateLanguage(int id, String name) {
        if (id < 1 || name == null || name.isEmpty()) return null;
        String sql = "UPDATE " + TABLE_NAME + " SET name=? WHERE id=?";
        int affectedRow = database.update(sql, name, id);
        if (affectedRow > 0) {
            Language language = cache.getById(id);
            if (language != null) language.setName(name);
            else language = new Language(id, name);
            cache.put(language);
        }
        return cache.getById(id);
    }
}
