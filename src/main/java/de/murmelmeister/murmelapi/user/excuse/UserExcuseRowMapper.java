package de.murmelmeister.murmelapi.user.excuse;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

final class UserExcuseRowMapper {
    static UserExcuse resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        int userId = resultSet.getInt("user_id");
        LocalDate startDate = resultSet.getObject("start_date", LocalDate.class);
        int extraDays = resultSet.getInt("extra_days");
        String reason = resultSet.getString("reason");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        int createdBy = resultSet.getInt("created_by");
        LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
        Integer changedBy = resultSet.getObject("changed_by", Integer.class);
        return new UserExcuseImpl(id, userId, startDate, extraDays, reason, createdAt, createdBy, changedAt, changedBy);
    }
}
