package de.murmelmeister.murmelapi.maintenance.whitelist;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

final class MaintenanceWhitelistRowMapper {
    static MaintenanceWhitelist resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        int maintenanceId = resultSet.getInt("maintenance_id");
        int userId = resultSet.getInt("user_id");
        LocalDateTime startAt = resultSet.getObject("start_at", LocalDateTime.class);
        LocalDateTime endAt = resultSet.getObject("end_at", LocalDateTime.class);
        String note = resultSet.getString("note");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        int createdBy = resultSet.getInt("created_by");
        LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
        Integer changedBy = resultSet.getObject("changed_by", Integer.class);
        return new MaintenanceWhitelistImpl(id, maintenanceId, userId, startAt, endAt, note, createdAt, createdBy, changedAt, changedBy);
    }
}
