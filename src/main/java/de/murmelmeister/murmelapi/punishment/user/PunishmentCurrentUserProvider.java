package de.murmelmeister.murmelapi.punishment.user;

import java.util.List;
import java.util.UUID;

public interface PunishmentCurrentUserProvider {
    void refreshCache();

    List<Integer> getAllPunishedUserIds(int typeId);

    PunishmentCurrentUser getPunishedUser(int userId, int typeId);

    PunishmentCurrentUser create(int userId, int typeId, UUID logId);

    int delete(int userId, int typeId);

    PunishmentCurrentUser update(int userId, int typeId, UUID logId);
}
