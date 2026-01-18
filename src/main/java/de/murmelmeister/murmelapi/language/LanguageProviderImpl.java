package de.murmelmeister.murmelapi.language;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private final RefreshProvider refreshProvider;
    private final LanguageCache cache;
    private final RefreshType all = RefreshType.LANGUAGES;
    private final RefreshType single = RefreshType.SINGLE_LANGUAGE;

    public LanguageProviderImpl(Database database, RefreshProvider refreshProvider, long cacheCapacity) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new LanguageCache(database, refreshProvider, TABLE_NAME, cacheCapacity);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable Language findById(int id) {
        return cache.getById(id);
    }

    @Override
    public @Nullable Language findByCode(String code) {
        String normalized = normalize(code);
        return normalized != null ? cache.getByCode(normalized) : null;
    }

    @Override
    public @NotNull List<Language> findAll() {
        return cache.getCachedLanguages();
    }

    @Override
    public @Nullable Language create(@NotNull String code) {
        String normalized = normalize(code);
        if (normalized == null) return null;

        @org.intellij.lang.annotations.Language("MariaDB")
        String sql = "INSERT INTO %s (code) VALUES (?)".formatted(TABLE_NAME);
        int id = (int) database.updateAndGetGeneratedKeys(sql,
                stmt -> stmt.setString(1, normalized));
        if (id < 1) return null;

        Language language = new Language(id, normalized);
        refreshProvider.fireSingle(single, id);
        return language;
    }

    @Override
    public int delete(int id) {
        if (id < 1 || id == 1 || id == 2) return 0;

        @org.intellij.lang.annotations.Language("MariaDB")
        String sql = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);
        int row = database.update(sql,
                stmt -> stmt.setInt(1, id));
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, id);
        return row;
    }

    @Override
    public @Nullable Language update(int id, @NotNull String code) {
        if (id < 1) return null;
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

        @org.intellij.lang.annotations.Language("MariaDB")
        String sql = "UPDATE %s SET code = ? WHERE id = ?".formatted(TABLE_NAME);
        int rows = database.update(sql, stmt -> {
            stmt.setString(1, normalized);
            stmt.setInt(2, id);
        });
        if (rows < 1) return null;

        Language language = Language.builder(existing)
                .code(code)
                .build();
        refreshProvider.fireSingle(single, id);
        return language;
    }

    @Override
    public @Nullable Language upsert(@NotNull Language language) {
        Language existing = cache.getById(language.id());
        if (existing != null
                && Objects.equals(language.id(), existing.id())
                && Objects.equals(language.code(), existing.code()))
            return existing;

        @org.intellij.lang.annotations.Language("MariaDB")
        String sql = """
                INSERT INTO %s (code)
                VALUES (?)
                ON DUPLICATE KEY UPDATE code = VALUES(code)
                RETURNING id, code
                """.formatted(TABLE_NAME);
        Language saved = database.query(sql, null, ResultSetUtil.language(), stmt -> stmt.setString(1, language.code()));
        if (saved == null) return null;
        refreshProvider.fireSingle(single, saved.id());
        return saved;
    }

    private String normalize(String code) {
        if (code == null) return null;
        if (code.isBlank()) return null;
        return code;
    }
}
