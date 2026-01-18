package de.murmelmeister.murmelapi.language;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * LanguageCache provides a Caffeine-backed cache for language lookups by id and language code.
 */
public class LanguageCache implements MurmelCache {
    @org.intellij.lang.annotations.Language(value = "MariaDB")
    private static final String SELECT_ALL = "SELECT id, code FROM %s";
    @org.intellij.lang.annotations.Language(value = "MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";
    @org.intellij.lang.annotations.Language(value = "MariaDB")
    private static final String SELECT_BY_CODE = "SELECT id FROM %s WHERE LOWER(code) = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;

    private final LoadingCache<@NotNull Integer, Optional<Language>> cacheById;
    private final LoadingCache<@NotNull String, Optional<Integer>> codeToId;
    private final LoadingCache<@NotNull String, List<Language>> listCache;

    public LanguageCache(Database database, RefreshProvider refreshProvider, String tableName, long cacheCapacity) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.cacheById = CacheUtil.buildCache(this::loadById, cacheCapacity);
        this.codeToId = CacheUtil.buildCache(this::loadByCodeKey, cacheCapacity);
        this.listCache = CacheUtil.buildCache(key -> loadAllFromDatabase(), 1);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.LANGUAGES.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_LANGUAGE.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof Integer id)
                    remove(id);
            } else {
                int id = Integer.parseInt((String) key);
                remove(id);
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<Language> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, null, ResultSetUtil.language());
    }

    private @NotNull Optional<Integer> loadByCodeKey(String key) {
        String sql = SELECT_BY_CODE.formatted(tableName);
        Integer id = CacheUtil.loadSingle(database, sql, null,
                resultSet -> resultSet.getInt("id"),
                stmt -> stmt.setString(1, key));

        return Optional.ofNullable(id);
    }

    private @NotNull Optional<Language> loadById(int id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        Language language = CacheUtil.loadSingle(database, sql, null, ResultSetUtil.language(),
                stmt -> stmt.setInt(1, id));

        return Optional.ofNullable(language);
    }

    public @Nullable Language getById(int id) {
        Optional<Language> optLang = cacheById.get(id);
        return optLang != null && optLang.isPresent() ? optLang.orElse(null) : null;
    }

    public @Nullable Language getByCode(@Nullable String code) {
        if (code == null) return null;
        Optional<Integer> optId = codeToId.get(toKey(code));
        return optId != null && optId.isPresent() ? getById(optId.get()) : null;
    }

    public void put(@Nullable Language language) {
        if (language == null) return;
        cacheById.put(language.id(), Optional.of(language));
        codeToId.put(toKey(language.code()), Optional.of(language.id()));
        CacheUtil.put(listCache, ALL_KEY, language, v -> v.id() == language.id());
    }

    public void remove(int id) {
        Optional<Language> optLang = cacheById.getIfPresent(id);
        cacheById.invalidate(id);

        if (optLang != null && optLang.isPresent()) {
            Language language = optLang.get();
            codeToId.invalidate(toKey(language.code()));
        }
        CacheUtil.remove(listCache, ALL_KEY, v -> v.id() == id);
    }

    public void clear() {
        cacheById.invalidateAll();
        codeToId.invalidateAll();
        listCache.invalidateAll();
    }

    public @NotNull List<Language> getCachedLanguages() {
        List<Language> languages = listCache.get(ALL_KEY);
        if (languages == null || languages.isEmpty())
            return Collections.emptyList();
        return List.copyOf(languages);
    }

    private static @NotNull String toKey(@NotNull String code) {
        return code.toLowerCase();
    }
}
