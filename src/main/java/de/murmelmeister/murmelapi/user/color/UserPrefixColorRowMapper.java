package de.murmelmeister.murmelapi.user.color;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

final class UserPrefixColorRowMapper {
    static UserPrefixColor resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int userId = resultSet.getInt("user_id");
        String colorId = resultSet.getString("color_id");
        boolean active = resultSet.getBoolean("active");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        return new UserPrefixColorImpl(userId, colorId, active, createdAt);
    }
}
