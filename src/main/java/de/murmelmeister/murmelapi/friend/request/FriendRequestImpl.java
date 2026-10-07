package de.murmelmeister.murmelapi.friend.request;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

record FriendRequestImpl(
        int senderId,
        int receiverId,
        @NotNull LocalDateTime createdAt,
        @NotNull LocalDateTime expiresAt
) implements FriendRequest {
    public FriendRequestImpl {
        if (senderId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("senderId must be greater than " + CONSOLE_USER_ID);
        if (receiverId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("receiverId must be greater than " + CONSOLE_USER_ID);
        if (senderId == receiverId)
            throw new IllegalArgumentException("senderId '" + senderId + "' and receiverId '" + receiverId + "' must be different");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (expiresAt.isBefore(createdAt))
            throw new IllegalArgumentException("expiresAt '" + expiresAt + "' must be before createdAt " + createdAt);
    }

    @Override
    public boolean isExpired() {
        return expiresAt().isBefore(LocalDateTime.now());
    }
}
