package de.murmelmeister.murmelapi.punishment.audit;

import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public interface PunishmentLogProvider {
    void refreshCache();

    PunishmentLog getLog(UUID logId);

    List<PunishmentLog> getLogsByUserId(int userId);

    List<PunishmentLog> getLogsByIpAddress(InetAddress inetAddress);

    List<PunishmentLog> getLogs();

    PunishmentLog create(Integer userId, InetAddress inetAddress, PunishmentReason reason, int createdBy);

    PunishmentLog modify(Integer userId, InetAddress inetAddress, PunishmentReason reason, int createdBy);

    PunishmentLog revoke(Integer userId, InetAddress inetAddress, PunishmentLog log, int createdBy);
}
