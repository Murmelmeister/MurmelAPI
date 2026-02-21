package de.murmelmeister.murmelapi.user.stats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserStats(
        int userId,
        int playTime,
        int dailyStreak,
        @Nullable LocalDate dailyStreakLastDay,
        @Nullable LocalDateTime lastSeenAt
) {
    public UserStats {
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (playTime < 0) throw new IllegalArgumentException("playTime must be >= 0");
        if (dailyStreak < 0) throw new IllegalArgumentException("dailyStreak must be >= 0");
    }

    public static @NotNull Builder builder(@NotNull UserStats userStats) {
        return new Builder(userStats);
    }

    public static class Builder {
        private final int userId;

        private int playTime;
        private int dailyStreak;
        private LocalDate dailyStreakLastDay;
        private LocalDateTime lastSeenAt;

        private Builder(@NotNull UserStats userStats) {
            this.userId = userStats.userId();
            this.playTime = userStats.playTime();
            this.dailyStreak = userStats.dailyStreak();
            this.dailyStreakLastDay = userStats.dailyStreakLastDay();
            this.lastSeenAt = userStats.lastSeenAt();
        }

        public Builder playTime(int playTime) {
            this.playTime = playTime;
            return this;
        }

        public Builder dailyStreak(int dailyStreak) {
            this.dailyStreak = dailyStreak;
            return this;
        }

        public Builder dailyStreakLastDay(@Nullable LocalDate dailyStreakLastDay) {
            this.dailyStreakLastDay = dailyStreakLastDay;
            return this;
        }

        public Builder lastSeenAt(@Nullable LocalDateTime lastSeenAt) {
            this.lastSeenAt = lastSeenAt;
            return this;
        }

        public @NotNull UserStats build() {
            return new UserStats(userId, playTime, dailyStreak, dailyStreakLastDay, lastSeenAt);
        }
    }
}
