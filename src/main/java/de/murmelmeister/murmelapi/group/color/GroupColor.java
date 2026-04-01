package de.murmelmeister.murmelapi.group.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public interface GroupColor {
    int groupId();

    int typeId();

    @NotNull String value();

    int createdBy();

    @NotNull LocalDateTime createdAt();

    @Nullable Integer changedBy();

    @Nullable LocalDateTime changedAt();

    @NotNull Builder builder();

    @NotNull GroupColor with(@NotNull Consumer<Builder> consumer);

    static @NotNull GroupColor of(int groupId, int typeId, @NotNull String value, int createdBy, @NotNull LocalDateTime createdAt) {
        return new GroupColorImpl(groupId, typeId, value, createdBy, createdAt, null, null);
    }

    interface Builder {
        @NotNull Builder value(@NotNull String value);

        @NotNull Builder changedBy(@Nullable Integer changedBy);

        @NotNull Builder changedAt(@Nullable LocalDateTime changedAt);

        @NotNull GroupColor build();
    }
}
