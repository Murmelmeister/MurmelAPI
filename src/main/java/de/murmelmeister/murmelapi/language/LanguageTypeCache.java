package de.murmelmeister.murmelapi.language;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

final class LanguageTypeCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageTypeCache.class);

    @Language(value = "MariaDB")
    private static final String SELECT_ALL = "SELECT id, code FROM %s";
    @Language(value = "MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";
    @Language(value = "MariaDB")
    private static final String SELECT_BY_CODE = "SELECT id FROM %s WHERE LOWER(code) = ?";

    private static final String ALL_KEY = "ALL";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;

    private final LoadingCache<@NotNull Integer, Optional<LanguageType>> cacheById;
    private final LoadingCache<@NotNull String, Optional<Integer>> codeToId;
    private final LoadingCache<@NotNull String, List<LanguageType>> listCache;

    public LanguageTypeCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, long cacheCapacity) {
        this.database = database;
        this.gson = gson;
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
            if (key instanceof LanguageKey language)
                remove(language);
            else if (key instanceof String json) {
                try {
                    final LanguageKey language = gson.fromJson(json, LanguageKey.class);

                    if (language == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(language);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single refresh: {}", json, e);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<LanguageType> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, null, LanguageTypeRowMapper::resultSet);
    }

    private @NotNull Optional<Integer> loadByCodeKey(String key) {
        String sql = SELECT_BY_CODE.formatted(tableName);
        Integer id = CacheUtil.loadSingle(database, sql, null,
                resultSet -> resultSet.getInt("id"),
                stmt -> stmt.setString(1, key));

        return Optional.ofNullable(id);
    }

    private @NotNull Optional<LanguageType> loadById(int id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        LanguageType language = CacheUtil.loadSingle(database, sql, null, LanguageTypeRowMapper::resultSet,
                stmt -> stmt.setInt(1, id));

        return Optional.ofNullable(language);
    }

    public @NotNull Optional<LanguageType> getById(int id) {
        return cacheById.get(id);
    }

    public @NotNull Optional<LanguageType> getByCode(@NotNull String code) {
        Optional<Integer> optId = codeToId.get(toKey(code));
        return optId != null && optId.isPresent() ? getById(optId.get()) : Optional.empty();
    }

    public @NotNull @Unmodifiable List<LanguageType> getAll() {
        List<LanguageType> languages = listCache.get(ALL_KEY);
        if (languages == null || languages.isEmpty())
            return Collections.emptyList();
        return List.copyOf(languages);
    }

    public void remove(@NotNull LanguageKey language) {
        cacheById.invalidate(language.id());
        codeToId.invalidate(toKey(language.code()));
        listCache.invalidate(ALL_KEY);
    }

    public void clear() {
        cacheById.invalidateAll();
        codeToId.invalidateAll();
        listCache.invalidateAll();
    }

    private static @NotNull String toKey(@NotNull String code) {
        return code.toLowerCase();
    }

    record LanguageKey(int id, @NotNull String code) {
    }
}
