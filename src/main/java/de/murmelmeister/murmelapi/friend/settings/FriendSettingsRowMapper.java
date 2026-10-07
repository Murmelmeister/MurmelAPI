package de.murmelmeister.murmelapi.friend.settings;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

class FriendSettingsRowMapper {
    static FriendSettings resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int userId = resultSet.getInt("user_id");
        boolean allowRequests = resultSet.getBoolean("allow_requests");
        boolean showServer = resultSet.getBoolean("show_server");
        boolean allowFollow = resultSet.getBoolean("allow_follow");
        boolean loginNotify = resultSet.getBoolean("login_notify");
        return new FriendSettingsImpl(userId, allowRequests, showServer, allowFollow, loginNotify);
    }
}
