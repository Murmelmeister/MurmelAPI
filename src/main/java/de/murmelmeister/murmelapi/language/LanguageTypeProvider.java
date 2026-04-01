package de.murmelmeister.murmelapi.language;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

public interface LanguageTypeProvider {
    void refreshCache();

    @NotNull Optional<LanguageType> findById(int id);

    @NotNull Optional<LanguageType> findByCode(@NotNull String code);

    @NotNull
    @Unmodifiable
    List<LanguageType> findAll();

    @NotNull Optional<LanguageType> create(@NotNull String code);

    int delete(int id);

    @NotNull Optional<LanguageType> update(int id, @NotNull String code);

    @NotNull Optional<LanguageType> upsert(@NotNull LanguageType language);

    @ApiStatus.Internal
    static @NotNull LanguageTypeProvider of(Database database, Gson gson, RefreshProvider refreshProvider, long cacheCapacity) {
        return new LanguageTypeProviderImpl(database, gson, refreshProvider, cacheCapacity);
    }
}
