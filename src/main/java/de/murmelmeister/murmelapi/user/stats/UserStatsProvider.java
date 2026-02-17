package de.murmelmeister.murmelapi.user.stats;

import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface UserStatsProvider {
    void refreshCache();

    @Nullable UserStats findByUserId(int userId);

    @Nullable UserStats create(int userId);

    int delete(int userId);

    @Nullable UserStats update(int userId, int playTime, int dailyStreak, @Nullable LocalDate lastDay, @Nullable LocalDateTime lastSeen);
}
