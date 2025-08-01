package de.murmelmeister.murmelapi.punishment.user;

import java.util.UUID;

public record PunishmentCurrentUser(int userId, int typeId, UUID logId) {

    public PunishmentCurrentUser withUpdateLog(UUID logId) {
        return new PunishmentCurrentUser(userId, typeId, logId != null ? logId : this.logId);
    }
}
