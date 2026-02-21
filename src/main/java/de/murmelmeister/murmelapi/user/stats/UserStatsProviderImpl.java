package de.murmelmeister.murmelapi.user.stats;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public final class UserStatsProviderImpl implements UserStatsProvider {
    private static final String TABLE_NAME = "user_stats";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final UserStatsCache cache;
    private final RefreshType all = RefreshType.USER_STATS;
    private final RefreshType single = RefreshType.SINGLE_USER_STAT;

    public UserStatsProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new UserStatsCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable UserStats findByUserId(int userId) {
        return cache.getById(userId);
    }

    @Override
    public @Nullable UserStats create(int userId) {
        if (userId < 1) return null;

        @Language("MariaDB")
        String sql = """
                INSERT INTO %s (id)
                VALUES (?)
                RETURNING id, play_time, daily_streak, daily_streak_last_day, last_seen_at
                """.formatted(TABLE_NAME);
        UserStats userStats = database.query(sql, null, ResultSetUtil.userStats(),
                stmt -> stmt.setInt(1, userId));

        if (userStats == null) return null;
        refreshProvider.fireSingle(single, userStats);
        return userStats;
    }

    @Override
    public int delete(int userId) {
        if (userId < 1) return 0;

        UserStats existing = cache.getById(userId);
        if (existing == null) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);
        int rows = database.update(sql, stmt -> stmt.setInt(1, userId));
        if (rows != 1) return 0;

        refreshProvider.fireSingle(single, existing);
        return rows;
    }

    @Override
    public @Nullable UserStats update(int userId, int playTime, int dailyStreak, @Nullable LocalDate lastDay, @Nullable LocalDateTime lastSeen) {
        if (userId < 1 || playTime < 0 || dailyStreak < 0) return null;

        UserStats existing = cache.getById(userId);
        if (existing == null) return null;

        if (Objects.equals(existing.dailyStreakLastDay(), lastDay)
                && Objects.equals(existing.lastSeenAt(), lastSeen)
                && existing.playTime() == playTime
                && existing.dailyStreak() == dailyStreak)
            return existing; // No update needed

        @Language("MariaDB")
        String sql = """
                UPDATE %s SET
                    play_time = ?,
                    daily_streak = ?,
                    daily_streak_last_day = ?,
                    last_seen_at = ?
                WHERE id = ?
                """.formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, playTime);
            stmt.setInt(2, dailyStreak);
            stmt.setObject(3, lastDay, Types.DATE);
            stmt.setObject(4, lastSeen, Types.TIMESTAMP);
            stmt.setInt(5, userId);
        });
        if (row != 1) return null;

        UserStats updated = UserStats.builder(existing)
                .playTime(playTime)
                .dailyStreak(dailyStreak)
                .dailyStreakLastDay(lastDay)
                .lastSeenAt(lastSeen)
                .build();
        refreshProvider.fireSingle(single, updated);
        return updated;
    }
}
