package de.murmelmeister.murmelapi.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

record PermissionImpl(
        int id,
        @Nullable Integer userId,
        @Nullable Integer groupId,
        @NotNull String permission,
        @Nullable LocalDateTime expiresAt,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) implements Permission {

    public PermissionImpl {
        Objects.requireNonNull(permission, "permission must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if ((userId != null) == (groupId != null))
            throw new IllegalArgumentException("userId and groupId cannot both be null or both be non-null");
        if (permission.length() > 200)
            throw new IllegalArgumentException("permission cannot be longer than 200 characters");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        if (changedBy != null && changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be null or >= " + CONSOLE_USER_ID);
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public boolean isUser() {
        return userId != null;
    }

    public boolean isGroup() {
        return groupId != null;
    }

    public @NotNull Permission.Builder builder() {
        return new Builder(this);
    }

    public @NotNull Permission with(@NotNull Consumer<Permission.Builder> consumer) {
        Permission.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements Permission.Builder {
        private final int id;
        private final Integer userId;
        private final Integer groupId;
        private final String permission;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private LocalDateTime expiresAt;
        private Integer changedBy;
        private LocalDateTime changedAt;

        public Builder(@NotNull Permission permission) {
            this.id = permission.id();
            this.userId = permission.userId();
            this.groupId = permission.groupId();
            this.permission = permission.permission();
            this.createdBy = permission.createdBy();
            this.createdAt = permission.createdAt();
            this.expiresAt = permission.expiresAt();
            this.changedBy = permission.changedBy();
            this.changedAt = permission.changedAt();
        }

        public @NotNull Permission.Builder expiresAt(@Nullable LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public @NotNull Permission.Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public @NotNull Permission.Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public @NotNull Permission build() {
            return new PermissionImpl(id, userId, groupId, permission, expiresAt, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
