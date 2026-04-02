package de.murmelmeister.murmelapi.settings;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface SettingsProvider {
    void refreshCache();

    @NotNull Optional<Settings> findById(@NotNull String tag);

    @NotNull
    @Unmodifiable
    List<Settings> findAll();

    @NotNull Optional<Settings> upsert(@NotNull String tagId, @NotNull String json);

    int delete(@NotNull String tagId);

    @ApiStatus.Internal
    static @NotNull SettingsProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new SettingsProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
