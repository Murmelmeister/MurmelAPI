package de.murmelmeister.murmelapi.punishment.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PunishmentUserProvider {
    void refreshCache();

    @NotNull Optional<PunishmentUser> findPunishedUser(@NotNull UUID mojangId, int typeId);

    @NotNull
    @Unmodifiable
    List<PunishmentUser> findPunishedUsers(int typeId);

    @NotNull Optional<PunishmentUser> upsert(@NotNull UUID mojangId, int typeId, @NotNull UUID logId, @Nullable Long durationSecs);

    int delete(@NotNull UUID mojangId, int typeId);

    int loadExpired();
}
