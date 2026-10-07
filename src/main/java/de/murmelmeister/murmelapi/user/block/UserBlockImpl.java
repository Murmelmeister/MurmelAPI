package de.murmelmeister.murmelapi.user.block;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

record UserBlockImpl(
        int userId,
        int blockedId,
        @NotNull LocalDateTime createdAt
) implements UserBlock {
    public UserBlockImpl {
        if (userId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userId must be greater than " + CONSOLE_USER_ID);
        if (blockedId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("blockedId must be greater than " + CONSOLE_USER_ID);
        if (userId == blockedId)
            throw new IllegalArgumentException("userId '" + userId + "' and blockedId '" + blockedId + "'  must be different");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
