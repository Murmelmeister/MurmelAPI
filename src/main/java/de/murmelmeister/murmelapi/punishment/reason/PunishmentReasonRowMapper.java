package de.murmelmeister.murmelapi.punishment.reason;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

final class PunishmentReasonRowMapper {
    static PunishmentReason resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        int typeId = resultSet.getInt("type_id");
        String reasonText = resultSet.getString("reason_text");
        Long durationSecs = resultSet.getObject("duration_secs", Long.class);
        boolean autoFlagIp = resultSet.getBoolean("auto_flag_ip");
        int createdBy = resultSet.getInt("created_by");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        Integer changedBy = resultSet.getObject("changed_by", Integer.class);
        LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
        return new PunishmentReasonImpl(id, typeId, reasonText, durationSecs, autoFlagIp,
                createdBy, createdAt, changedBy, changedAt);
    }
}
