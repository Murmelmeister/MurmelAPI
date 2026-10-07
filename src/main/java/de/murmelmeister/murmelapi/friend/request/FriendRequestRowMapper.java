package de.murmelmeister.murmelapi.friend.request;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

class FriendRequestRowMapper {
    static FriendRequest resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int senderId = resultSet.getInt("sender_id");
        int receiverId = resultSet.getInt("receiver_id");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        LocalDateTime expiresAt = resultSet.getTimestamp("expires_at").toLocalDateTime();
        return new FriendRequestImpl(senderId, receiverId, createdAt, expiresAt);
    }
}
