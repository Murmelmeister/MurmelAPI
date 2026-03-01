package de.murmelmeister.murmelapi.punishment;

import de.murmelmeister.murmelapi.exceptions.punishment.PunishmentIpAddressException;
import de.murmelmeister.murmelapi.exceptions.punishment.PunishmentAuditException;
import de.murmelmeister.murmelapi.exceptions.punishment.PunishmentReasonException;
import de.murmelmeister.murmelapi.exceptions.punishment.PunishmentUserException;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAudit;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAuditProvider;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentIpAddress;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentIpAddressProvider;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReasonProvider;
import de.murmelmeister.murmelapi.punishment.user.PunishmentUser;
import de.murmelmeister.murmelapi.punishment.user.PunishmentUserProvider;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public record PunishmentService(
        @NotNull PunishmentReasonProvider reasonProvider,
        @NotNull PunishmentAuditProvider auditProvider,
        @NotNull PunishmentIpAddressProvider ipProvider,
        @NotNull PunishmentUserProvider userProvider
) {
    public PunishmentService {
        Objects.requireNonNull(reasonProvider, "reasonProvider must not be null");
        Objects.requireNonNull(auditProvider, "auditProvider must not be null");
        Objects.requireNonNull(ipProvider, "ipProvider must not be null");
        Objects.requireNonNull(userProvider, "userProvider must not be null");
    }

    public int punishedUser(@NotNull UUID mojangId, int reasonId, int executorId) {
        PunishmentReason reason = reasonProvider.findReason(reasonId).orElseThrow(() -> new PunishmentReasonException("Punishment reason not found: " + reasonId));
        PunishmentUser existing = userProvider.findPunishedUser(mojangId, reason.typeId()).orElse(null);
        boolean isUpdate = existing != null;

        PunishmentAudit audit;
        if (isUpdate)
            audit = auditProvider.modify(mojangId, null, reason, executorId).orElse(null);
        else audit = auditProvider.create(mojangId, null, reason, executorId).orElse(null);

        if (audit == null)
            throw new PunishmentAuditException("Failed to create punishment audit for user: " + mojangId + " with reason: " + reasonId);

        PunishmentUser punish = userProvider.upsert(mojangId, reason.typeId(), audit.id(), reason.durationSecs()).orElse(null);
        if (punish == null)
            throw new PunishmentUserException("Failed to create punishment user: " + mojangId + " with reason: " + reasonId);
        return 2; // Note: 1 for log creation, 1 for user creation
    }

    public int punishedIp(@NotNull InetAddress inetAddress, int reasonId, int executorId) {
        PunishmentReason reason = reasonProvider.findReason(reasonId).orElseThrow(() -> new PunishmentReasonException("Punishment reason not found: " + reasonId));
        PunishmentIpAddress existing = ipProvider.findPunishedIpAddress(inetAddress, reason.typeId()).orElse(null);
        boolean isUpdate = existing != null;

        PunishmentAudit audit;
        if (isUpdate)
            audit = auditProvider.modify(null, inetAddress, reason, executorId).orElse(null);
        else audit = auditProvider.create(null, inetAddress, reason, executorId).orElse(null);

        if (audit == null)
            throw new PunishmentAuditException("Failed to create punishment audit for IP: " + inetAddress.getHostAddress() + " with reason: " + reasonId);

        PunishmentIpAddress punish = ipProvider.upsert(inetAddress, reason.typeId(), audit.id(), reason.durationSecs()).orElse(null);
        if (punish == null)
            throw new PunishmentIpAddressException("Failed to create punishment IP: " + inetAddress.getHostAddress() + " with reason: " + reasonId);
        return 2; // Note: 1 for log creation, 1 for IP creation
    }

    public int unpunishedUser(@NotNull UUID mojangId, int typeId, @NotNull UUID auditId, int executorId) {
        PunishmentAudit currentAudit = auditProvider.findAudit(auditId)
                .orElseThrow(() -> new PunishmentAuditException("Punishment audit not found for user: " + mojangId + " with type: " + typeId));

        PunishmentAudit revokeAudit = auditProvider.revoke(mojangId, null, currentAudit, executorId).orElse(null);
        if (revokeAudit == null)
            throw new PunishmentAuditException("Failed to revoke punishment audit for user: " + mojangId + " with type: " + typeId);

        int row = userProvider.delete(mojangId, typeId);
        if (row < 1)
            throw new PunishmentUserException("Failed to unpunished user: " + mojangId + " with type: " + typeId);
        return row;
    }

    public int unpunishedIp(@NotNull InetAddress inetAddress, int typeId, @NotNull UUID auditId, int executorId) {
        PunishmentAudit currentAudit = auditProvider.findAudit(auditId)
                .orElseThrow(() -> new PunishmentAuditException("Punishment audit not found for IP: " + inetAddress.getHostAddress() + " with type: " + typeId));

        PunishmentAudit revokeLog = auditProvider.revoke(null, inetAddress, currentAudit, executorId).orElse(null);
        if (revokeLog == null)
            throw new PunishmentAuditException("Failed to revoke punishment audit for IP: " + inetAddress.getHostAddress() + " with type: " + typeId);

        int row = ipProvider.delete(inetAddress, typeId);
        if (row < 1)
            throw new PunishmentIpAddressException("Failed to unpunished IP: " + inetAddress.getHostAddress() + " with type: " + typeId);
        return row;
    }

    public boolean isPunishedUser(@NotNull UUID mojangId, int typeId) {
        return userProvider.findPunishedUser(mojangId, typeId).isPresent();
    }

    public boolean isPunishedIp(@NotNull InetAddress inetAddress, int typeId) {
        return ipProvider.findPunishedIpAddress(inetAddress, typeId).isPresent();
    }

    public boolean isExpiredUser(@NotNull UUID mojangId, int typeId) {
        PunishmentUser punish = userProvider.findPunishedUser(mojangId, typeId).orElse(null);
        return punish != null && punish.isExpired();
    }

    public boolean isExpiredIp(@NotNull InetAddress inetAddress, int typeId) {
        PunishmentIpAddress punish = ipProvider.findPunishedIpAddress(inetAddress, typeId).orElse(null);
        return punish != null && punish.isExpired();
    }

    public boolean checkUserPunishment(@NotNull UUID mojangId, int typeId, @NotNull Consumer<PunishmentAudit> consumer) {
        Objects.requireNonNull(mojangId, "mojangId must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");

        return userProvider.findPunishedUser(mojangId, typeId)
                .flatMap(punish -> auditProvider.findAudit(punish.logId())
                        .map(audit -> {
                            if (punish.isExpired()) {
                                unpunishedUser(mojangId, typeId, punish.logId(), CONSOLE_USER_ID);
                                return false;
                            }
                            consumer.accept(audit);
                            return true;
                        })
                )
                .orElse(false);
    }

    public boolean checkIpPunishment(@NotNull InetAddress inetAddress, int typeId, @NotNull Consumer<PunishmentAudit> consumer) {
        Objects.requireNonNull(inetAddress, "inetAddress must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");

        return ipProvider.findPunishedIpAddress(inetAddress, typeId)
                .flatMap(punish -> auditProvider.findAudit(punish.logId())
                        .map(audit -> {
                            if (punish.isExpired()) {
                                unpunishedIp(inetAddress, typeId, punish.logId(), CONSOLE_USER_ID);
                                return false;
                            }
                            consumer.accept(audit);
                            return true;
                        })
                )
                .orElse(false);
    }

    public int loadExpired() {
        int users = userProvider.loadExpired();
        int ips = ipProvider.loadExpired();
        return users + ips;
    }
}
