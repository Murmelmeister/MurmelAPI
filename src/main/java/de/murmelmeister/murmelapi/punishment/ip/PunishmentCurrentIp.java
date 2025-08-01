package de.murmelmeister.murmelapi.punishment.ip;

import java.util.UUID;

public record PunishmentCurrentIp(String ipAddress, int typeId, UUID logId) {

    public PunishmentCurrentIp withUpdateLog(UUID logId) {
        return new PunishmentCurrentIp(ipAddress, typeId, logId != null ? logId : this.logId);
    }
}
