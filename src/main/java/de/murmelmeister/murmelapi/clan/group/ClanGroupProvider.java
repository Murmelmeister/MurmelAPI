package de.murmelmeister.murmelapi.clan.group;

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

public interface ClanGroupProvider {
    void refreshCache();

    @NotNull Optional<ClanGroup> findById(@NotNull UUID clanId, @NotNull UUID groupId);

    @NotNull
    @Unmodifiable
    List<ClanGroup> findByClanId(@NotNull UUID clanId);

    @NotNull
    @Unmodifiable
    List<ClanGroup> findAll();

    @NotNull Optional<ClanGroup> upsert(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String groupName, int priority, boolean defaultGroup, int executorId);

    int delete(@NotNull UUID clanId, @NotNull UUID groupId);

    @ApiStatus.Internal
    static @NotNull ClanGroupProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new ClanGroupProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
