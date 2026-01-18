package de.murmelmeister.murmelapi.punishment.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.UUID;

public interface PunishmentCurrentUserProvider {
    void refreshCache();

    @NotNull
    @Unmodifiable
    List<Integer> getAllPunishedUserIds(int typeId);

    @Nullable PunishmentCurrentUser getPunishedUser(int userId, int typeId);

    @Nullable PunishmentCurrentUser create(int userId, int typeId, @NotNull UUID logId);

    int delete(int userId, int typeId);

    @Nullable PunishmentCurrentUser update(int userId, int typeId, @NotNull UUID logId);
}
