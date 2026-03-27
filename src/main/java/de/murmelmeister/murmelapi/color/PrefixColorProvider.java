package de.murmelmeister.murmelapi.color;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface PrefixColorProvider {
    void refreshCache();

    @NotNull Optional<PrefixColor> findById(@NotNull String id);

    @NotNull
    @Unmodifiable
    List<PrefixColor> findAll();

    @NotNull Optional<PrefixColor> upsert(@NotNull String id, @NotNull String color, boolean animated, int executorId);

    @NotNull Optional<PrefixColor> upsert(@NotNull PrefixColor prefixColor, int executorId);

    int delete(@NotNull String id);

    @ApiStatus.Internal
    static @NotNull PrefixColorProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new PrefixColorProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
