package de.murmelmeister.murmelapi.user.stats;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserStats(int userId, int playTime, int dailyStreak, LocalDate dailyStreakLastDay,
                        LocalDateTime lastSeenAt) {
}
