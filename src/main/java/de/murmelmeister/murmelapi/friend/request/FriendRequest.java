package de.murmelmeister.murmelapi.friend.request;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public interface FriendRequest {
    int senderId();

    int receiverId();

    @NotNull LocalDateTime createdAt();

    @NotNull LocalDateTime expiresAt();

    boolean isExpired();

    static @NotNull FriendRequest of(int senderId, int receiverId, @NotNull LocalDateTime createdAt, @NotNull LocalDateTime expiresAt) {
        return new FriendRequestImpl(senderId, receiverId, createdAt, expiresAt);
    }

    @ApiStatus.Internal
    static FriendRequest resultSet(@NotNull ResultSet resultSet) throws SQLException {
        return FriendRequestRowMapper.resultSet(resultSet);
    }
}
