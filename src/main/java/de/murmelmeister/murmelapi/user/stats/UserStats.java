package de.murmelmeister.murmelapi.user.stats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public interface UserStats {
    int userId();

    int playTime();

    int dailyStreak();

    @Nullable LocalDateTime lastSeenAt();

    boolean isOnline();

    long currentPlayTime();

    static @NotNull UserStats of(int userId, int playTime, int dailyStreak, @Nullable LocalDateTime lastSeenAt, boolean isOnline) {
        return new UserStatsImpl(userId, playTime, dailyStreak, lastSeenAt, isOnline);
    }
}
