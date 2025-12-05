package de.murmelmeister.murmelapi.punishment.reason;

import java.util.List;

public interface PunishmentReasonProvider {
    void refreshCache();

    PunishmentReason getReason(int id);

    List<PunishmentReason> getReasonsByType(int typeId);

    List<PunishmentReason> getAllReasons();

    PunishmentReason create(int id, int typeId, String reasonText, Long durationSecs, boolean autoFlagIp, boolean autoPunish, int createdBy);

    int delete(int id);

    PunishmentReason update(int id, int typeId, String reasonText, Long durationSecs, boolean autoFlagIp, boolean autoPunish, int changedBy);
}
