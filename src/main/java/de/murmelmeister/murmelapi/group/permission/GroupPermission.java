package de.murmelmeister.murmelapi.group.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

public record GroupPermission(
        int groupId,
        @NotNull String permission,
        @Nullable LocalDateTime expiresAt,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) {
    public GroupPermission {
        Objects.requireNonNull(permission, "permission must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public static @NotNull Builder builder(@NotNull GroupPermission groupPermission) {
        return new Builder(groupPermission);
    }

    public static class Builder {
        private final int groupId;
        private final String permission;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private LocalDateTime expiresAt;
        private Integer changedBy;
        private LocalDateTime changedAt;

        private Builder(@NotNull GroupPermission groupPermission) {
            this.groupId = groupPermission.groupId();
            this.permission = groupPermission.permission();
            this.createdBy = groupPermission.createdBy();
            this.createdAt = groupPermission.createdAt();
            this.expiresAt = groupPermission.expiresAt();
            this.changedBy = groupPermission.changedBy();
            this.changedAt = groupPermission.changedAt();
        }

        public Builder expiresAt(@Nullable LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public @NotNull GroupPermission build() {
            return new GroupPermission(groupId, permission, expiresAt, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
