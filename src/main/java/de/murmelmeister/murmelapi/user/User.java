package de.murmelmeister.murmelapi.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

public interface User {
    int id();

    @NotNull UUID mojangId();

    @NotNull String username();

    @Nullable LocalDateTime firstLogin();

    boolean systemUser();

    boolean debugUser();

    boolean debugEnabled();

    int languageId();

    boolean debugMode();

    @NotNull Builder builder();

    @NotNull User with(@NotNull Consumer<Builder> consumer);

    static @NotNull User of(int id, @NotNull UUID mojangId, @NotNull String username, @Nullable LocalDateTime firstLogin, int languageId) {
        return new UserImpl(id, mojangId, username, firstLogin, false, false, false, languageId);
    }

    interface Builder {
        @NotNull Builder username(@NotNull String username);

        @NotNull Builder firstLogin(@Nullable LocalDateTime firstLogin);

        @NotNull Builder debugUser(boolean debugUser);

        @NotNull Builder debugEnabled(boolean debugEnabled);

        @NotNull Builder languageId(int languageId);

        @NotNull User build();
    }
}
