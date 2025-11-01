package de.murmelmeister.murmelapi.user.playtime;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class UserPlayTimeProviderImpl implements UserPlayTimeProvider {
    private static final String TABLE_NAME = "user_playtime";

    private final Database database;
    private final UserPlayTimeCache cache;
    private final RefreshType all = RefreshType.USER_PLAY_TIMES;
    private final RefreshType single = RefreshType.SINGLE_USER_PLAY_TIME;

    public UserPlayTimeProviderImpl(Database database, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.cache = new UserPlayTimeCache(database, TABLE_NAME, fetchLimit, cacheCapcity, refreshInterval);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY, " +
                                         "play_time INT NOT NULL DEFAULT 0, " +
                                         "login_count INT NOT NULL DEFAULT 0, " +
                                         "FOREIGN KEY (id) REFERENCES users(id)"
        );
    }

    @Override
    public void closeCache() {
        cache.close();
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public UserPlayTime findByUserId(int userId) {
        UserPlayTime original = cache.get(userId);
        return (original != null) ? new UserPlayTime(original) : null;
    }

    @Override
    public List<UserPlayTime> getAllPlayTimes() {
        return cache.getCachedPlayTimes().stream()
                .map(UserPlayTime::new) // Note: Create a new instance to avoid exposing the cache directly
                .toList();
    }

    @Override
    public UserPlayTime create(int userId) {
        if (userId < 1) return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (id) VALUES (?)";
        int row = database.update(sql,
                stmt -> stmt.setInt(1, userId));
        if (row < 1) return null;

        UserPlayTime newPlayTime = new UserPlayTime(userId, 0, 1);
        RefreshUtil.fireSingle(single, userId);
        cache.put(newPlayTime);
        return newPlayTime;

    }

    @Override
    public int delete(int userId) {
        if (userId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql,
                stmt -> stmt.setInt(1, userId));
        if (row < 1) return 0;

        cache.remove(userId);
        RefreshUtil.fireSingle(single, userId);
        return row;
    }

    @Override
    public UserPlayTime update(UserPlayTime playTime) {
        if (playTime == null || playTime.getUserId() < 1 || playTime.getPlayTime() < 0 || playTime.getLoginCount() < 0)
            return null;

        UserPlayTime existing = cache.get(playTime.getUserId());
        if (existing == null) return null;

        if (Objects.equals(playTime, existing))
            return existing; // No changes, return existing playtime

        String sql = "UPDATE " + TABLE_NAME + " SET play_time = ?, login_count = ? WHERE id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, playTime.getPlayTime());
            stmt.setInt(2, playTime.getLoginCount());
            stmt.setInt(3, playTime.getUserId());
        });
        if (row < 1) return null;

        RefreshUtil.fireSingle(single, playTime.getUserId());
        cache.put(playTime);
        return playTime;
    }

    @Override
    public boolean updateOnlyCache(UserPlayTime playTime) {
        if (playTime == null || playTime.getUserId() < 1 || playTime.getPlayTime() < 0 || playTime.getLoginCount() < 0)
            return false;

        UserPlayTime existing = cache.get(playTime.getUserId());
        if (existing == null) return false;

        if (Objects.equals(playTime, existing))
            return true; // No changes, return existing playtime

        RefreshUtil.fireSingle(single, playTime.getUserId());
        cache.put(playTime);
        return true;
    }

    @Override
    public void incrementPlayTime(UserPlayTime playTime) {
        if (playTime == null || playTime.getUserId() < 1 || playTime.getPlayTime() < 0 || playTime.getLoginCount() < 0)
            return;

        int currentPlayTime = playTime.getPlayTime();
        ++currentPlayTime;

        String sql = "UPDATE " + TABLE_NAME + " SET play_time = ? WHERE id = ?";
        int finalCurrentPlayTime = currentPlayTime;
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, finalCurrentPlayTime);
            stmt.setInt(2, playTime.getUserId());
        });
        if (row < 1) return; // No rows updated, exit early

        playTime.setPlayTime(currentPlayTime);
        RefreshUtil.fireSingle(single, playTime.getUserId());
        cache.put(playTime);
    }
}
