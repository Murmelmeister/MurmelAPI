package de.murmelmeister.murmelapi.language;

import de.murmelmeister.murmelapi.database.Database;

import java.util.List;

public final class LanguageProvider {
    private static final String TABLE_NAME = "languages";
    private final LanguageCache cache = new LanguageCache();
    private final Database database;

    public LanguageProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) UNIQUE");
    }

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

    public Language getLanguage(int id) {
        return cache.get(id);
    }

    public boolean existsLanguage(int id) {
        return cache.containsKey(id);
    }

    public Language createLanguage(String name) {
        if (name == null || name.isEmpty()) return null;
        String sql = "INSERT INTO " + TABLE_NAME + " (name) VALUES (?)";
        int id = database.updateAndGetAutoIncrement(sql, name);
        if (id < 1) return null;
        Language language = new Language(id, name);
        cache.put(language);
        return language;
    }

    public int deleteLanguage(int id) {
        if (id < 1) return 0;
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id=?";
        int affectedRow = database.update(sql, id);
        cache.remove(id);
        return affectedRow;
    }

    public Language updateLanguage(int id, String name) {
        if (id < 1 || name == null || name.isEmpty()) return null;
        String sql = "UPDATE " + TABLE_NAME + " SET name=? WHERE id=?";
        int affectedRow = database.update(sql, name, id);
        if (affectedRow > 0) {
            Language language = cache.get(id);
            if (language != null) language.setName(name);
            else language = new Language(id, name);
            cache.put(language);
        }
        return cache.get(id);
    }
}
