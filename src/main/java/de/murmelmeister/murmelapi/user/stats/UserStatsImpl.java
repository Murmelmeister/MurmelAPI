package de.murmelmeister.murmelapi.user.stats;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;

record UserStatsImpl(
        int userId,
        long playTime,
        int dailyStreak,
        @Nullable LocalDateTime lastSeenAt,
        boolean isOnline
) implements UserStats {
    public UserStatsImpl {
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (playTime < 0) throw new IllegalArgumentException("playTime must be >= 0");
        if (dailyStreak < 0) throw new IllegalArgumentException("dailyStreak must be >= 0");
    }

    @Override
    public long currentPlayTime() {
        if (!isOnline || lastSeenAt == null) return playTime;

        long additionalSeconds = Duration.between(lastSeenAt, LocalDateTime.now()).getSeconds();
        if (additionalSeconds <= 0) return playTime;
        return playTime + additionalSeconds;
    }
}
