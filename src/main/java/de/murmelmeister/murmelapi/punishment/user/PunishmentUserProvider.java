package de.murmelmeister.murmelapi.punishment.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.UUID;

public interface PunishmentUserProvider {
    void refreshCache();

    @Nullable PunishmentUser findPunishedUser(@NotNull UUID userId, int typeId);

    @NotNull
    @Unmodifiable
    List<PunishmentUser> findPunishedUsers(int typeId);

    @Nullable PunishmentUser create(@NotNull UUID userId, int typeId, @NotNull UUID logId);

    int delete(@NotNull UUID userId, int typeId);

    @Nullable PunishmentUser update(@NotNull UUID userId, int typeId, @NotNull UUID logId);
}
