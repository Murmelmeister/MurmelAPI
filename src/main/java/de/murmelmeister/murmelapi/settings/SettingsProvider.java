package de.murmelmeister.murmelapi.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SettingsProvider {
    void refreshCache();

    @Nullable Settings findById(@Nullable String tag);

    @NotNull List<Settings> findAll();

    @Nullable Settings create(@NotNull String tagId, @NotNull String json);

    @Nullable Settings update(@NotNull String tagId, @NotNull String json);

    @Nullable Settings upsert(@NotNull String tagId, @NotNull String json);

    int delete(@NotNull String tagId);
}
