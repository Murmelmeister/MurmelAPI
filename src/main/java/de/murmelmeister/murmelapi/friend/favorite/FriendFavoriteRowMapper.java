package de.murmelmeister.murmelapi.friend.favorite;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

class FriendFavoriteRowMapper {
    static FriendFavorite resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int ownerId = resultSet.getInt("owner_id");
        int friendId = resultSet.getInt("friend_id");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        return new FriendFavoriteImpl(ownerId, friendId, createdAt);
    }
}
