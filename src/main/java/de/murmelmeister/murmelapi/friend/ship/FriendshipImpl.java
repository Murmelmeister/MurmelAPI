package de.murmelmeister.murmelapi.friend.ship;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

record FriendshipImpl(
        int userOne,
        int userTwo,
        @NotNull LocalDateTime createdAt
) implements Friendship {
    public FriendshipImpl {
        if (userOne < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userOne must be greater than " + CONSOLE_USER_ID);
        if (userTwo < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userTwo must be greater than " + CONSOLE_USER_ID);
        if (userOne == userTwo)
            throw new IllegalArgumentException("userOne '" + userOne + "' and userTwo '" + userTwo + "' must be different");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
