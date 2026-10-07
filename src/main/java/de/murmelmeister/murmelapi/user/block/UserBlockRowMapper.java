package de.murmelmeister.murmelapi.user.block;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

class UserBlockRowMapper {
    static UserBlock resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int userId = resultSet.getInt("user_id");
        int blockedId = resultSet.getInt("blocked_id");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        return new UserBlockImpl(userId, blockedId, createdAt);
    }
}
