package de.murmelmeister.murmelapi.punishment.ip;

import java.util.List;
import java.util.UUID;

public interface PunishmentCurrentIpProvider {
    void closeCache();

    void refreshCache();

    List<String> getAllPunishedIps(int typeId);

    PunishmentCurrentIp getPunishedIp(String ipAddress, int typeId);

    PunishmentCurrentIp create(String ipAddress, int typeId, UUID logId);

    int delete(String ipAddress, int typeId);

    PunishmentCurrentIp update(String ipAddress, int typeId, UUID logId);
}
