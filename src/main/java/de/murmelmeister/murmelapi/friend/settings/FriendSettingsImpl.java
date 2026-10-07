package de.murmelmeister.murmelapi.friend.settings;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

record FriendSettingsImpl(
        int userId,
        boolean allowRequests,
        boolean showServer,
        boolean allowFollow,
        boolean loginNotify
) implements FriendSettings {
    public FriendSettingsImpl {
        if (userId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userId must be greater than " + CONSOLE_USER_ID);
    }
}
