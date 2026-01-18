package de.murmelmeister.murmelapi.language;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface LanguageProvider {
    void refreshCache();

    @Nullable Language findById(int id);

    @Nullable Language findByCode(@Nullable String code);

    @NotNull List<Language> findAll();

    @Nullable Language create(@NotNull String code);

    int delete(int id);

    @Nullable Language update(int id, @NotNull String code);

    @Nullable Language upsert(@NotNull Language language);
}
