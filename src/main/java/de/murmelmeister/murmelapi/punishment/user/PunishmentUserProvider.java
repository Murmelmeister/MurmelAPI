package de.murmelmeister.murmelapi.punishment.user;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PunishmentUserProvider {
    void refreshCache();

    @NotNull Optional<PunishmentUser> findPunishedUser(@NotNull UUID mojangId, int typeId);

    @NotNull
    @Unmodifiable
    List<PunishmentUser> findPunishedUsers(int typeId);

    @NotNull Optional<PunishmentUser> upsert(@NotNull UUID mojangId, int typeId, @NotNull UUID auditId, @Nullable Long durationSecs);

    int delete(@NotNull UUID mojangId, int typeId);

    int loadExpired();

    @ApiStatus.Internal
    static @NotNull PunishmentUserProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new PunishmentUserProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
