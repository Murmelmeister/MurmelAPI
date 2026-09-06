package de.murmelmeister.murmelapi.user.excuse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public interface UserExcuse {
    int id();

    int userId();

    @NotNull LocalDate startDate();

    int extraDays();

    @Nullable String reason();

    @NotNull LocalDateTime createdAt();

    int createdBy();

    @Nullable LocalDateTime changedAt();

    @Nullable Integer changedBy();

    @NotNull Builder builder();

    @NotNull UserExcuse with(@NotNull Consumer<Builder> consumer);

    static @NotNull UserExcuse of(int id, int userId, @NotNull LocalDate startDate, int extraDays, @Nullable String reason, @NotNull LocalDateTime createdAt, int createdBy) {
        return new UserExcuseImpl(id, userId, startDate, extraDays, reason, createdAt, createdBy, null, null);
    }

    interface Builder {
        @NotNull Builder startDate(@NotNull LocalDate startDate);

        @NotNull Builder extraDays(int extraDays);

        @NotNull Builder reason(@Nullable String reason);

        @NotNull Builder changedAt(@Nullable LocalDateTime changedAt);

        @NotNull Builder changedBy(@Nullable Integer changedBy);

        @NotNull UserExcuse build();
    }
}
