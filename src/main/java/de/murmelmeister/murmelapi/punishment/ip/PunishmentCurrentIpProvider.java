package de.murmelmeister.murmelapi.punishment.ip;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public interface PunishmentCurrentIpProvider {
    void refreshCache();

    List<InetAddress> getAllPunishedIps(int typeId);

    PunishmentCurrentIp getPunishedIp(InetAddress inetAddress, int typeId);

    PunishmentCurrentIp create(InetAddress inetAddress, int typeId, UUID logId);

    int delete(InetAddress inetAddress, int typeId);

    PunishmentCurrentIp update(InetAddress inetAddress, int typeId, UUID logId);
}
