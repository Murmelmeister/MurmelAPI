package de.murmelmeister.murmelapi.friend.ship;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public interface Friendship {
    int userOne();

    int userTwo();

    @NotNull LocalDateTime createdAt();

    static @NotNull Friendship of(int userOne, int userTwo, @NotNull LocalDateTime createdAt) {
        return new FriendshipImpl(userOne, userTwo, createdAt);
    }

    @ApiStatus.Internal
    static Friendship resultSet(@NotNull ResultSet resultSet) throws SQLException {
        return FriendshipRowMapper.resultSet(resultSet);
    }
}
