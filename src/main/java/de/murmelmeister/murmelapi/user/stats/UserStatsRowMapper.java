package de.murmelmeister.murmelapi.user.stats;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

final class UserStatsRowMapper {
    static UserStats resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int userId = resultSet.getInt("id");
        int playTime = resultSet.getInt("play_time");
        int dailyStreak = resultSet.getInt("daily_streak");
        LocalDate lastDay = resultSet.getObject("daily_streak_last_day", LocalDate.class);
        LocalDateTime lastSeen = resultSet.getObject("last_seen_at", LocalDateTime.class);
        return new UserStatsImpl(userId, playTime, dailyStreak, lastDay, lastSeen);
    }
}
