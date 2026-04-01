package de.murmelmeister.murmelapi.language;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.language.LanguageException;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.murmelmeister.murmelapi.MurmelAPI.ENGLISH_CODE;
import static de.murmelmeister.murmelapi.MurmelAPI.GERMAN_CODE;

final class LanguageTypeProviderImpl implements LanguageTypeProvider {
    private static final String TABLE_NAME = "languages";

    @org.intellij.lang.annotations.Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (code)
            VALUES (?)
            RETURNING id, code
            """.formatted(TABLE_NAME);

    @org.intellij.lang.annotations.Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    @org.intellij.lang.annotations.Language("MariaDB")
    private static final String UPDATE_SQL = "UPDATE %s SET code = ? WHERE id = ?".formatted(TABLE_NAME);

    @org.intellij.lang.annotations.Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (code)
            VALUES (?)
            ON DUPLICATE KEY UPDATE code = VALUES(code)
            RETURNING id, code
            """.formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final LanguageTypeCache cache;
    private final RefreshType all = RefreshType.LANGUAGES;
    private final RefreshType single = RefreshType.SINGLE_LANGUAGE;

    public LanguageTypeProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, long cacheCapacity) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new LanguageTypeCache(database, gson, refreshProvider, TABLE_NAME, cacheCapacity);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @NotNull Optional<LanguageType> findById(int id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull Optional<LanguageType> findByCode(@NotNull String code) {
        return cache.getByCode(code);
    }

    @Override
    public @NotNull @Unmodifiable List<LanguageType> findAll() {
        return cache.getAll();
    }

    @Override
    public @NotNull Optional<LanguageType> create(@NotNull String code) {
        Objects.requireNonNull(code, "code cannot be null");
        if (code.isBlank()) throw new IllegalArgumentException("code must not be blank");

        LanguageType language = MurmelExceptionWrapper.dbWrap(
                "Failed to create Language (code=" + code + ")",
                () -> database.query(CREATE_SQL, null, LanguageTypeAdapter::resultSet, stmt -> stmt.setString(1, code)),
                LanguageException::new
        );

        if (language == null) return Optional.empty();
        refreshProvider.fireSingle(single, language);
        return Optional.of(language);
    }

    @Override
    public int delete(int id) {
        if (id < 1) throw new IllegalArgumentException("id must be >= 1");
        if (id == 1 || id == 2) throw new IllegalArgumentException("id must not be 1 or 2");

        Optional<LanguageType> existing = cache.getById(id);
        if (existing.isEmpty()) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete Language (id=" + id + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setInt(1, id)),
                LanguageException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, existing.get());
        return row;
    }

    @Override
    public @NotNull Optional<LanguageType> update(int id, @NotNull String code) {
        if (id < 1) throw new IllegalArgumentException("id must be >= 1");
        if (code.isBlank()) throw new IllegalArgumentException("code must not be blank");
        if ((id == 1 && !ENGLISH_CODE.equals(code))
                || (id == 2 && !GERMAN_CODE.equals(code)))
            throw new IllegalArgumentException("code must not be changed for id 1 or 2");

        Optional<LanguageType> existingOpt = cache.getById(id);
        if (existingOpt.isEmpty()) return Optional.empty();
        LanguageType existing = existingOpt.get();

        if (Objects.equals(code, existing.code()))
            return existingOpt;

        int rows = MurmelExceptionWrapper.dbWrap(
                "Failed to update Language (id=" + id + ", code=" + code + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setString(1, code);
                    stmt.setInt(2, id);
                }),
                LanguageException::new
        );
        if (rows != 1) return Optional.empty();

        LanguageType language = LanguageType.of(existing.id(), code);
        refreshProvider.fireSingle(single, language);
        return Optional.of(language);
    }

    @Override
    public @NotNull Optional<LanguageType> upsert(@NotNull LanguageType language) {
        Objects.requireNonNull(language, "language cannot be null");

        Optional<LanguageType> existingOpt = cache.getById(language.id());
        if (existingOpt.isPresent()
                && Objects.equals(language.id(), existingOpt.get().id())
                && Objects.equals(language.code(), existingOpt.get().code()))
            return existingOpt;

        LanguageType saved = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert Language (id=" + language.id() + ", code=" + language.code() + ")",
                () -> database.query(UPSERT_SQL, null, LanguageTypeAdapter::resultSet, stmt -> stmt.setString(1, language.code())),
                LanguageException::new
        );

        if (saved == null) return Optional.empty();
        refreshProvider.fireSingle(single, saved);
        return Optional.of(saved);
    }
}
