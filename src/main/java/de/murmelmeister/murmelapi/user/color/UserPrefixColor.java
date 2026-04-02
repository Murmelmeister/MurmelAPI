package de.murmelmeister.murmelapi.user.color;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public interface UserPrefixColor {
    int userId();

    @NotNull String colorId();

    boolean active();

    @NotNull LocalDateTime createdAt();

    @NotNull Builder builder();

    @NotNull UserPrefixColor with(@NotNull Consumer<Builder> consumer);

    static @NotNull UserPrefixColor of(int userId, @NotNull String colorId, boolean active, @NotNull LocalDateTime createdAt) {
        return new UserPrefixColorImpl(userId, colorId, active, createdAt);
    }

    interface Builder {
        @NotNull Builder active(boolean active);

        @NotNull UserPrefixColor build();
    }
}
