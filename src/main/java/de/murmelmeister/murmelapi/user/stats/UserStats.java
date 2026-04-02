package de.murmelmeister.murmelapi.user.stats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public interface UserStats {
    int userId();

    int playTime();

    int dailyStreak();

    @Nullable LocalDate dailyStreakLastDay();

    @Nullable LocalDateTime lastSeenAt();

    @NotNull Builder builder();

    @NotNull UserStats with(@NotNull Consumer<Builder> consumer);

    static @NotNull UserStats of(int userId, int playTime, int dailyStreak, @Nullable LocalDate dailyStreakLastDay, @Nullable LocalDateTime lastSeenAt) {
        return new UserStatsImpl(userId, playTime, dailyStreak, dailyStreakLastDay, lastSeenAt);
    }

    interface Builder {
        @NotNull Builder playTime(int playTime);

        @NotNull Builder dailyStreak(int dailyStreak);

        @NotNull Builder dailyStreakLastDay(@Nullable LocalDate dailyStreakLastDay);

        @NotNull Builder lastSeenAt(@Nullable LocalDateTime lastSeenAt);

        @NotNull UserStats build();
    }
}
