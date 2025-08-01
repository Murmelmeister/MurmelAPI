package de.murmelmeister.murmelapi.punishment.reason;

import java.time.LocalDateTime;

/**
 * @param durationSecs Duration in seconds, null for permanent reasons
 */
public record PunishmentReason(int id, int typeId, String reasonText, Long durationSecs, boolean autoFlagIp,
                               boolean autoPunish, int createdBy, LocalDateTime createdAt, Integer changedBy,
                               LocalDateTime changedAt) {

    public boolean isPermanent() {
        return durationSecs == null;
    }

    public PunishmentReason withUpdateMeta(Integer typeId, String reasonText, Long durationSecs,
                                           Boolean autoFlagIp, Boolean autoPunish, Integer changedBy, LocalDateTime changedAt) {
        return new PunishmentReason(id,
                typeId != null ? typeId : this.typeId,
                reasonText != null ? reasonText : this.reasonText,
                durationSecs != null ? durationSecs : this.durationSecs,
                autoFlagIp != null ? autoFlagIp : this.autoFlagIp,
                autoPunish != null ? autoPunish : this.autoPunish,
                createdBy, createdAt,
                changedBy != null ? changedBy : this.changedBy,
                changedAt != null ? changedAt : this.changedAt);
    }
}
