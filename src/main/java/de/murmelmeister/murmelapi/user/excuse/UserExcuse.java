package de.murmelmeister.murmelapi.user.excuse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public interface UserExcuse {
    int id();

    int userId();

    @NotNull LocalDateTime startAt();

    @NotNull LocalDateTime endAt();

    @Nullable String reason();

    @NotNull LocalDateTime createdAt();

    int createdBy();

    @Nullable LocalDateTime changedAt();

    @Nullable Integer changedBy();

    @NotNull Builder builder();

    @NotNull UserExcuse with(@NotNull Consumer<Builder> consumer);

    static @NotNull UserExcuse of(int id, int userId, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, @Nullable String reason, @NotNull LocalDateTime createdAt, int createdBy) {
        return new UserExcuseImpl(id, userId, startAt, endAt, reason, createdAt, createdBy, null, null);
    }

    interface Builder {
        @NotNull Builder startAt(@NotNull LocalDateTime startAt);

        @NotNull Builder endAt(@NotNull LocalDateTime endAt);

        @NotNull Builder reason(@Nullable String reason);

        @NotNull Builder changedAt(@Nullable LocalDateTime changedAt);

        @NotNull Builder changedBy(@Nullable Integer changedBy);

        @NotNull UserExcuse build();
    }
}
