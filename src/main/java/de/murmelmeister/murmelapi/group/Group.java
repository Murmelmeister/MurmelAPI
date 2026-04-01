package de.murmelmeister.murmelapi.group;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public interface Group {
    int id();

    @NotNull String groupName();

    int priority();

    boolean isDefault();

    int createdBy();

    @NotNull LocalDateTime createdAt();

    @Nullable Integer changedBy();

    @Nullable LocalDateTime changedAt();

    @NotNull Builder builder();

    @NotNull Group with(@NotNull Consumer<Builder> consumer);

    static @NotNull Group of(int id, @NotNull String groupName, int priority, boolean isDefault, int createdBy, @NotNull LocalDateTime createdAt) {
        return new GroupImpl(id, groupName, priority, isDefault, createdBy, createdAt, null, null);
    }

    interface Builder {
        @NotNull Builder groupName(@NotNull String groupName);

        @NotNull Builder priority(int priority);

        @NotNull Builder changedBy(@Nullable Integer changedBy);

        @NotNull Builder changedAt(@Nullable LocalDateTime changedAt);

        @NotNull Group build();
    }
}
