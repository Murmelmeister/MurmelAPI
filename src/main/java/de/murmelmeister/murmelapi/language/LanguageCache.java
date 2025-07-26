package de.murmelmeister.murmelapi.language;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshListener;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.util.List;

/**
 * LanguageCache is a thread-safe cache for storing Language objects.
 * It uses a ConcurrentHashMap to allow concurrent access and modifications.
 */
public class LanguageCache implements RefreshListener, AutoCloseable {
    private static final String ALL_KEY = "ALL";
    private final Database database;
    private final String tableName;
    private final LoadingCache<Integer, Language> cacheById;
    private final LoadingCache<String, Integer> nameToId;
    private final LoadingCache<String, List<Language>> listCache;

    public LanguageCache(Database database, String tableName, long cacheCapacity) {
        this.database = database;
        this.tableName = tableName;
        this.cacheById = CacheUtil.buildCache(this::loadById, cacheCapacity);
        this.nameToId = CacheUtil.buildCache(this::loadByName, cacheCapacity);
        this.listCache = CacheUtil.buildCache(key -> loadAllFromDatabase(), 1);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.getType();
        if (RefreshType.LANGUAGES.getName().equalsIgnoreCase(cacheName)
            || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_LANGUAGE.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.getKey();
            if (key instanceof Integer id)
                remove(id);
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
        languages.forEach(this::put);
    }

    private List<Language> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, null, ResultSetUtil.language());
    }

    private Integer loadByName(String name) {
        String sql = "SELECT id FROM " + tableName + " WHERE name = ?";
        return CacheUtil.loadSingle(database, sql, null, resultSet -> resultSet.getInt("id"), name);
    }

    private Language loadById(int id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, null, ResultSetUtil.language(), id);
    }

    public Language getById(int id) {
        return cacheById.get(id);
    }

    public Language getByName(String name) {
        if (name == null) return null;
        Integer id = nameToId.get(name.toLowerCase());
        return id != null ? cacheById.get(id) : null;
    }

    public void put(Language language) {
        cacheById.put(language.id(), language);
        nameToId.put(language.name().toLowerCase(), language.id());
        CacheUtil.put(listCache, ALL_KEY, language, v -> v.id() == language.id());
    }

    public void remove(int id) {
        Language removed = cacheById.getIfPresent(id);
        if (removed != null) {
            cacheById.invalidate(id);
            nameToId.invalidate(removed.name().toLowerCase());
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == id);
    }

    public void clear() {
        cacheById.invalidateAll();
        nameToId.invalidateAll();
        listCache.invalidateAll();
    }

    public List<Language> getCachedLanguages() {
        return listCache.get(ALL_KEY);
    }
}
