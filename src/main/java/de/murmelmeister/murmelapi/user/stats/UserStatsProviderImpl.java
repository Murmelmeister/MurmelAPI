package de.murmelmeister.murmelapi.user.stats;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.user.UserStatsException;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

final class UserStatsProviderImpl implements UserStatsProvider {
    private static final String TABLE_NAME = "user_stats";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (id)
            VALUES (?)
            RETURNING id, play_time, daily_streak, daily_streak_last_day, last_seen_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s SET
                play_time = ?,
                daily_streak = ?,
                daily_streak_last_day = ?,
                last_seen_at = ?
            WHERE id = ?
            """.formatted(TABLE_NAME);

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
    public @NotNull Optional<UserStats> findByUserId(int userId) {
        return cache.getById(userId);
    }

    @Override
    public @NotNull Optional<UserStats> create(int userId) {
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");

        UserStats userStats = MurmelExceptionWrapper.dbWrap(
                "Failed to create UserStats (userId=" + userId + ")",
                () -> database.query(CREATE_SQL, null, UserStatsRowMapper::resultSet,
                        stmt -> stmt.setInt(1, userId)),
                UserStatsException::new
        );

        if (userStats == null) return Optional.empty();
        refreshProvider.fireSingle(single, new UserStatsCache.StatsKey(userStats.userId()));
        return Optional.of(userStats);
    }

    @Override
    public int delete(int userId) {
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");

        Optional<UserStats> existingOpt = cache.getById(userId);
        if (existingOpt.isEmpty()) return 0;
        UserStats existing = existingOpt.get();

        int rows = MurmelExceptionWrapper.dbWrap(
                "Failed to delete UserStats (userId=" + userId + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setInt(1, userId)),
                UserStatsException::new
        );

        if (rows != 1) return 0;
        refreshProvider.fireSingle(single, new UserStatsCache.StatsKey(existing.userId()));
        return rows;
    }

    @Override
    public @NotNull Optional<UserStats> update(int userId, int playTime, int dailyStreak, @Nullable LocalDate lastDay, @Nullable LocalDateTime lastSeen) {
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (playTime < 0) throw new IllegalArgumentException("playTime must be >= 0");
        if (dailyStreak < 0) throw new IllegalArgumentException("dailyStreak must be >= 0");

        Optional<UserStats> existingOpt = cache.getById(userId);
        if (existingOpt.isEmpty()) return Optional.empty();
        UserStats existing = existingOpt.get();

        if (Objects.equals(existing.dailyStreakLastDay(), lastDay)
                && Objects.equals(existing.lastSeenAt(), lastSeen)
                && existing.playTime() == playTime
                && existing.dailyStreak() == dailyStreak)
            return existingOpt;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update UserStats (userId=" + userId + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setInt(1, playTime);
                    stmt.setInt(2, dailyStreak);
                    stmt.setObject(3, lastDay, Types.DATE);
                    stmt.setObject(4, lastSeen, Types.TIMESTAMP);
                    stmt.setInt(5, userId);
                }),
                UserStatsException::new
        );
        if (row != 1) return Optional.empty();

        UserStats updated = existing.builder()
                .playTime(playTime)
                .dailyStreak(dailyStreak)
                .dailyStreakLastDay(lastDay)
                .lastSeenAt(lastSeen)
                .build();
        refreshProvider.fireSingle(single, new UserStatsCache.StatsKey(updated.userId()));
        return Optional.of(updated);
    }
}
