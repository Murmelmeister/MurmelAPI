package de.murmelmeister.murmelapi.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record User(
        int id,
        @Nullable UUID mojangId,
        @NotNull String username,
        @Nullable LocalDateTime firstLogin,
        boolean systemUser,
        boolean debugUser,
        boolean debugEnabled,
        int languageId
) {
    public User {
        Objects.requireNonNull(username, "username cannot be null");
    }

    public boolean debugMode() {
        return debugUser && debugEnabled;
    }

    public static @NotNull Builder builder(User user) {
        return new Builder(user);
    }

    public static class Builder {
        private final int id;
        private final UUID mojangId;
        private final boolean systemUser;

        private String username;
        private LocalDateTime firstLogin;
        private boolean debugUser;
        private boolean debugEnabled;
        private int languageId;

        private Builder(User user) {
            this.id = user.id();
            this.mojangId = user.mojangId();
            this.systemUser = user.systemUser();
            this.username = user.username();
            this.firstLogin = user.firstLogin();
            this.debugUser = user.debugUser();
            this.debugEnabled = user.debugEnabled();
            this.languageId = user.languageId();
        }

        public Builder username(@NotNull String username) {
            this.username = username;
            return this;
        }

        public Builder firstLogin(@Nullable LocalDateTime firstLogin) {
            this.firstLogin = firstLogin;
            return this;
        }

        public Builder debugUser(boolean debugUser) {
            this.debugUser = debugUser;
            return this;
        }

        public Builder debugEnabled(boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
            return this;
        }

        public Builder languageId(int languageId) {
            this.languageId = languageId;
            return this;
        }

        public @NotNull User build() {
            return new User(id, mojangId, username, firstLogin, systemUser, debugUser, debugEnabled, languageId);
        }
    }
}
