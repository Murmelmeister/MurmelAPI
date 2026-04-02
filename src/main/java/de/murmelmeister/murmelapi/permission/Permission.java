package de.murmelmeister.murmelapi.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public interface Permission {
    int id();

    @Nullable Integer userId();

    @Nullable Integer groupId();

    @NotNull String permission();

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

    @NotNull Permission with(@NotNull Consumer<Builder> consumer);

    static @NotNull Permission of(int id, @Nullable Integer userId, @Nullable Integer groupId, @NotNull String permission, @Nullable LocalDateTime expiresAt, int createdBy, @NotNull LocalDateTime createdAt) {
        return new PermissionImpl(id, userId, groupId, permission, expiresAt, createdBy, createdAt, null, null);
    }

    interface Builder {
        @NotNull Builder expiresAt(@Nullable LocalDateTime expiresAt);

        @NotNull Builder changedBy(@Nullable Integer changedBy);

        @NotNull Builder changedAt(@Nullable LocalDateTime changedAt);

        @NotNull Permission build();
    }
}
