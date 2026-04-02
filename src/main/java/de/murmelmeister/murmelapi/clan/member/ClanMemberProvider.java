package de.murmelmeister.murmelapi.clan.member;

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

public interface ClanMemberProvider {
    void refreshCache();

    @NotNull Optional<ClanMember> findMember(@NotNull UUID clanId, int userId);

    @NotNull
    @Unmodifiable
    List<ClanMember> findClan(@NotNull UUID clanId);

    @NotNull
    @Unmodifiable
    List<ClanMember> findClan(int userId);

    @NotNull
    @Unmodifiable
    List<ClanMember> findAll();

    @NotNull Optional<ClanMember> upsert(@NotNull UUID clanId, int userId, @NotNull UUID groupId);

    int delete(@NotNull UUID clanId, int userId);

    @ApiStatus.Internal
    static @NotNull ClanMemberProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new ClanMemberProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
