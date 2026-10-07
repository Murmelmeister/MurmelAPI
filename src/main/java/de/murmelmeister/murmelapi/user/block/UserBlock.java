package de.murmelmeister.murmelapi.user.block;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public interface UserBlock {
    int userId();

    int blockedId();

    @NotNull LocalDateTime createdAt();

    static @NotNull UserBlock of(int userId, int blockedId, @NotNull LocalDateTime createdAt) {
        return new UserBlockImpl(userId, blockedId, createdAt);
    }

    @ApiStatus.Internal
    static UserBlock resultSet(@NotNull ResultSet resultSet) throws SQLException {
        return UserBlockRowMapper.resultSet(resultSet);
    }
}
