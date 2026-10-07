package de.murmelmeister.murmelapi.friend.settings;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface FriendSettings {
    int userId();

    boolean allowRequests();

    boolean showServer();

    boolean allowFollow();

    boolean loginNotify();

    static @NotNull FriendSettings of(int userId, boolean allowRequests, boolean showServer, boolean allowFollow, boolean loginNotify) {
        return new FriendSettingsImpl(userId, allowRequests, showServer, allowFollow, loginNotify);
    }

    @ApiStatus.Internal
    static FriendSettings resultSet(@NotNull ResultSet resultSet) throws SQLException {
        return FriendSettingsRowMapper.resultSet(resultSet);
    }
}
