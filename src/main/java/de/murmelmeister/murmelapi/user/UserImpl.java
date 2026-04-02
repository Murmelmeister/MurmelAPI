package de.murmelmeister.murmelapi.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

record UserImpl(
        int id,
        @NotNull UUID mojangId,
        @NotNull String username,
        @Nullable LocalDateTime firstLogin,
        boolean systemUser,
        boolean debugUser,
        boolean debugEnabled,
        int languageId
) implements User {

    public UserImpl {
        Objects.requireNonNull(mojangId, "mojangId cannot be null");
        Objects.requireNonNull(username, "username cannot be null");
        if (username.length() > 16)
            throw new IllegalArgumentException("username cannot be longer than 16 characters");
        if (languageId < 1) throw new IllegalArgumentException("languageId must be >= 1");
    }

    public boolean debugMode() {
        return debugUser && debugEnabled;
    }

    public @NotNull User.Builder builder() {
        return new Builder(this);
    }

    public @NotNull User with(@NotNull Consumer<User.Builder> consumer) {
        User.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements User.Builder {
        private final int id;
        private final UUID mojangId;
        private final boolean systemUser;

        private String username;
        private LocalDateTime firstLogin;
        private boolean debugUser;
        private boolean debugEnabled;
        private int languageId;

        private Builder(@NotNull User user) {
            this.id = user.id();
            this.mojangId = user.mojangId();
            this.systemUser = user.systemUser();
            this.username = user.username();
            this.firstLogin = user.firstLogin();
            this.debugUser = user.debugUser();
            this.debugEnabled = user.debugEnabled();
            this.languageId = user.languageId();
        }

        public @NotNull User.Builder username(@NotNull String username) {
            this.username = username;
            return this;
        }

        public @NotNull User.Builder firstLogin(@Nullable LocalDateTime firstLogin) {
            this.firstLogin = firstLogin;
            return this;
        }

        public @NotNull User.Builder debugUser(boolean debugUser) {
            this.debugUser = debugUser;
            return this;
        }

        public @NotNull User.Builder debugEnabled(boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
            return this;
        }

        public @NotNull User.Builder languageId(int languageId) {
            this.languageId = languageId;
            return this;
        }

        public @NotNull User build() {
            return new UserImpl(id, mojangId, username, firstLogin, systemUser, debugUser, debugEnabled, languageId);
        }
    }
}
