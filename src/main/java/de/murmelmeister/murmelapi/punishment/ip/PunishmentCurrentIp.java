package de.murmelmeister.murmelapi.punishment.ip;

import java.net.InetAddress;
import java.util.UUID;

public record PunishmentCurrentIp(InetAddress inetAddress, int typeId, UUID logId) {

    public PunishmentCurrentIp withUpdateLog(UUID logId) {
        return new PunishmentCurrentIp(inetAddress, typeId, logId != null ? logId : this.logId);
    }
}
