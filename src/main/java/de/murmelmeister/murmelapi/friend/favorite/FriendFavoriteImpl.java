package de.murmelmeister.murmelapi.friend.favorite;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

record FriendFavoriteImpl(
        int ownerId,
        int friendId,
        @NotNull LocalDateTime createdAt
) implements FriendFavorite {
    public FriendFavoriteImpl {
        if (ownerId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("ownerId must be greater than " + CONSOLE_USER_ID);
        if (friendId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("friendId must be greater than " + CONSOLE_USER_ID);
        if (ownerId == friendId)
            throw new IllegalArgumentException("ownerId '" + ownerId + "' and friendId '" + friendId + "' must be different");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
