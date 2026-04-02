package de.murmelmeister.murmelapi.permission.parent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public interface Parent {
    int id();

    @Nullable Integer userId();

    @Nullable Integer groupId();

    int parentId();

    @Nullable LocalDateTime expiresAt();

    int createdBy();

    @NotNull LocalDateTime createdAt();

    @Nullable Integer changedBy();

    @Nullable LocalDateTime changedAt();

    boolean isExpired();

    boolean isPermanent();

    boolean isUser();

    boolean isGroup();

    @NotNull Builder builder();

    @NotNull Parent with(@NotNull Consumer<Builder> consumer);

    static @NotNull Parent of(int id, @Nullable Integer userId, @Nullable Integer groupId, int parentId, @Nullable LocalDateTime expiresAt, int createdBy, @NotNull LocalDateTime createdAt) {
        return new ParentImpl(id, userId, groupId, parentId, expiresAt, createdBy, createdAt, null, null);
    }

    interface Builder {
        @NotNull Builder expiresAt(@Nullable LocalDateTime expiresAt);

        @NotNull Builder changedBy(@Nullable Integer changedBy);

        @NotNull Builder changedAt(@Nullable LocalDateTime changedAt);

        @NotNull Parent build();
    }
}
