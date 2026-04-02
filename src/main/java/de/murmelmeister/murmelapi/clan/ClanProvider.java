package de.murmelmeister.murmelapi.clan;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClanProvider {
    void refreshCache();

    @NotNull Optional<Clan> findById(@NotNull UUID id);

    @NotNull Optional<Clan> findByName(@NotNull String name);

    @NotNull Optional<Clan> findByOwner(int ownerId);

    @NotNull
    @Unmodifiable
    List<Clan> findAll();

    @NotNull Optional<Clan> upsert(@NotNull UUID id, @NotNull String name, @NotNull String tag, @NotNull String sign, @NotNull String description, int ownerId, int executorId);

    int delete(@NotNull UUID id);

    @ApiStatus.Internal
    static @NotNull ClanProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new ClanProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
