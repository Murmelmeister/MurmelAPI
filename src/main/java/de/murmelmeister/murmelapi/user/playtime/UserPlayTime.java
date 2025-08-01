package de.murmelmeister.murmelapi.user.playtime;

import java.time.LocalDate;
import java.util.Objects;

public class UserPlayTime {
    private final int userId;
    private int playTime;
    private int loginCount;
    private LocalDate lastSeenDate;

    public UserPlayTime(int userId, int playTime, int loginCount) {
        this.userId = userId;
        this.playTime = playTime;
        this.loginCount = loginCount;
        this.lastSeenDate = null;
    }

    public UserPlayTime(UserPlayTime userPlayTime) {
        this.userId = userPlayTime.userId;
        this.playTime = userPlayTime.playTime;
        this.loginCount = userPlayTime.loginCount;
        this.lastSeenDate = userPlayTime.lastSeenDate;
    }

    public int getUserId() {
        return userId;
    }

    public int getPlayTime() {
        return playTime;
    }

    public void setPlayTime(int playTime) {
        this.playTime = playTime;
    }

    public int getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }

    public LocalDate getLastSeenDate() {
        return lastSeenDate;
    }

    public void setLastSeenDate(LocalDate lastSeenDate) {
        this.lastSeenDate = lastSeenDate;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UserPlayTime userPlayTime)) return false;
        return userId == userPlayTime.userId && playTime == userPlayTime.playTime && loginCount == userPlayTime.loginCount && Objects.equals(lastSeenDate, userPlayTime.lastSeenDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, playTime, loginCount, lastSeenDate);
    }

    @Override
    public String toString() {
        return "UserPlayTime{" +
               "userId=" + userId +
               ", playTime=" + playTime +
               ", loginCount=" + loginCount +
               ", lastSeenDate=" + lastSeenDate +
               '}';
    }
}
