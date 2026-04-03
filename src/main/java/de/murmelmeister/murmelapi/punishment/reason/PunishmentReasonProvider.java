package de.murmelmeister.murmelapi.punishment.reason;

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

public interface PunishmentReasonProvider {
    void refreshCache();

    @NotNull Optional<PunishmentReason> findReason(int id);

    @NotNull
    @Unmodifiable
    List<PunishmentReason> findReasons(int typeId);

    @NotNull
    @Unmodifiable
    List<PunishmentReason> findAll();

    @NotNull Optional<PunishmentReason> upsert(int id, int typeId, @NotNull String reasonText, @Nullable Long durationSecs,
                                               boolean autoFlagIp, int executorId);

    @NotNull Optional<PunishmentReason> upsert(@NotNull PunishmentReason reason, int executorId);

    int delete(int id);

    @ApiStatus.Internal
    static @NotNull PunishmentReasonProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new PunishmentReasonProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
