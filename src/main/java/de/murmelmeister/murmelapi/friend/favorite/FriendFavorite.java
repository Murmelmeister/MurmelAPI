package de.murmelmeister.murmelapi.friend.favorite;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public interface FriendFavorite {
    int ownerId();

    int friendId();

    @NotNull LocalDateTime createdAt();

    static @NotNull FriendFavorite of(int ownerId, int friendId, @NotNull LocalDateTime createdAt) {
        return new FriendFavoriteImpl(ownerId, friendId, createdAt);
    }

    @ApiStatus.Internal
    static FriendFavorite resultSet(@NotNull ResultSet resultSet) throws SQLException {
        return FriendFavoriteRowMapper.resultSet(resultSet);
    }
}
