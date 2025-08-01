package de.murmelmeister.murmelapi.punishment.audit;

import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;

import java.util.List;
import java.util.UUID;

public interface PunishmentLogProvider {
    void closeCache();

    void refreshCache();

    PunishmentLog getLog(UUID logId);

    List<PunishmentLog> getLogsByUserId(int userId);

    List<PunishmentLog> getLogsByIpAddress(String ipAddress);

    List<PunishmentLog> getLogs();

    PunishmentLog create(Integer userId, String ipAddress, PunishmentReason reason, int createdBy);

    PunishmentLog modify(Integer userId, String ipAddress, PunishmentReason reason, int createdBy);

    PunishmentLog revoke(Integer userId, String ipAddress, PunishmentLog log, int createdBy);
}
