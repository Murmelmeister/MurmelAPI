package de.murmelmeister.murmelapi.friend.ship;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

final class FriendshipRowMapper {
    static Friendship resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int userOne = resultSet.getInt("user_one");
        int userTwo = resultSet.getInt("user_two");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        return new FriendshipImpl(userOne, userTwo, createdAt);
    }
}
