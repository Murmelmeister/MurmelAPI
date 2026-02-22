package de.murmelmeister.murmelapi.participant;

import org.jetbrains.annotations.Nullable;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public record Participant(
        int id,
        @Nullable Integer groupId,
        @Nullable Integer userId
) {
    public Participant {
        if (userId != null && userId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userId must be null or >= " + CONSOLE_USER_ID);
    }
}
