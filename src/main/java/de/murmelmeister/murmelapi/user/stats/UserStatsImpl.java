package de.murmelmeister.murmelapi.user.stats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Consumer;

record UserStatsImpl(
        int userId,
        int playTime,
        int dailyStreak,
        @Nullable LocalDate dailyStreakLastDay,
        @Nullable LocalDateTime lastSeenAt
) implements UserStats {

    public UserStatsImpl {
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (playTime < 0) throw new IllegalArgumentException("playTime must be >= 0");
        if (dailyStreak < 0) throw new IllegalArgumentException("dailyStreak must be >= 0");
    }

    public @NotNull UserStats.Builder builder() {
        return new Builder(this);
    }

    public @NotNull UserStats with(@NotNull Consumer<UserStats.Builder> consumer) {
        UserStats.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements UserStats.Builder {
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

        public @NotNull UserStats.Builder playTime(int playTime) {
            this.playTime = playTime;
            return this;
        }

        public @NotNull UserStats.Builder dailyStreak(int dailyStreak) {
            this.dailyStreak = dailyStreak;
            return this;
        }

        public @NotNull UserStats.Builder dailyStreakLastDay(@Nullable LocalDate dailyStreakLastDay) {
            this.dailyStreakLastDay = dailyStreakLastDay;
            return this;
        }

        public @NotNull UserStats.Builder lastSeenAt(@Nullable LocalDateTime lastSeenAt) {
            this.lastSeenAt = lastSeenAt;
            return this;
        }

        public @NotNull UserStats build() {
            return new UserStatsImpl(userId, playTime, dailyStreak, dailyStreakLastDay, lastSeenAt);
        }
    }
}
