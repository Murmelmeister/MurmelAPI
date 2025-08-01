package de.murmelmeister.murmelapi.punishment;

import de.murmelmeister.murmelapi.exceptions.punishment.PunishmentException;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLogProvider;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIp;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIpProvider;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReasonProvider;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUser;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProvider;

import java.util.UUID;

public final class PunishmentService {
    private final PunishmentReasonProvider reasonProvider;
    private final PunishmentLogProvider logProvider;
    private final PunishmentCurrentIpProvider ipProvider;
    private final PunishmentCurrentUserProvider userProvider;

    public PunishmentService(PunishmentReasonProvider reasonProvider, PunishmentLogProvider logProvider, PunishmentCurrentIpProvider ipProvider, PunishmentCurrentUserProvider userProvider) {
        this.reasonProvider = reasonProvider;
        this.logProvider = logProvider;
        this.ipProvider = ipProvider;
        this.userProvider = userProvider;
    }

    public int punishedUser(int userId, int reasonId, int createdBy) {
        PunishmentReason reason = reasonProvider.getReason(reasonId);
        PunishmentLog log = reason == null ? null : logProvider.create(userId, null, reason, createdBy);
        if (log == null)
            throw new PunishmentException("Failed to create punishment log for user: " + userId + " with reason: " + reasonId);

        PunishmentCurrentUser punish = userProvider.create(userId, log.reasonTypeId(), log.id());
        if (punish == null)
            throw new PunishmentException("Failed to create punishment user: " + userId + " with reason: " + reasonId);
        return 2; // Note: 1 for log creation, 1 for user creation
    }

    public int updatedPunishedUser(int userId, int reasonId, int createdBy) {
        PunishmentReason reason = reasonProvider.getReason(reasonId);
        PunishmentLog log = reason == null ? null : logProvider.modify(userId, null, reason, createdBy);
        if (log == null)
            throw new PunishmentException("Failed to update punishment log for user: " + userId + " with reason: " + reasonId);

        PunishmentCurrentUser punish = userProvider.getPunishedUser(userId, log.reasonTypeId());
        PunishmentCurrentUser updated = punish == null ? null : userProvider.update(punish.userId(), punish.typeId(), log.id());
        if (updated == null)
            throw new PunishmentException("Failed to update punishment user: " + userId + " with reason: " + reasonId);
        return 2; // Note: 1 for log creation, 1 for user update
    }

    public int punishedIp(String ipAddress, int reasonId, int createdBy) {
        PunishmentReason reason = reasonProvider.getReason(reasonId);
        PunishmentLog log = reason == null ? null : logProvider.create(null, ipAddress, reason, createdBy);
        if (log == null)
            throw new PunishmentException("Failed to create punishment log for IP: " + ipAddress + " with reason: " + reasonId);

        PunishmentCurrentIp punish = ipProvider.create(ipAddress, log.reasonTypeId(), log.id());
        if (punish == null)
            throw new PunishmentException("Failed to create punishment IP: " + ipAddress + " with reason: " + reasonId);
        return 2; // Note: 1 for log creation, 1 for IP creation
    }

    public int updatedPunishedIp(String ipAddress, int reasonId, int createdBy) {
        PunishmentReason reason = reasonProvider.getReason(reasonId);
        PunishmentLog log = reason == null ? null : logProvider.modify(null, ipAddress, reason, createdBy);
        if (log == null)
            throw new PunishmentException("Failed to update punishment log for IP: " + ipAddress + " with reason: " + reasonId);

        PunishmentCurrentIp punish = ipProvider.getPunishedIp(ipAddress, log.reasonTypeId());
        PunishmentCurrentIp updated = punish == null ? null : ipProvider.update(punish.ipAddress(), punish.typeId(), log.id());
        if (updated == null)
            throw new PunishmentException("Failed to update punishment IP: " + ipAddress + " with reason: " + reasonId);
        return 2; // Note: 1 for log creation, 1 for IP update
    }

    public int unpunishedUser(int userId, int typeId, UUID logId, int changedBy) {
        PunishmentLog currentLog = logProvider.getLog(logId);
        if (currentLog == null)
            throw new PunishmentException("Punishment log not found for user: " + userId + " with type: " + typeId);

        PunishmentLog revokeLog = logProvider.revoke(userId, null, currentLog, changedBy);
        if (revokeLog == null)
            throw new PunishmentException("Failed to revoke punishment log for user: " + userId + " with type: " + typeId);

        int row = userProvider.delete(userId, typeId);
        if (row < 1)
            throw new PunishmentException("Failed to unpunished user: " + userId + " with type: " + typeId);
        return row;
    }

    public int unpunishedIp(String ipAddress, int typeId, UUID logId, int changedBy) {
        PunishmentLog currentLog = logProvider.getLog(logId);
        if (currentLog == null)
            throw new PunishmentException("Punishment log not found for IP: " + ipAddress + " with type: " + typeId);

        PunishmentLog revokeLog = logProvider.revoke(null, ipAddress, currentLog, changedBy);
        if (revokeLog == null)
            throw new PunishmentException("Failed to revoke punishment log for IP: " + ipAddress + " with type: " + typeId);

        int row = ipProvider.delete(ipAddress, typeId);
        if (row < 1)
            throw new PunishmentException("Failed to unpunished IP: " + ipAddress + " with type: " + typeId);
        return row;
    }

    public void autoUnpunishedUser(int userId, int typeId) {
        userProvider.delete(userId, typeId);
    }

    public void autoUnpunishedIp(String ipAddress, int typeId) {
        ipProvider.delete(ipAddress, typeId);
    }

    public boolean isPunishedUser(int userId, int typeId) {
        return userProvider.getPunishedUser(userId, typeId) != null;
    }

    public boolean isPunishedIp(String ipAddress, int typeId) {
        return ipProvider.getPunishedIp(ipAddress, typeId) != null;
    }

    public boolean isExpiredUser(UUID logId) {
        PunishmentLog log = logProvider.getLog(logId);
        return log != null && log.isExpired();
    }

    public boolean isExpiredIp(UUID logId) {
        PunishmentLog log = logProvider.getLog(logId);
        return log != null && log.isExpired();
    }
}
