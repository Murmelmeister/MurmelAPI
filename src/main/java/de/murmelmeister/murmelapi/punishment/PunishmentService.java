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
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;

public record PunishmentService(
        @NotNull PunishmentReasonProvider reasonProvider,
        @NotNull PunishmentLogProvider logProvider,
        @NotNull PunishmentCurrentIpProvider ipProvider,
        @NotNull PunishmentCurrentUserProvider userProvider
) {
    public PunishmentService {
        Objects.requireNonNull(reasonProvider, "reasonProvider must not be null");
        Objects.requireNonNull(logProvider, "logProvider must not be null");
        Objects.requireNonNull(ipProvider, "ipProvider must not be null");
        Objects.requireNonNull(userProvider, "userProvider must not be null");
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

    public int punishedIp(@NotNull InetAddress inetAddress, int reasonId, int createdBy) {
        PunishmentReason reason = reasonProvider.getReason(reasonId);
        PunishmentLog log = reason == null ? null : logProvider.create(null, inetAddress, reason, createdBy);
        if (log == null)
            throw new PunishmentException("Failed to create punishment log for IP: " + inetAddress.getHostAddress() + " with reason: " + reasonId);

        PunishmentCurrentIp punish = ipProvider.create(inetAddress, log.reasonTypeId(), log.id());
        if (punish == null)
            throw new PunishmentException("Failed to create punishment IP: " + inetAddress.getHostAddress() + " with reason: " + reasonId);
        return 2; // Note: 1 for log creation, 1 for IP creation
    }

    public int updatedPunishedIp(@NotNull InetAddress inetAddress, int reasonId, int createdBy) {
        PunishmentReason reason = reasonProvider.getReason(reasonId);
        PunishmentLog log = reason == null ? null : logProvider.modify(null, inetAddress, reason, createdBy);
        if (log == null)
            throw new PunishmentException("Failed to update punishment log for IP: " + inetAddress.getHostAddress() + " with reason: " + reasonId);

        PunishmentCurrentIp punish = ipProvider.getPunishedIp(inetAddress, log.reasonTypeId());
        PunishmentCurrentIp updated = punish == null ? null : ipProvider.update(punish.inetAddress(), punish.typeId(), log.id());
        if (updated == null)
            throw new PunishmentException("Failed to update punishment IP: " + inetAddress.getHostAddress() + " with reason: " + reasonId);
        return 2; // Note: 1 for log creation, 1 for IP update
    }

    public int unpunishedUser(int userId, int typeId, @NotNull UUID logId, int changedBy) {
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

    public int unpunishedIp(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID logId, int changedBy) {
        PunishmentLog currentLog = logProvider.getLog(logId);
        if (currentLog == null)
            throw new PunishmentException("Punishment log not found for IP: " + inetAddress.getHostAddress() + " with type: " + typeId);

        PunishmentLog revokeLog = logProvider.revoke(null, inetAddress, currentLog, changedBy);
        if (revokeLog == null)
            throw new PunishmentException("Failed to revoke punishment log for IP: " + inetAddress.getHostAddress() + " with type: " + typeId);

        int row = ipProvider.delete(inetAddress, typeId);
        if (row < 1)
            throw new PunishmentException("Failed to unpunished IP: " + inetAddress.getHostAddress() + " with type: " + typeId);
        return row;
    }

    public void autoUnpunishedUser(int userId, int typeId) {
        userProvider.delete(userId, typeId);
    }

    public void autoUnpunishedIp(@NotNull InetAddress inetAddress, int typeId) {
        ipProvider.delete(inetAddress, typeId);
    }

    public boolean isPunishedUser(int userId, int typeId) {
        return userProvider.getPunishedUser(userId, typeId) != null;
    }

    public boolean isPunishedIp(@NotNull InetAddress inetAddress, int typeId) {
        return ipProvider.getPunishedIp(inetAddress, typeId) != null;
    }

    public boolean isExpiredUser(@NotNull UUID logId) {
        PunishmentLog log = logProvider.getLog(logId);
        return log != null && log.isExpired();
    }

    public boolean isExpiredIp(@NotNull UUID logId) {
        PunishmentLog log = logProvider.getLog(logId);
        return log != null && log.isExpired();
    }
}
