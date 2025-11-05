package de.murmelmeister.murmelapi.language;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshListener;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.util.Collections;
import java.util.List;

/**
 * LanguageCache provides a Caffeine-backed cache for language lookups by id and language code.
 */
public class LanguageCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<Integer, Language> cacheById;
    private final LoadingCache<String, Integer> codeToId;
    private final LoadingCache<String, List<Language>> listCache;

    public LanguageCache(Database database, String tableName, long cacheCapacity) {
        this.database = database;
        this.tableName = tableName;
        this.cacheById = CacheUtil.buildCache(this::loadById, cacheCapacity);
        this.codeToId = CacheUtil.buildCache(this::loadByCodeKey, cacheCapacity);
        this.listCache = CacheUtil.buildCache(key -> loadAllFromDatabase(), 1);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.LANGUAGES.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_LANGUAGE.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof Integer id)
                    refreshSingle(id);
            } else {
                int id = Integer.parseInt((String) key);
                refreshSingle(id);
            }
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        clear();
    }

    private void refreshAll() {
        clear();
        List<Language> languages = loadAllFromDatabase();
        if (languages.isEmpty())
            return;
        languages.forEach(language -> {
            cacheById.put(language.id(), language);
            codeToId.put(toKey(language.code()), language.id());
        });
        listCache.put(ALL_KEY, List.copyOf(languages));
    }

    private void refreshSingle(int id) {
        remove(id);
        Language language = loadById(id);
        if (language != null)
            put(language);
    }

    private List<Language> loadAllFromDatabase() {
        String sql = "SELECT id, code FROM " + tableName;
        return CacheUtil.loadList(database, sql, null, ResultSetUtil.language());
    }

    private Integer loadByCodeKey(String key) {
        String sql = "SELECT id FROM " + tableName + " WHERE LOWER(code) = ?";
        return CacheUtil.loadSingle(database, sql, null,
                resultSet -> resultSet.getInt("id"),
                stmt -> stmt.setString(1, key));
    }

    private Language loadById(int id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, null, ResultSetUtil.language(),
                stmt -> stmt.setInt(1, id));
    }

    public Language getById(int id) {
        return cacheById.get(id);
    }

    public Language getByCode(String code) {
        if (code == null) return null;
        Integer id = codeToId.get(toKey(code));
        return id != null ? cacheById.get(id) : null;
    }

    public void put(Language language) {
        cacheById.put(language.id(), language);
        codeToId.put(toKey(language.code()), language.id());
        CacheUtil.put(listCache, ALL_KEY, language, v -> v.id() == language.id());
    }

    public void remove(int id) {
        Language removed = cacheById.getIfPresent(id);
        if (removed != null) {
            cacheById.invalidate(id);
            codeToId.invalidate(toKey(removed.code()));
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == id);
    }

    public void clear() {
        cacheById.invalidateAll();
        codeToId.invalidateAll();
        listCache.invalidateAll();
    }

    public List<Language> getCachedLanguages() {
        List<Language> languages = listCache.get(ALL_KEY);
        if (languages == null || languages.isEmpty())
            return Collections.emptyList();
        return List.copyOf(languages);
    }

    private static String toKey(String code) {
        return code.toLowerCase();
    }
}
