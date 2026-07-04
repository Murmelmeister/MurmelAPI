package de.murmelmeister.murmelapi.user.stats;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

final class UserStatsRowMapper {
    static UserStats resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int userId = resultSet.getInt("user_id");
        int playTime = resultSet.getInt("play_time");
        int dailyStreak = resultSet.getInt("login_streak");
        LocalDateTime lastSeen = resultSet.getObject("last_seen_at", LocalDateTime.class);
        boolean online = resultSet.getBoolean("online");
        return new UserStatsImpl(userId, playTime, dailyStreak, lastSeen, online);
    }
}
