package de.murmelmeister.murmelapi.user.playtime;

import java.util.List;

public interface UserPlayTimeProvider {
    void closeCache();

    void refreshCache();

    UserPlayTime findByUserId(int userId);

    List<UserPlayTime> getAllPlayTimes();

    UserPlayTime create(int userId);

    int delete(int userId);

    UserPlayTime update(UserPlayTime playTime);

    boolean updateOnlyCache(UserPlayTime playTime);

    void incrementPlayTime(UserPlayTime playTime);
}
