package de.murmelmeister.murmelapi.language;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.language.message.MessageProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

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
    private static final LanguageCache CACHE = new LanguageCache();
    private final Database database;
    private final MessageProvider messageProvider;

    public LanguageProvider(Database database, MessageProvider messageProvider) {
        this.database = database;
        this.messageProvider = messageProvider;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) UNIQUE");
        database.update("INSERT IGNORE INTO " + TABLE_NAME + " (name) VALUES ('English'), ('German')");
    }

    static {
        RefreshUtil.register(cacheName -> {
            if ("languages".equals(cacheName) || "global".equals(cacheName))
                CACHE.clear();
        });
    }

    /**
     * Loads all languages from the database into the cache.
     * <p>
     * Clears any existing cache state, fetches every row from the "languages" table,
     * constructs a {@link Language} for each record, populates the cache, and returns
     * whether any entries were loaded.
     * </p>
     */
    public void loadData() {
        CACHE.clear();
        String sql = "SELECT id, name FROM " + TABLE_NAME;
        List<Language> languages = database.queryList(sql, result -> {
            int id = result.getInt("id");
            String name = result.getString("name");
            return new Language(id, name);
        });

        languages.forEach(CACHE::put);
    }

    /**
     * Ensures that the cache is loaded with languages.
     * <p>
     * If the cache is empty, it calls {@link #loadData()} to populate it.
     * </p>
     */
    private void ensureLoaded() {
        if (CACHE.getLanguages().isEmpty())
            loadData();
    }

    /**
     * Retrieves all cached languages.
     *
     * @return A list of {@link Language} instances currently in the cache
     */
    public List<Language> getLanguages() {
        ensureLoaded();
        return CACHE.getLanguages();
    }

    /**
     * Retrieves the names of all cached languages.
     *
     * @return A set of language names (case-sensitive) currently in the cache
     */
    public Set<String> getNames() {
        ensureLoaded();
        return CACHE.getNames();
    }

    /**
     * Retrieves a single language by its primary key from the cache.
     *
     * @param id The unique identifier of the language
     * @return The cached {@link Language}, or {@code null} if not found
     */
    public Language get(int id) {
        ensureLoaded();
        return CACHE.getById(id);
    }

    /**
     * Retrieves a language by its name from the cache.
     *
     * @param name The language name (case‐insensitive)
     * @return The cached {@link Language}, or null if not found
     */
    public Language get(String name) {
        ensureLoaded();
        return CACHE.getByName(name);
    }

    /**
     * Checks whether a language with the given ID exists in the cache.
     *
     * @param id The primary key to check
     * @return {@code true} if present; {@code false} otherwise
     */
    public boolean exists(int id) {
        ensureLoaded();
        return CACHE.containsId(id);
    }

    /**
     * Checks whether a language with the given name exists in the cache.
     *
     * @param name The language name to check (case‐insensitive)
     * @return True if present; false otherwise
     */
    public boolean exists(String name) {
        ensureLoaded();
        return CACHE.containsName(name);
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
    public Language create(String name) {
        if (name == null || name.isEmpty()) return null;
        String sql = "INSERT INTO " + TABLE_NAME + " (name) VALUES (?)";
        int id = database.updateAndGetAutoIncrement(sql, name);
        if (id < 1) return null;
        Language language = new Language(id, name);
        CACHE.put(language);
        return language;
    }

    /**
     * Deletes a language record by its primary key from the database and removes it from the cache.
     * <p>
     * If the ID is less than 1, no deletion occurs. The method also deletes any associated
     * messages for this language via {@link MessageProvider#delete(int)}.
     * </p>
     *
     * @param id The primary key of the language to delete (must be ≥ 1)
     * @return The number of affected rows, including both language and message deletions
     */
    public int delete(int id) {
        if (id < 1) return 0;
        int affectedMessageRows = messageProvider.delete(id);
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id=?";
        int affectedRow = database.update(sql, id);
        CACHE.remove(id);
        return affectedMessageRows + affectedRow;
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
    public Language update(int id, String name) {
        if (id < 1 || name == null || name.isEmpty()) return null;
        String sql = "UPDATE " + TABLE_NAME + " SET name=? WHERE id=?";
        int affectedRow = database.update(sql, name, id);
        if (affectedRow > 0) {
            Language language = CACHE.getById(id);
            if (language != null) language.setName(name);
            else language = new Language(id, name);
            CACHE.put(language);
        }
        return CACHE.getById(id);
    }
}
