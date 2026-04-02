package de.murmelmeister.murmelapi.maintenance;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

final class MaintenanceRowMapper {
    static Maintenance resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String title = resultSet.getString("title");
        String reason = resultSet.getString("reason");
        MaintenanceType status = MaintenanceType.valueOf(resultSet.getString("status"));
        LocalDateTime startAt = resultSet.getTimestamp("start_at").toLocalDateTime();
        LocalDateTime endAt = resultSet.getTimestamp("end_at").toLocalDateTime();
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        int createdBy = resultSet.getInt("created_by");
        LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
        Integer changedBy = resultSet.getObject("changed_by", Integer.class);
        return new MaintenanceImpl(id, title, reason, status, startAt, endAt, createdAt, createdBy, changedAt, changedBy);
    }
}
