package de.murmelmeister.murmelapi.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public interface PrefixColor {
    @NotNull String id();

    @NotNull String color();

    boolean animated();

    @NotNull LocalDateTime createdAt();

    int createdBy();

    @Nullable LocalDateTime changedAt();

    @Nullable Integer changedBy();

    @NotNull Builder builder();

    @NotNull PrefixColor with(@NotNull Consumer<Builder> consumer);

    interface Builder {
        @NotNull Builder color(@NotNull String color);

        @NotNull Builder animated(boolean animated);

        @NotNull Builder changedAt(@Nullable LocalDateTime changedAt);

        @NotNull Builder changedBy(@Nullable Integer changedBy);

        @NotNull PrefixColor build();
    }
}
